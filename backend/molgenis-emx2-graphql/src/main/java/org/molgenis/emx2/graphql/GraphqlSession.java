package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.ANONYMOUS;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import graphql.GraphQL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.DatabaseListener;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.tasks.TaskService;
import org.molgenis.emx2.tasks.TaskServiceInMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Lightweight session that uses caches to maintain the bigger objects */
public class GraphqlSession {
  private static final Logger logger = LoggerFactory.getLogger(GraphqlSession.class);

  // static caches between the sessions
  private static GraphqlApiFactory graphqlApiFactory = new GraphqlApiFactory();
  private static Cache<String, Database> databaseCachePerUser =
      Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(5, TimeUnit.MINUTES).build();
  private static Cache<String, GraphQL> schemaApiCachePerUser =
      Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(5, TimeUnit.MINUTES).build();
  private static Cache<String, GraphQL> databaseApiCachePerUser =
      Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(5, TimeUnit.MINUTES).build();
  private static TaskService taskService = new TaskServiceInMemory();

  private String userName = ANONYMOUS;

  private DatabaseListener databaseListener =
      new DatabaseListener() {
        @Override
        public void afterCommit() {
          if (isDirty()) {
            clearCache();
            // clean commit cache
            super.afterCommit();
          }
        }
      };

  public GraphqlSession(String user) {
    this.setSessionUser(user);
  }

  public String getSessionUser() {
    return userName;
  }

  public void signOut() {
    this.userName = ANONYMOUS;
    clearCache();
  }

  public void signUp(String userName, String passWord) {
    if (passWord == null) {
      throw new MolgenisException("Password cannot be not null");
    }
    if (passWord.length() < 8) {
      throw new MolgenisException("Password too short");
    }
    if (getDatabase().hasUser(userName)) {
      throw new MolgenisException("Email already exists");
    }
    getDatabase()
        .runAsAdmin(
            db -> {
              db.addUser(userName);
              db.setUserPassword(userName, passWord);
            });
  }

  public void signIn(String userName, String passWord) {
    if (getDatabase().hasUser(userName)
        && getDatabase().checkUserPassword(userName, passWord)
        && getDatabase().getUser(userName).getEnabled()) {
      this.userName = userName;
      clearCache();
    } else {
      throw new MolgenisException(
          String.format(
              "Sign in as '%s' failed: user or password unknown or user disabled", userName));
    }
  }

  public void setSessionUser(String user) {
    logger.info("Changing session from '{}' to '{}'", this.userName, user);
    clearCache();
    this.userName = user;
  }

  public TaskService getTaskService() {
    return taskService;
  }

  public Database getDatabase() {
    String userName = this.getSessionUser();
    Database database =
        databaseCachePerUser.get(
            userName,
            key -> {
              logger.info("creating database instance for user '{}'", userName);
              SqlDatabase db = new SqlDatabase(userName);
              db.setListener(this.getDatabaseChangeListener());
              // TODO db.addTableListener(new ScriptTableListener(TaskApi.taskSchedulerService));
              // TODO db.setBindings(JavaScriptBindings.getBindingsForSession(session));
              return db;
            });
    logger.info("returned cached database instance for user '{}'", userName);
    return database;
  }

  public GraphQL getGraphqlForDatabase() {
    String userName = this.getSessionUser();
    GraphQL graphQL =
        databaseApiCachePerUser.get(
            userName,
            key -> {
              logger.info("creating graphql database api for user '{}'", userName);
              return graphqlApiFactory.createGraphqlForDatabase(this);
            });
    logger.info("returned cached graphql database api for user '{}'", userName);
    return graphQL;
  }

  public GraphQL getGraphqlForSchema(String schemaName) {
    Objects.requireNonNull(schemaName);
    String userName = this.getSessionUser();
    String userNameAndSchemaName = userName + ":" + schemaName;
    GraphQL graphQL =
        schemaApiCachePerUser.get(
            userNameAndSchemaName,
            key -> {
              Database database = getDatabase();
              logger.info("creating graphql schema api '{}' for user '{}'", schemaName, userName);
              return graphqlApiFactory.createGraphqlForSchema(database.getSchema(schemaName), this);
            });
    logger.info("returned cached graphql schema api '{}' for user '{}'", schemaName, userName);
    return graphQL;
  }

  public void clearCache() {
    // todo make more finegrained
    databaseCachePerUser.invalidateAll();
    schemaApiCachePerUser.invalidateAll();
    databaseApiCachePerUser.invalidateAll();
    logger.info("cleared caches");
  }

  public DatabaseListener getDatabaseChangeListener() {
    return databaseListener;
  }

  protected void setDatabaseListener(DatabaseListener databaseListener) {
    this.databaseListener = databaseListener;
  }
}
