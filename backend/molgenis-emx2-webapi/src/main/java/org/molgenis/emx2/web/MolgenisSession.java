package org.molgenis.emx2.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.graphql.EMX2GraphQL;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.tasks.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MolgenisSession {
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSession.class);
  private final AnonymousGqlSchemaCache anonymousGqlSchemaCache =
      AnonymousGqlSchemaCache.getInstance();
  private final GraphqlApiFactory graphqlApiFactory;
  private final Database database;
  private final Map<String, EMX2GraphQL> graphqlPerSchema = new ConcurrentHashMap<>();
  private EMX2GraphQL graphqlForDatabase;

  public MolgenisSession(Database database, GraphqlApiFactory graphqlApiFactory) {
    database.setBindings(JavaScriptBindings.getBindingsForSession(this));
    this.database = database;
    this.graphqlApiFactory = graphqlApiFactory;
  }

  public EMX2GraphQL getGraphqlForDatabase(String user, TaskService taskService) {
    if (graphqlForDatabase == null) {
      graphqlForDatabase = new GraphqlApiFactory().createGraphql(user, taskService);
      logger.info("created graphql for user {}", getSessionUser());
    }
    return graphqlForDatabase;
  }

  public EMX2GraphQL getGraphqlForSchema(String schemaName) {
    logger.info("getting graphql schema '{}' for user '{}'", schemaName, getSessionUser());
    EMX2GraphQL emx2GraphQL = graphqlPerSchema.get(schemaName);
    if (emx2GraphQL != null) {
      logger.info("return cached graphql schema '{}' for user '{}'", schemaName, getSessionUser());
      return emx2GraphQL;
    }

    Schema schema = database.getSchema(schemaName);
    if (schema == null)
      throw new MolgenisException(
          "Schema not found: Schema with name '"
              + schemaName
              + "' does not exist or permission denied");

    return database.isAnonymous()
        ? new EMX2GraphQL(schema.getDatabase(), anonymousGqlSchemaCache.get(schema))
        : graphqlPerSchema.computeIfAbsent(
            schemaName,
            key -> graphqlApiFactory.createGraphqlForSchema(schema, TaskApi.taskService));
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
    anonymousGqlSchemaCache.invalidate();
    logger.info("cleared database and caches for user {}", getSessionUser());
  }
}
