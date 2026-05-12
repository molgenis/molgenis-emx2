package org.molgenis.emx2.harvester;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.jetbrains.annotations.NotNull;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntologyPostProcessor {

  private static final Logger logger = LoggerFactory.getLogger(OntologyPostProcessor.class);

  private final TableMetadata table;
  private final TableStore tableStore;
  private final List<Column> ontologyColumns;

  // Ontology type -> Ontology semantic -> name
  private final Map<String, Map<String, String>> ontologySemanticsMapping = new HashMap<>();

  public OntologyPostProcessor(TableStore tableStore, TableMetadata table) {
    this.table = table;
    this.tableStore = tableStore;
    this.ontologyColumns = table.getColumns().stream().filter(Column::isOntology).toList();
  }

  public void process() {
    List<Row> rows =
        StreamSupport.stream(tableStore.readTable(table.getTableName()).spliterator(), false)
            .toList();
    for (Row row : rows) {
      parseRow(row);
    }

    tableStore.writeTable(
        table.getTableName(),
        table.getDownloadColumnNames().stream().map(Column::getName).toList(),
        rows);
  }

  private void parseRow(Row row) {
    for (Column column : ontologyColumns) {
      if (Boolean.TRUE.equals(column.isArray())) {
        mapOntologyArray(row, column);
      } else {
        mapOntology(row, column);
      }
    }
  }

  private void mapOntology(Row row, Column column) {
    Map<String, String> mapping = getMappingForColumn(column);

    String uri = row.getString(column.getName());
    if (uri == null) {
      return;
    }

    if (mapping.containsKey(uri)) {
      row.set(column.getName(), mapping.get(uri));
    } else {
      logger.warn(
          "No mapping found for table: {}, column: {} and value: {}, using null instead",
          column.getTableName(),
          column.getName(),
          uri);
      row.clear(column.getName());
    }
  }

  private void mapOntologyArray(Row row, Column column) {
    Map<String, String> mapping = getMappingForColumn(column);

    String[] stringArray = row.getStringArray(column.getName());
    if (stringArray == null) {
      return;
    }

    List<String> value = new ArrayList<>();

    for (String s : stringArray) {
      value.add(mapping.get(s));
    }

    if (value.size() != stringArray.length) {
      logger.warn("Missing semantic in array");
    }

    if (value.isEmpty()) {
      row.clear(column.getName());
    } else {
      row.set(column.getName(), value.stream().distinct().collect(Collectors.joining(",")));
    }
  }

  @NotNull
  private Map<String, String> getMappingForColumn(Column column) {
    return ontologySemanticsMapping.computeIfAbsent(
        column.getName(), name -> buildMappingForOntologyColumn(column));
  }

  private Map<String, String> buildMappingForOntologyColumn(Column column) {
    Map<String, String> mapping = new HashMap<>();

    for (Row row : column.getRefTable().getTable().retrieveRows()) {
      String uri = row.getString("ontologyTermURI");
      if (uri != null) {
        mapping.put(uri, row.getString("name"));
      }
    }

    return mapping;
  }
}
