package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.*;
import java.util.stream.Collectors;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.SelectColumn;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntologyResolver implements PostProcessor {

  private static final String ONTOLOGY_TERM_URI = "ontologyTermURI";
  private static final Logger logger = LoggerFactory.getLogger(OntologyResolver.class);

  private final Map<String, Map<String, String>> ontologyValues = new HashMap<>();
  private final SchemaMetadata schema;

  public OntologyResolver(SchemaMetadata schema) {
    this.schema = schema;
  }

  @Override
  public void process(TableStore tableStore) {
    for (String table : tableStore.getTableNames()) {
      Iterable<Row> rows = tableStore.readTable(table);
      for (Column column : schema.getTableMetadata(table).getColumns()) {
        if (!column.isOntology()) {
          continue;
        }

        for (Row row : rows) {
          if (row.containsName(column.getName())) {
            resolveCell(column, row);
          }
        }
      }
    }
  }

  private void resolveCell(Column column, Row row) {
    Map<String, String> ontologyMapping = getOntologyMapping(column);
    String columnValue = row.getString(column.getName());
    Optional<String> mappedValue;

    if (column.isArray()) {
      mappedValue = mapArray(column, columnValue, ontologyMapping);
    } else {
      mappedValue = mapSingle(column, columnValue, ontologyMapping);
    }

    if (mappedValue.isEmpty()) {
      row.clear(column.getName());
    } else {
      row.set(column.getName(), mappedValue.get());
    }
  }

  private Optional<String> mapSingle(
      Column column, String columnValue, Map<String, String> ontologyMapping) {
    Optional<String> mapped = Optional.ofNullable(ontologyMapping.get(columnValue));
    if (mapped.isEmpty()) {
      logMissingMapping(column, columnValue);
    }
    return mapped;
  }

  private static void logMissingMapping(Column column, String columnValue) {
    String referenceKey = referenceKey(column);
    logger.warn("No ontology of type: {} for value: {}", referenceKey, columnValue);
  }

  private static Optional<String> mapArray(
      Column column, String columnValue, Map<String, String> ontologyMapping) {
    String[] split = columnValue.split(",");
    List<String> mappedValues = new ArrayList<>();
    for (String value : split) {
      String mapped = ontologyMapping.get(value);
      if (mapped == null) {
        logMissingMapping(column, value);
      } else {
        mappedValues.add(mapped);
      }
      handleValue(column, ontologyMapping, value);
    }

    if (mappedValues.size() != split.length) {
      return Optional.empty();
    } else {
      return Optional.of(String.join(",", mappedValues));
    }
  }

  private Map<String, String> getOntologyMapping(Column column) {
    String key = referenceKey(column);
    return ontologyValues.computeIfAbsent(key, s -> generateMapping(column));
  }

  private static Optional<String> handleValue(
      Column column, Map<String, String> ontologyMapping, String value) {
    Optional<String> handled = Optional.ofNullable(ontologyMapping.get(value));
    if (handled.isEmpty()) {
      logMissingMapping(column, value);
    }
    return handled;
  }

  private Map<String, String> generateMapping(Column column) {
    List<Row> ontologyRows =
        column
            .getRefTable()
            .getTable()
            .query()
            .select(SelectColumn.s(ONTOLOGY_TERM_URI), SelectColumn.s("name"))
            .retrieveRows();

    return ontologyRows.stream()
        .filter(row -> row.getString(ONTOLOGY_TERM_URI) != null)
        .collect(Collectors.toMap(r -> r.getString(ONTOLOGY_TERM_URI), r -> r.getString("name")));
  }

  private static String referenceKey(Column column) {
    return column.getRefSchemaName() + "." + column.getRefTableName();
  }
}
