package org.molgenis.emx2.graphql;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import graphql.GraphQL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.sql.SqlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphqlApiPerUserCache {
  private GraphqlApiFactory graphqlApiFactory = new GraphqlApiFactory();
  private static final Logger logger = LoggerFactory.getLogger(GraphqlApiPerUserCache.class);

  private Cache<String, Database> databaseCachePerUser =
      Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(30, TimeUnit.MINUTES).build();
  private Cache<String, GraphQL> schemaApiCachePerUser =
      Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(30, TimeUnit.MINUTES).build();
  private Cache<String, GraphQL> databaseApiCachePerUser =
      Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(30, TimeUnit.MINUTES).build();

  public GraphqlApiPerUserCache() {}

  public void clearCache() {
    databaseCachePerUser.invalidateAll();
    schemaApiCachePerUser.invalidateAll();
    databaseApiCachePerUser.invalidateAll();
    logger.info("cleared caches");
  }

  public Database getDatabase(GraphqlSession session) {
    Objects.requireNonNull(session);
    String userName = session.getSessionUser();
    Database database =
        databaseCachePerUser.get(
            userName,
            key -> {
              logger.info("creating database instance for user '{}'", userName);
              SqlDatabase db = new SqlDatabase(userName);
              db.setListener(session.getDatabaseChangeListener());
              // TODO db.addTableListener(new ScriptTableListener(TaskApi.taskSchedulerService));
              // TODO db.setBindings(JavaScriptBindings.getBindingsForSession(session));
              return db;
            });
    logger.info("returned cached database instance for user '{}'", userName);
    return database;
  }

  public GraphQL getGraphqlForSchema(GraphqlSession session, String schemaName) {
    Objects.requireNonNull(session);
    Objects.requireNonNull(schemaName);
    String userName = session.getSessionUser();
    String userNameAndSchemaName = userName + ":" + schemaName;
    GraphQL graphQL =
        schemaApiCachePerUser.get(
            userNameAndSchemaName,
            key -> {
              Database database = getDatabase(session);
              logger.info("creating graphql schema api '{}' for user '{}'", schemaName, userName);
              return graphqlApiFactory.createGraphqlForSchema(
                  database.getSchema(schemaName), session);
            });
    logger.info("returned cached graphql schema api '{}' for user '{}'", schemaName, userName);
    return graphQL;
  }

  public GraphQL getGraphqlForDatabase(GraphqlSession session) {
    Objects.requireNonNull(session);
    String userName = session.getSessionUser();
    GraphQL graphQL =
        schemaApiCachePerUser.get(
            userName,
            key -> {
              logger.info("creating graphql database api for user '{}'", userName);
              return graphqlApiFactory.createGraphqlForDatabase(session);
            });
    logger.info("returned cached graphql database api for user '{}'", userName);
    return graphQL;
  }
}
