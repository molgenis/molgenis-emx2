package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.*;
import java.util.stream.StreamSupport;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MissingPkResolver implements PostProcessor {

  private static final Logger logger = LoggerFactory.getLogger(MissingPkResolver.class);
  private static final String SUBJECT_VARIABLE = "_subject_";

  private final SchemaMetadata schema;

  public MissingPkResolver(SchemaMetadata schema) {
    this.schema = schema;
  }

  @Override
  public void process(TableStore tableStore) {
    for (String tableName : schema.getTableNames()) {
      List<Column> referenceColumns =
          schema.getTableMetadata(tableName).getColumns().stream()
              .filter(Column::isReference)
              .toList();

      tableStore.processTable(
          tableName,
          (iterator, source) ->
              iterator.forEachRemaining(row -> resolveRow(tableStore, row, referenceColumns)));
    }
  }

  private void resolveRow(TableStore tableStore, Row row, List<Column> columns) {
    for (Column column : columns) {
      if (column.isArray()) {
        resolveArray(tableStore, row, column);
      } else {
        resolveSingular(tableStore, row, column);
      }
    }
  }

  private void resolveSingular(TableStore tableStore, Row row, Column column) {
    String subject = row.getString(SUBJECT_VARIABLE + column.getName());
    if (subject == null) {
      return;
    }

    Row referringRow = getRowForSubject(tableStore, column.getRefTableName(), subject);

    for (Reference reference : column.getReferences()) {
      if (row.containsName(reference.getColumnName())) {
        continue;
      }

      if (referringRow.containsName(reference.getReferencedColumnName())) {
        Object newValue = referringRow.getValueMap().get(reference.getReferencedColumnName());
        logger.info(
            "Updating subject: {}, deriving column: {} from reference, setting new value: {}",
            subject,
            reference.getColumnName(),
            newValue);
        row.set(reference.getColumnName(), newValue);
      } else {
        // This referenced row is missing its half of a composite key that points back to
        // this row's own table: treat it as a one-to-one back-reference, backfill the
        // missing part onto the referenced row from this row's value, and use that same
        // value as this row's entry in the array.
        if (isCircularReference(column, reference)) {
          Object value = row.getValueMap().get(reference.getTargetColumn());
          updateReferringRow(row, reference, referringRow, value);
          row.set(reference.getColumnName(), value);
        }
      }
    }
  }

  private static boolean isCircularReference(Column column, Reference reference) {
    List<String> columnTableNames = column.getTable().getAllInheritNames();
    columnTableNames.add(column.getTableName());
    return columnTableNames.contains(reference.getTargetTable());
  }

  private void resolveArray(TableStore tableStore, Row row, Column column) {
    if (!row.containsName(SUBJECT_VARIABLE + column.getName())) {
      return;
    }

    List<Row> rows =
        Optional.ofNullable(row.getString(SUBJECT_VARIABLE + column.getName())).stream()
            .flatMap(s -> Arrays.stream(s.split("\\|")))
            .map(subject -> getRowForSubject(tableStore, column.getRefTableName(), subject))
            .toList();

    for (Reference reference : column.getReferences()) {
      if (row.containsName(reference.getColumnName())) {
        continue;
      }

      List<Object> values = new ArrayList<>();
      for (Row referringRow : rows) {
        if (referringRow.containsName(reference.getReferencedColumnName())) {
          values.add(referringRow.getValueMap().get(reference.getReferencedColumnName()));
        } else {
          // This referenced row is missing its half of a composite key that points back to
          // this row's own table: treat it as a one-to-one back-reference, backfill the
          // missing part onto the referenced row from this row's value, and use that same
          // value as this row's entry in the array.
          if (isCircularReference(column, reference)) {
            Object value = row.getValueMap().get(reference.getTargetColumn());
            updateReferringRow(row, reference, referringRow, value);
            values.add(value);
          }
        }
      }
      row.setRefArray(reference.getColumnName(), values.toArray());
    }
  }

  private static void updateReferringRow(
      Row row, Reference reference, Row referringRow, Object value) {
    logger.info(
        "Object from table: {}, that refers to subject: {}, has a missing reference. Updating column {} with value: {}",
        reference.getTargetTable(),
        row.getString(SUBJECT_VARIABLE),
        reference.getReferencedColumnName(),
        value);
    referringRow.set(reference.getReferencedColumnName(), value);
  }

  private static Row getRowForSubject(
      TableStore tableStore, String targetTable, String referenceSubject) {
    return StreamSupport.stream(tableStore.readTable(targetTable).spliterator(), false)
        .filter(row -> row.getString(SUBJECT_VARIABLE).equals(referenceSubject))
        .findFirst()
        .orElseThrow(
            () ->
                new MolgenisException(
                    "Referencing non-existing row for table: "
                        + targetTable
                        + ", for subject: "
                        + referenceSubject));
  }
}
