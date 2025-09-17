package org.molgenis.emx2.web;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationCachePerUser {
  // use this as a key in the cache
  private record UserSchema(String userName, String schemaName) {}
  ;

  private static Logger logger = LoggerFactory.getLogger(ApplicationCachePerUser.class);

  private Cache<String, Database> databaseCache;
  private Cache<UserSchema, Schema> schemaCache;
  private Cache<String, GraphQL> graphqlDatabaseCache;
  private Cache<UserSchema, GraphQL> graphqlSchemaCache;

  GraphqlApiFactory graphqlApiFactory = new GraphqlApiFactory();

  public ApplicationCachePerUser(int cacheDurationInMinutes) {
    databaseCache =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(cacheDurationInMinutes))
            .maximumSize(10_000)
            .build();
    schemaCache =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(cacheDurationInMinutes))
            .maximumSize(10_000)
            .build();
    graphqlDatabaseCache =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(cacheDurationInMinutes))
            .maximumSize(10_000)
            .build();
    graphqlSchemaCache =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(cacheDurationInMinutes))
            .maximumSize(10_000)
            .build();
  }

  public Database getDatabaseForUser(Context ctx) {
    return databaseCache.get(
        getUser(ctx),
        userName -> {
          logger.info("create database cache for user {}", getUser(ctx));
          SqlDatabase database = new SqlDatabase(false);
          if (!database.hasUser(userName) || !database.getUser(userName).getEnabled()) {
            throw new MolgenisException(
                "User " + userName + " does not exist or has been disabled");
          }
          database.setActiveUser(userName);
          database.addTableListener(new ScriptTableListener(TaskApi.taskSchedulerService));
          database.setBindings(JavaScriptBindings.getBindingsForContext(ctx));
          database.setListener(
              new DatabaseListener() {

                @Override
                public void userChanged() {
                  // should never happen anymore
                  logger.error("user changed, that should never happen");
                  clearAllCaches();
                }

                @Override
                public void afterCommitOfSchemaChanges() {
                  // just to make sure we now clear all caches
                  // we can make it smarter to only invalidate per schema
                  // nb this only fires after commit of schema changes, not of row updates
                  clearAllCaches();
                }
              });

          return database;
        });
  }

  public Schema getSchemaForUser(String schemaName, Context ctx) {
    return schemaCache.get(
        new UserSchema(getUser(ctx), schemaName),
        k -> {
          logger.info("create schema '{}' cache for user {}", schemaName, getUser(ctx));
          return getDatabaseForUser(ctx).getSchema(schemaName);
        });
  }

  public GraphQL getDatabaseGraphqlForUser(Context ctx) {
    return graphqlDatabaseCache.get(
        getUser(ctx),
        k -> {
          logger.info("create graphqlDatabaseApi cache for user {}", getUser(ctx));
          return graphqlApiFactory.createGraphqlForDatabase(
              getDatabaseForUser(ctx), null /* not forget task service */);
        });
  }

  public GraphQL getSchemaGraphqlForUser(String schemaName, Context ctx) {
    return graphqlSchemaCache.get(
        new UserSchema(getUser(ctx), schemaName),
        k -> {
          logger.info("create graphqlSchemaApi '{}' cache for user {}", schemaName, getUser(ctx));
          return graphqlApiFactory.createGraphqlForSchema(getSchemaForUser(schemaName, ctx));
        });
  }

  private String getUser(Context ctx) {
    HttpServletRequest request = ctx.req();

    // check if we are in a session
    HttpSession session = ctx.req().getSession(false);
    if (session != null) {
      String username = (String) session.getAttribute(USERNAME);
      logger.info("found a session for user " + username);
      if (username != null && !username.isEmpty()) {
        return username;
      }
    }

    // check if we have a token using local admin database
    String authTokenKey = findUsedAuthTokenKey(request);
    if (authTokenKey != null) {
      SqlDatabase database = new SqlDatabase(false);
      return JWTgenerator.getUserFromToken(database, request.getHeader(authTokenKey));
    }

    // default user
    return ANONYMOUS;
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
