package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.*;
import java.util.stream.StreamSupport;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.rdf.generators.query.SparqlVariableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MissingReferencePrimaryKeyResolver {

  private static final Logger logger =
      LoggerFactory.getLogger(MissingReferencePrimaryKeyResolver.class);

  private static final String SUBJECT_VARIABLE = "_subject_";

  private final SchemaMetadata schema;

  public MissingReferencePrimaryKeyResolver(SchemaMetadata schema) {
    this.schema = schema;
  }

  public void resolve(TableStore tableStore, String... tableNames) {
    for (String tableName : tableNames) {
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

  private void resolveArray(TableStore tableStore, Row row, Column column) {
    if (!row.containsName(SUBJECT_VARIABLE + column.getName())) {
      return;
    }

    String[] subjects =
        Optional.ofNullable(row.getString(SUBJECT_VARIABLE + column.getName()))
            .map(s -> s.split(String.valueOf(SparqlVariableUtil.CONCAT_SEPARATOR)))
            .orElse(new String[0]);

    List<Row> rows =
        Arrays.stream(subjects)
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
          List<String> columnTableNames = column.getTable().getAllInheritNames();
          columnTableNames.add(column.getTableName());

          // When a part of the primary key is missing, and it's a reference to the root row, we
          // assume it's a one-to-one reference.
          if (columnTableNames.contains(reference.getTargetTable())) {
            Object value = row.getValueMap().get(reference.getTargetColumn());
            values.add(value);
            referringRow.set(reference.getColumnName(), value);
          }
        }
      }
      row.setRefArray(reference.getColumnName(), values.toArray());
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
        row.set(
            reference.getColumnName(),
            referringRow.getValueMap().get(reference.getReferencedColumnName()));
      } else {
        List<String> columnTableNames = column.getTable().getAllInheritNames();
        columnTableNames.add(column.getTableName());

        // When a part of the primary key is missing, and it's a reference to the root row, we
        // assume it's a one-to-one reference.
        if (columnTableNames.contains(reference.getTargetTable())) {
          Object value = row.getValueMap().get(reference.getTargetColumn());
          referringRow.set(reference.getReferencedColumnName(), value);
          row.set(reference.getColumnName(), value);
        }
      }
    }
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
