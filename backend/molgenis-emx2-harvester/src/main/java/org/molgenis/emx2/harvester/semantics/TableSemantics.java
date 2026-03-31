package org.molgenis.emx2.harvester.semantics;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.jetbrains.annotations.NotNull;
import org.molgenis.emx2.*;
import org.molgenis.emx2.rdf.DefaultNamespace;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;

public class TableSemantics {

  private static final IRI RDF_TYPE = Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
  private static final String LOCALHOST_BASE = "http://localhost/";

  public record ColumnMapping(TableMetadata table, Column column) {}

  // Table name -> semantic -> mapping
  private final Map<String, Map<String, ColumnMapping>> mapping;

  public TableSemantics(Schema schema) {
    NamespaceMapper namespaceMapper = new NamespaceMapper(LOCALHOST_BASE, schema);
    mapping = buildMapping(schema.getMetadata(), semantic -> namespaceMapper.map(schema, semantic));
  }

  public Map<String, ColumnMapping> getMappingsForTableColumns(String tableName) {
    return mapping.get(tableName);
  }

  public Optional<ColumnMapping> getMappingForSemantic(String tableName, String semantic) {
    return Optional.ofNullable(getMappingsForTableColumns(tableName).get(semantic));
  }

  public Set<String> getSupportedSemantics() {
    Set<String> semantics =
        mapping.values().stream().flatMap(map -> map.keySet().stream()).collect(Collectors.toSet());
    semantics.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    return semantics;
  }

  private static Map<String, Map<String, ColumnMapping>> buildMapping(
      SchemaMetadata schema, Function<String, IRI> semanticToIri) {
    Map<String, Map<String, ColumnMapping>> tableMappings = new HashMap<>();

    for (TableMetadata table : schema.getTables()) {
      if (table.getTableType() != TableType.DATA) {
        continue;
      }

      Map<String, ColumnMapping> tableMap = getTableMap(semanticToIri, table);
      if (!tableMap.isEmpty()) {
        tableMappings.put(table.getTableName(), tableMap);
      }
    }

    return tableMappings;
  }

  private static Map<String, ColumnMapping> getTableMap(
      Function<String, IRI> semanticToIri, TableMetadata table) {

    Map<String, ColumnMapping> tableMapping = new HashMap<>();

    for (Column column : table.getColumns()) {
      if (column.getComputed() != null) {
        continue;
      }

      String[] semantics = column.getSemantics();
      if (semantics == null) {
        continue;
      }

      for (String semantic : semantics) {
        IRI predicate = semanticToIri.apply(semantic);
        if (RDF_TYPE.equals(predicate)) {
          continue;
        }

        tableMapping.put(semantic, new ColumnMapping(table, column));
      }
    }

    return tableMapping;
  }
}
