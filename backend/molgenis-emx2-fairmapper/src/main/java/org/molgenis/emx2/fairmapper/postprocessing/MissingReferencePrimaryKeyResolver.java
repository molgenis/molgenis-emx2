package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;
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
    String[] subjects = row.getString(SUBJECT_VARIABLE + column.getName()).split(",");

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

            // TODO: Check if array type?
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
          referringRow.set(reference.getColumnName(), value);
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

  private void resolveSingular(TableStore tableStore, String tableName, Column column) {
    String subjectColumnName = SUBJECT_VARIABLE + column.getName();

    //    StreamSupport.stream(tableStore.readTable(tableName).spliterator(), false)
    //                    .filter(x -> x)

    tableStore.processTable(
        tableName,
        (iterator, source) ->
            iterator.forEachRemaining(
                row -> {
                  if (!row.containsName(column.getName())) {
                    resolveRow(source, row, column, subjectColumnName);
                  }
                }));
  }

  private void resolveRow(TableStore tableStore, Row row, Column column, String subjectColumnName) {
    String subjectValue = row.getString(subjectColumnName);
    if (subjectValue == null) {
      return;
    }

    List<Reference> references = column.getReferences();
    if (references.isEmpty()) {
      return;
    }

    Optional<Row> optReferenceRow =
        StreamSupport.stream(tableStore.readTable(column.getRefTableName()).spliterator(), false)
            .filter(r -> r.getString(SUBJECT_VARIABLE).equals(subjectValue))
            .findFirst();

    if (optReferenceRow.isEmpty()) {
      throw new MolgenisException(
          "Cannot find reference for column: "
              + column.getName()
              + " with subject: "
              + subjectValue);
    }

    Row referenceRow = optReferenceRow.get();

    for (Reference reference : references) {
      String referencedColumnName = reference.getReferencedColumnName();
      if (referenceRow.containsName(referencedColumnName)) {
        row.set(reference.getColumnName(), referenceRow.getValueMap().get(referencedColumnName));
      } else {
        // Check for circular reference
        System.out.println("test");
      }
    }

    String targetTable = references.getFirst().getTargetTable();

    // Multiple references are a composite key, so that's fine
    List<Row> referenceTableRows =
        StreamSupport.stream(tableStore.readTable(targetTable).spliterator(), false).toList();

    // Only array reference columns hold multiple comma-separated subject IRIs; a singular
    // reference's IRI may itself legally contain a comma, so it must not be split.
    String[] subjectIris = column.isArray() ? subjectValue.split(",") : new String[] {subjectValue};

    List<Row> matches =
        Arrays.stream(subjectIris)
            .flatMap(subjectIri -> findBySubject(referenceTableRows, subjectIri).stream())
            .toList();

    if (matches.size() != subjectIris.length) {
      logger.warn("No matches found in table: {} for subject: {}", targetTable, subjectValue);
      return;
    }

    if (matches.isEmpty()) {
      logger.warn("No matches found in table: {} for subject: {}", targetTable, subjectValue);
      return;
    }

    for (Reference reference : references) {
      if (row.getString(reference.getColumnName()) != null) {
        continue;
      }

      String resolvedValue =
          matches.stream()
              .map(match -> match.getString(reference.getTargetColumn()))
              .filter(Objects::nonNull)
              .collect(Collectors.joining(","));

      if (!resolvedValue.isEmpty()) {
        row.set(reference.getColumnName(), resolvedValue);
      }
    }
  }

  private Optional<Row> findBySubject(List<Row> rows, String subjectIri) {
    return rows.stream()
        .filter(candidate -> subjectIri.equals(candidate.getString(SUBJECT_VARIABLE)))
        .findFirst();
  }
}
