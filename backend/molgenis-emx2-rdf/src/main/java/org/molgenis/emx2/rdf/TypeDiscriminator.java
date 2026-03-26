package org.molgenis.emx2.rdf;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeDiscriminator {

  private static final Logger log = LoggerFactory.getLogger(TypeDiscriminator.class);
  private static final String LOCALHOST_BASE = "http://localhost/";

  public record TableAssignment(String tableName, String typeColumnName, String typeValue) {}

  private final Map<IRI, TableAssignment> typeMap;

  public TypeDiscriminator(Schema schema) {
    typeMap = buildTypeMap(schema);
    log.debug("TypeDiscriminator built with {} entries", typeMap.size());
  }

  private static Map<IRI, TableAssignment> buildTypeMap(Schema schema) {
    NamespaceMapper namespaceMapper = new NamespaceMapper(LOCALHOST_BASE, schema);
    Map<IRI, TableAssignment> map = new LinkedHashMap<>();

    List<TableMetadata> tables =
        schema.getMetadata().getTables().stream()
            .filter(t -> t.getTableType() == TableType.DATA)
            .toList();

    addOntologyColumnMappings(tables, schema, map);
    addTableSemanticMappings(tables, schema, namespaceMapper, map);

    return map;
  }

  private static void addTableSemanticMappings(
      List<TableMetadata> tables,
      Schema schema,
      NamespaceMapper namespaceMapper,
      Map<IRI, TableAssignment> map) {

    List<TableMetadata> parentFirst =
        tables.stream()
            .sorted(
                (a, b) -> {
                  boolean aIsChild = a.getInheritName() != null;
                  boolean bIsChild = b.getInheritName() != null;
                  if (aIsChild && !bIsChild) return 1;
                  if (!aIsChild && bIsChild) return -1;
                  return 0;
                })
            .toList();

    for (TableMetadata table : parentFirst) {
      String[] semantics = table.getSemantics();
      if (semantics == null) {
        continue;
      }
      for (String semanticEntry : semantics) {
        for (String semantic : semanticEntry.split(",")) {
          semantic = semantic.trim();
          if (!semantic.contains(":")) {
            continue;
          }
          try {
            IRI typeIri = namespaceMapper.map(schema, semantic);
            map.put(typeIri, new TableAssignment(table.getTableName(), null, null));
          } catch (Exception e) {
            log.trace("Could not expand table semantic '{}': {}", semantic, e.getMessage());
          }
        }
      }
    }
  }

  private static void addOntologyColumnMappings(
      List<TableMetadata> tables, Schema schema, Map<IRI, TableAssignment> map) {

    for (TableMetadata tableMeta : tables) {
      for (Column column : tableMeta.getColumns()) {
        ColumnType colType = column.getColumnType();
        if (colType != ColumnType.ONTOLOGY && colType != ColumnType.ONTOLOGY_ARRAY) {
          continue;
        }
        Table refTable = resolveRefTable(column, schema);
        if (refTable == null) {
          continue;
        }
        loadOntologyTermMappings(refTable, tableMeta.getTableName(), column.getName(), schema, map);
      }
    }
  }

  private static Table resolveRefTable(Column column, Schema schema) {
    try {
      String refTableName = column.getRefTableName();
      if (refTableName == null) {
        return null;
      }
      String refSchemaName = column.getRefSchemaName();
      Schema targetSchema =
          refSchemaName.equals(schema.getName())
              ? schema
              : schema.getDatabase().getSchema(refSchemaName);
      return targetSchema != null ? targetSchema.getTable(refTableName) : null;
    } catch (Exception e) {
      log.trace(
          "Could not resolve ref table for column '{}': {}", column.getName(), e.getMessage());
      return null;
    }
  }

  private static void loadOntologyTermMappings(
      Table ontologyTable,
      String targetTableName,
      String typeColumnName,
      Schema schema,
      Map<IRI, TableAssignment> map) {

    List<Row> rows;
    try {
      rows =
          ontologyTable
              .query()
              .select(
                  org.molgenis.emx2.SelectColumn.s("name"),
                  org.molgenis.emx2.SelectColumn.s("ontologyTermURI"),
                  org.molgenis.emx2.SelectColumn.s("alternativeIds"))
              .retrieveRows();
    } catch (Exception e) {
      log.trace(
          "Could not load ontology terms from '{}': {}", ontologyTable.getName(), e.getMessage());
      return;
    }

    for (Row row : rows) {
      String name = row.getString("name");
      if (name == null) {
        continue;
      }
      TableAssignment assignment = new TableAssignment(targetTableName, typeColumnName, name);

      String uri = row.getString("ontologyTermURI");
      if (uri != null && !uri.isBlank()) {
        try {
          map.putIfAbsent(org.eclipse.rdf4j.model.util.Values.iri(uri), assignment);
        } catch (Exception e) {
          log.trace("Invalid ontologyTermURI '{}': {}", uri, e.getMessage());
        }
      }

      String[] altIds = row.getStringArray("alternativeIds");
      if (altIds != null) {
        for (String altId : altIds) {
          if (altId != null && !altId.isBlank()) {
            try {
              map.putIfAbsent(org.eclipse.rdf4j.model.util.Values.iri(altId), assignment);
            } catch (Exception e) {
              log.trace("Invalid alternativeId '{}': {}", altId, e.getMessage());
            }
          }
        }
      }
    }
  }

  public TableAssignment assignTable(Set<IRI> subjectTypes) {
    for (Map.Entry<IRI, TableAssignment> entry : typeMap.entrySet()) {
      if (subjectTypes.contains(entry.getKey())) {
        log.trace("Matched type {} → {}", entry.getKey().getLocalName(), entry.getValue());
        return entry.getValue();
      }
    }
    return null;
  }
}
