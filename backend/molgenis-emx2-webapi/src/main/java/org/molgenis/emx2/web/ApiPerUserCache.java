package org.molgenis.emx2.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import graphql.GraphQL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.tasks.ScriptTableListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiPerUserCache {
  private GraphqlApiFactory graphqlApiFactory = new GraphqlApiFactory();
  private static final Logger logger = LoggerFactory.getLogger(ApiPerUserCache.class);

  private Cache<String, Database> databaseCachePerUser =
      Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(30, TimeUnit.MINUTES).build();
  private Cache<String, GraphQL> schemaApiCachePerUser =
      Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(30, TimeUnit.MINUTES).build();
  private Cache<String, GraphQL> databaseApiCachePerUser =
      Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(30, TimeUnit.MINUTES).build();

  public ApiPerUserCache() {}

  public void clearCache() {
    databaseApiCachePerUser.invalidateAll();
    schemaApiCachePerUser.invalidateAll();
    databaseApiCachePerUser.invalidateAll();
  }

  public Database getDatabase(MolgenisSession session) {
    Objects.requireNonNull(session);
    String userName = session.getSessionUser();
    Database database =
        databaseCachePerUser.get(
            userName,
            key -> {
              SqlDatabase db = new SqlDatabase(false);
              db.setListener(session.getDatabaseChangeListener());
              db.addTableListener(new ScriptTableListener(TaskApi.taskSchedulerService));
              db.setActiveUser(userName);
              db.setBindings(JavaScriptBindings.getBindingsForSession(session));
              logger.info("created database instance for user '{}'", userName);
              return db;
            });
    logger.info("returned cached database instance for user '{}'", userName);
    return database;
  }

  public GraphQL getGraphqlForSchema(MolgenisSession session, String schemaName) {
    Objects.requireNonNull(session);
    Objects.requireNonNull(schemaName);
    String userName = session.getSessionUser();
    String userNameAndSchemaName = userName + ":" + schemaName;
    GraphQL graphQL =
        schemaApiCachePerUser.get(
            userNameAndSchemaName,
            key -> {
              logger.info("creating graphql schema api '{}' for user '{}'", schemaName, userName);
              return graphqlApiFactory.createGraphqlForSchema(
                  getDatabase(session).getSchema(schemaName), session);
            });
    logger.info("returned cached graphql schema api '{}' for user '{}'", schemaName, userName);
    return graphQL;
  }

  public GraphQL getGraphqlForDatabase(MolgenisSession session) {
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
