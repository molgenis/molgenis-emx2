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
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.sql.JWTgenerator;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.tasks.ScriptTableListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MolgenisBackendCache {
  // use this as a key in the cache
  private record UserSchema(String userName, String schemaName) {}
  ;

  private static Logger logger = LoggerFactory.getLogger(MolgenisBackendCache.class);
  private Cache<String, Database> databaseCache =
      Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(5)).maximumSize(10_000).build();
  private Cache<UserSchema, Schema> schemaCache =
      Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(5)).maximumSize(10_000).build();
  private Cache<String, GraphQL> graphqlDatabaseCache =
      Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(5)).maximumSize(10_000).build();
  private Cache<UserSchema, GraphQL> graphqlSchemaCache =
      Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(5)).maximumSize(10_000).build();

  GraphqlApiFactory graphqlApiFactory = new GraphqlApiFactory();

  public Database getDatabase(Context ctx) {
    return databaseCache.get(
        getUser(ctx),
        userName -> {
          logger.debug("create database cache for user {}", getUser(ctx));
          SqlDatabase database = new SqlDatabase(false);
          database.setActiveUser(userName);
          database.addTableListener(new ScriptTableListener(TaskApi.taskSchedulerService));
          database.setListener(
              new DatabaseListener() {

                @Override
                public void userChanged() {
                  // should never happen anymore
                  logger.error("user changed, that should never happen");
                  clearAllCaches();
                }

                @Override
                public void afterCommit() {
                  // just to make sure we now clear all caches
                  // we can make it smarter to only invalidate per schema
                  clearAllCaches();
                }
              });
          return database;
        });
  }

  public Schema getSchema(String schemaName, Context ctx) {
    return schemaCache.get(
        new UserSchema(getUser(ctx), schemaName),
        k -> {
          logger.debug("create schema '{}' cache for user {}", schemaName, getUser(ctx));
          return getDatabase(ctx).getSchema(schemaName);
        });
  }

  public GraphQL getGraphql(Context ctx) {
    return graphqlDatabaseCache.get(
        getUser(ctx),
        k -> {
          logger.debug("create graphqlDatabaseApi cache for user {}", getUser(ctx));
          return graphqlApiFactory.createGraphqlForDatabase(
              getDatabase(ctx), null /* not forget task service */);
        });
  }

  public GraphQL getGraphqlForSchema(String schemaName, Context ctx) {
    return graphqlSchemaCache.get(
        new UserSchema(getUser(ctx), schemaName),
        k -> {
          logger.debug("create graphqlSchemaApi '{}' cache for user {}", schemaName, getUser(ctx));
          return graphqlApiFactory.createGraphqlForSchema(getSchema(schemaName, ctx));
        });
  }

  private String getUser(Context ctx) {
    HttpServletRequest request = ctx.req();

    // check if we are in a session
    HttpSession session = ctx.req().getSession(false);
    if (session != null) {
      return (String) session.getAttribute(USERNAME);
    }

    // check if we have a token
    String authTokenKey = findUsedAuthTokenKey(request);
    if (authTokenKey != null) {
      SqlDatabase database = new SqlDatabase(false);
      database.addTableListener(new ScriptTableListener(TaskApi.taskSchedulerService));
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
    logger.debug("cleared all caches");
  }
}
