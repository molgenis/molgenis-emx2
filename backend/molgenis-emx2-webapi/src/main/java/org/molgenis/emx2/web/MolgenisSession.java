package org.molgenis.emx2.web;

import graphql.GraphQL;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
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
  private final Map<String, GraphQL> graphqlPerSchema = new ConcurrentHashMap<>();
  private GraphQL graphqlForDatabase;

  public MolgenisSession(Database database, GraphqlApiFactory graphqlApiFactory) {
    database.setBindings(JavaScriptBindings.getBindingsForSession(this));
    this.database = database;
    this.graphqlApiFactory = graphqlApiFactory;
  }

  public GraphQL getGraphqlForDatabase(
      String user, TaskService taskService, HttpServletRequest request) {
    if (graphqlForDatabase == null) {
      graphqlForDatabase = new GraphqlApiFactory().createGraphql(user, taskService, request);
      logger.info("created graphql for user {}", getSessionUser());
    }
    return graphqlForDatabase;
  }

  public GraphQL getGraphqlForSchema(String schemaName, HttpServletRequest request) {
    logger.info("getting graphql schema '{}' for user '{}'", schemaName, getSessionUser());
    GraphQL emx2GraphQL = graphqlPerSchema.get(schemaName);
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
        ? anonymousGqlSchemaCache.get(schema, request)
        : graphqlPerSchema.computeIfAbsent(
            schemaName,
            key -> graphqlApiFactory.createGraphqlForSchema(schema, TaskApi.taskService, request));
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
