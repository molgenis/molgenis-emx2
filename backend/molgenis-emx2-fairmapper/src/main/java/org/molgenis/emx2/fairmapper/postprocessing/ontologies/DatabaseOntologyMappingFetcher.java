package org.molgenis.emx2.fairmapper.postprocessing.ontologies;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseOntologyMappingFetcher implements OntologyMappingFetcher {

  private static final Logger logger =
      LoggerFactory.getLogger(DatabaseOntologyMappingFetcher.class);
  private static final String ONTOLOGY_TERM_URI = "ontologyTermURI";

  private final Database database;

  public DatabaseOntologyMappingFetcher(Database database) {
    this.database = database;
  }

  @Override
  public Map<String, String> getMapping(String schemaName, String tableName) {
    Schema schema = database.getSchema(schemaName);
    if (schema == null) {
      logger.warn("Unable to get ontology mapping, could not find schema: {}", schemaName);
      return Collections.emptyMap();
    }

    Table table = schema.getTable(tableName);
    if (table == null) {
      logger.warn("Unable to get ontology mapping, could not find table: {}", tableName);
      return Collections.emptyMap();
    }

    return table
        .query()
        .select(SelectColumn.s(ONTOLOGY_TERM_URI), SelectColumn.s("name"))
        .retrieveRows()
        .stream()
        .filter(row -> row.getString(ONTOLOGY_TERM_URI) != null)
        .collect(Collectors.toMap(r -> r.getString(ONTOLOGY_TERM_URI), r -> r.getString("name")));
  }
}
