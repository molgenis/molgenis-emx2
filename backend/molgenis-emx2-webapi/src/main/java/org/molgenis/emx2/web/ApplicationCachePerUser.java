package org.molgenis.emx2.web;

import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Constants.ANONYMOUS;
import static org.molgenis.emx2.web.MolgenisWebservice.oidcController;
import static org.pac4j.core.util.Pac4jConstants.USERNAME;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import graphql.GraphQL;
import io.javalin.http.Context;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.DatabaseListener;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.sql.JWTgenerator;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.tasks.ScriptTableListener;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationCachePerUser {
  private static final long DEFAULT_APP_CACHE_SIZE = 10_000;
  public static final Integer APP_CACHE_DURATION =
      (Integer)
          EnvironmentProperty.getParameter(
              org.molgenis.emx2.Constants.MOLGENIS_APP_CACHE_DURATION, 5, INT);

  public static final Long APP_DB_CACHE_SIZE =
      (Long)
          EnvironmentProperty.getParameter(
              org.molgenis.emx2.Constants.MOLGENIS_APP_DB_CACHE_SIZE, DEFAULT_APP_CACHE_SIZE, LONG);

  public static final Long APP_SCHEMA_CACHE_SIZE =
      (Long)
          EnvironmentProperty.getParameter(
              org.molgenis.emx2.Constants.MOLGENIS_APP_SCHEMA_CACHE_SIZE,
              DEFAULT_APP_CACHE_SIZE,
              LONG);

  public static final Long APP_GQL_DB_CACHE_SIZE =
      (Long)
          EnvironmentProperty.getParameter(
              org.molgenis.emx2.Constants.MOLGENIS_APP_GQL_DB_CACHE_SIZE,
              DEFAULT_APP_CACHE_SIZE,
              LONG);

  public static final Long APP_GQL_SCHEMA_CACHE_SIZE =
      (Long)
          EnvironmentProperty.getParameter(
              org.molgenis.emx2.Constants.MOLGENIS_APP_GQL_SCHEMA_CACHE_SIZE,
              DEFAULT_APP_CACHE_SIZE,
              LONG);

  private record UserKey(String userName) {
    public UserKey {
      if (userName == null || userName.isEmpty()) {
        throw new IllegalArgumentException("userName cannot be null or empty");
      }
    }
  }

  private record UserSchemaKey(UserKey userKey, String schemaName) {
    public UserSchemaKey {
      if (schemaName == null || schemaName.isEmpty()) {
        throw new IllegalArgumentException("schemaName cannot be null or empty");
      }
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(ApplicationCachePerUser.class);

  private final Cache<UserKey, Database> databaseCache;
  private final Cache<UserSchemaKey, Schema> schemaCache;
  private final Cache<UserKey, GraphQL> graphqlDatabaseCache;
  private final Cache<UserSchemaKey, GraphQL> graphqlSchemaCache;

  GraphqlApiFactory graphqlApiFactory = new GraphqlApiFactory();

  private static final ApplicationCachePerUser INSTANCE = new ApplicationCachePerUser();

  private ApplicationCachePerUser() {
    databaseCache =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(APP_CACHE_DURATION))
            .maximumSize(APP_DB_CACHE_SIZE)
            .build();
    schemaCache =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(APP_CACHE_DURATION))
            .maximumSize(APP_SCHEMA_CACHE_SIZE)
            .build();
    graphqlDatabaseCache =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(APP_CACHE_DURATION))
            .maximumSize(APP_GQL_DB_CACHE_SIZE)
            .build();
    graphqlSchemaCache =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(APP_CACHE_DURATION))
            .maximumSize(APP_GQL_SCHEMA_CACHE_SIZE)
            .build();
  }

  public static ApplicationCachePerUser getInstance() {
    return INSTANCE;
  }

  public Database getDatabaseForUser(Context ctx) {
    UserKey key = getUserKey(ctx);
    return getDatabaseForUser(key);
  }

  public Database getDatabaseForUser(UserKey key) {
    return databaseCache.get(
        key,
        userKey -> {
          logger.info("create database cache for user {}", key);
          SqlDatabase database = new SqlDatabase(false);
          if (!database.hasUser(userKey.userName)
              || !database.getUser(userKey.userName).getEnabled()) {
            throw new MolgenisException(
                "User " + userKey.userName + " does not exist or has been disabled");
          }
          database.setActiveUser(userKey.userName);
          database.addTableListener(new ScriptTableListener(TaskApi.taskSchedulerService));
          database.setBindings(JavaScriptBindings.getBindingsForUser(userKey.userName()));
          database.clearCache();
          database.setListener(
              new DatabaseListener() {

                @Override
                public void onUserChange() {
                  // should never happen anymore
                  logger.error("user changed, that should never happen");
                  clearAllCaches();
                }

                @Override
                public void onSchemaChange() {
                  // just to make sure we now clear all caches
                  // we can make it smarter to only invalidate per schema
                  // nb this only fires after commit of schema changes, not of row updates
                  clearAllCaches();
                }
              });

          return database;
        });
  }

  public Schema getSchemaForUser(String schemaName, UserKey userKey) {
    UserSchemaKey key = new UserSchemaKey(userKey, schemaName);
    return schemaCache.get(
        key,
        k -> {
          logger.info("create schema '{}' cache for user {}", schemaName, userKey.userName());
          return getDatabaseForUser(userKey).getSchema(schemaName);
        });
  }

  public Schema getSchemaForUser(String schemaName, Context ctx) {
    UserKey userKey = getUserKey(ctx);
    return getSchemaForUser(schemaName, userKey);
  }

  public GraphQL getDatabaseGraphqlForUser(Context ctx) {
    return graphqlDatabaseCache.get(
        getUserKey(ctx),
        k -> {
          logger.info("create graphqlDatabaseApi cache for user {}", getUserKey(ctx));
          return graphqlApiFactory.createGraphqlForDatabase(
              getDatabaseForUser(ctx), TaskApi.taskService);
        });
  }

  public GraphQL getSchemaGraphqlForUser(String schemaName, String username) {
    UserKey userKey = new UserKey(username);
    return getSchemaGraphqlForUser(schemaName, userKey);
  }

  public GraphQL getSchemaGraphqlForUser(String schemaName, Context ctx) {
    UserKey userKey = getUserKey(ctx);
    return getSchemaGraphqlForUser(schemaName, userKey);
  }

  public GraphQL getSchemaGraphqlForUser(String schemaName, UserKey userKey) {
    return graphqlSchemaCache.get(
        new UserSchemaKey(userKey, schemaName),
        k -> {
          logger.info("create graphqlSchemaApi '{}' cache for user {}", schemaName, userKey);
          Schema schema = getSchemaForUser(schemaName, userKey);
          return graphqlApiFactory.createGraphqlForSchema(schema, TaskApi.taskService);
        });
  }

  private UserKey getUserKey(Context ctx) {
    HttpServletRequest request = ctx.req();

    // check if we are in a session
    HttpSession session = ctx.req().getSession(false);
    if (session != null) {
      String username = (String) session.getAttribute(USERNAME);
      logger.info("found a session for user {}", username);
      if (username != null && !username.isEmpty()) {
        return new UserKey(username);
      }
    }

    // check if we have a token using local admin database
    String authTokenKey = findUsedAuthTokenKey(request);
    if (authTokenKey != null) {
      SqlDatabase database = new SqlDatabase(false);
      return new UserKey(JWTgenerator.getUserFromToken(database, request.getHeader(authTokenKey)));
    }

    // default user
    return new UserKey(ANONYMOUS);
  }

  public String findUsedAuthTokenKey(HttpServletRequest request) {
    for (String authTokenKey : Constants.MOLGENIS_TOKEN) {
      if (request.getHeader(authTokenKey) != null) {
        return authTokenKey;
      }
    }
    return null;
  }

  /**
   * this method is used to reset cache of all sessions, necessary when for example metadata changes
   */
  public void clearAllCaches() {
    oidcController.reloadConfig();
    databaseCache.invalidateAll();
    schemaCache.invalidateAll();
    graphqlSchemaCache.invalidateAll();
    graphqlDatabaseCache.invalidateAll();
    logger.info("cleared all caches");
  }
}
