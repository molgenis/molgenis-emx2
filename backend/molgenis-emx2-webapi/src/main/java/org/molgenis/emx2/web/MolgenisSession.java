package org.molgenis.emx2.web;

import graphql.GraphQL;
import java.util.LinkedHashMap;
import java.util.Map;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MolgenisSession {
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSession.class);
  private Database database;
  private GraphQL graphqlForDatabase;
  private Map<String, GraphQL> graphqlPerSchema = new LinkedHashMap<>();

  public MolgenisSession(Database database) {
    this.database = database;
  }

  public GraphQL getGraphqlForDatabase() {
    if (graphqlForDatabase == null) {
      graphqlForDatabase =
          new GraphqlApiFactory().createGraphqlForDatabase(database, TaskApi.taskService);
      logger.info("created graphql for user {}", getSessionUser());
    }
    return graphqlForDatabase;
  }

  public GraphQL getGraphqlForSchema(String schemaName) {
    logger.info("getting graphql schema '{}' for user '{}'", schemaName, getSessionUser());
    if (graphqlPerSchema.get(schemaName) == null) {
      Schema schema = database.getSchema(schemaName);
      if (schema == null)
        throw new MolgenisException(
            "Schema not found: Schema with name '"
                + schemaName
                + "' does not exist or permission denied");
      graphqlPerSchema.put(
          schemaName, new GraphqlApiFactory().createGraphqlForSchema(schema, TaskApi.taskService));
      logger.info("created graphql schema '{}' for user '{}'", schemaName, getSessionUser());
    }
    logger.info("return graphql schema '{}' for user '{}'", schemaName, getSessionUser());
    return graphqlPerSchema.get(schemaName);
  }

  public Database getDatabase() {
    return database;
  }

  public String getSessionUser() {
    return database.getActiveUser();
  }

  public void clearCache() {
    this.graphqlPerSchema.clear();
    this.graphqlForDatabase = null;
    this.database.clearCache();
    logger.info("cleared database and caches for user {}", getSessionUser());
  }
}
