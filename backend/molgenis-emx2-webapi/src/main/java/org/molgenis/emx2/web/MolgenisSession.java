package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.util.EnvHelpers.getEnvInt;
import static org.molgenis.emx2.web.util.EnvHelpers.getEnvLong;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import graphql.GraphQL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MolgenisSession {
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSession.class);

  private static final Cache<String, GraphQL> anonymousGqlObjectCache =
      Caffeine.newBuilder()
          .maximumSize(getEnvInt("ANONYMOUS_GQL_CACHE_MAX_SIZE", 100))
          .expireAfterAccess(
              getEnvLong("ANONYMOUS_GQL_CACHE_EXPIRE_ACCESS_MIN", 1L), TimeUnit.MINUTES)
          .expireAfterWrite(
              getEnvLong("ANONYMOUS_GQL_CACHE_EXPIRE_WRITE_MIN", 30L), TimeUnit.MINUTES)
          .build();
  private final GraphqlApiFactory graphqlApiFactory;
  private final Database database;
  private final Map<String, GraphQL> graphqlPerSchema = new LinkedHashMap<>();
  private GraphQL graphqlForDatabase;

  public MolgenisSession(Database database, GraphqlApiFactory graphqlApiFactory) {
    database.setBindings(JavaScriptBindings.getBindingsForSession(this));
    this.database = database;
    this.graphqlApiFactory = graphqlApiFactory;
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
    GraphQL graphQL = graphqlPerSchema.get(schemaName);
    if (graphQL != null) {
      logger.info("return cached graphql schema '{}' for user '{}'", schemaName, getSessionUser());
      return graphQL;
    }

    Schema schema = database.getSchema(schemaName);
    if (schema == null)
      throw new MolgenisException(
          "Schema not found: Schema with name '"
              + schemaName
              + "' does not exist or permission denied");

    return database.isAnonymous()
        ? anonymousGqlObjectCache.get(
            schemaName,
            key -> graphqlApiFactory.createGraphqlForSchema(schema, TaskApi.taskService))
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
    anonymousGqlObjectCache.invalidateAll();
    logger.info("cleared database and caches for user {}", getSessionUser());
  }
}
