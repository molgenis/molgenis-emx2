package org.molgenis.emx2.fairmapper.postprocessing.ontologies;

import java.util.HashMap;
import java.util.Map;
import org.molgenis.emx2.*;

public class DatabaseOntologyMappingFetcher implements OntologyMappingFetcher {

  private static final String ONTOLOGY_TERM_URI = "ontologyTermURI";

  private final Database database;

  public DatabaseOntologyMappingFetcher(Database database) {
    this.database = database;
  }

  @Override
  public Map<String, String> getMapping(String schemaName, String tableName) {
    Schema schema = database.getSchema(schemaName);
    if (schema == null) {
      throw new MolgenisException("No schema found with name: " + schemaName);
    }

    Table table = schema.getTable(tableName);
    if (table == null) {
      throw new MolgenisException(
          "No table with name: " + tableName + " found for schema " + schemaName);
    }

    HashMap<String, String> mapping = new HashMap<>();
    table
        .query()
        .select(SelectColumn.s(ONTOLOGY_TERM_URI), SelectColumn.s("name"))
        .streamRows(
            row -> {
              if (row.notEmpty(ONTOLOGY_TERM_URI)) {
                mapping.put(row.getString(ONTOLOGY_TERM_URI), row.getString("name"));
              }
            });
    return mapping;
  }
}
