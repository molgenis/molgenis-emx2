package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MissingPkResolver implements PostProcessor {

  private static final Logger logger = LoggerFactory.getLogger(MissingPkResolver.class);

  private static final String SUBJECT_VARIABLE = "_subject_";

  private final SchemaMetadata schema;
  private final List<String> tableNames;

  public MissingPkResolver(SchemaMetadata schema, String... tableNames) {
    this.schema = schema;
    this.tableNames = List.of(tableNames);
  }

  @Override
  public void process(TableStore tableStore) {

    for (String tableName : tableNames) {
      TableMetadata table = schema.getTableMetadata(tableName);
      for (Column column : table.getColumns()) {
        if (column.isReference()) {
          resolveColumn(tableStore, tableName, column);
        }
      }
    }
  }

  private void resolveColumn(TableStore tableStore, String tableName, Column column) {
    String subjectColumnName = SUBJECT_VARIABLE + column.getName();
    tableStore.processTable(
        tableName,
        (iterator, source) ->
            iterator.forEachRemaining(row -> resolveRow(source, row, column, subjectColumnName)));
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
