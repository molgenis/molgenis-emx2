package org.molgenis.emx2.harvester.semantics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.rdf.DefaultNamespace;

public class TableNameMapping {

  private static final IRI RDF_TYPE = Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

  /* Mapping from table semantic to emx2 table name */
  private final Map<String, String> tableNameMappings;

  public TableNameMapping(Schema schema) {
    tableNameMappings = setupTableNameMapping(schema);
  }

  private Map<String, String> setupTableNameMapping(Schema schema) {
    final Map<String, String> tableNameMappings;
    tableNameMappings = new HashMap<>();
    for (TableMetadata table : schema.getMetadata().getTables()) {
      String[] semantics = table.getSemantics();
      if (semantics == null) {
        continue;
      }

      for (String semantic : semantics) {

        String[] split = semantic.split(":");
        if (split.length != 2) {
          tableNameMappings.put(semantic, table.getTableName());
        } else {
          Namespace namespace =
              DefaultNamespace.streamAll()
                  .filter(x -> x.getPrefix().equals(split[0]))
                  .findFirst()
                  .orElseThrow();

          tableNameMappings.put(namespace.getName() + split[1], table.getTableName());
        }
      }
    }
    return tableNameMappings;
  }

  public Optional<String> getTableNameFromStatements(List<Statement> statements) {
    for (Statement statement : statements) {
      if (statement.getPredicate().equals(RDF_TYPE)) {
        if (tableNameMappings.containsKey(statement.getObject().stringValue())) {
          return Optional.of(tableNameMappings.get(statement.getObject().stringValue()));
        } else {
          return Optional.empty();
        }
      }
    }

    return Optional.empty();
  }
}
