package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.ANONYMOUS;

import graphql.GraphQL;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.DatabaseListener;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.tasks.TaskService;
import org.molgenis.emx2.tasks.TaskServiceInMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserSession {
  private static final Logger logger = LoggerFactory.getLogger(UserSession.class);

  // singletons shared between UserSession instances
  private static GraphqlApiPerUserCache cache = new GraphqlApiPerUserCache();
  private static TaskService taskService = new TaskServiceInMemory();

  private String userName = ANONYMOUS;
  private DatabaseListener databaseListener =
      new DatabaseListener() {
        @Override
        public void afterCommit() {
          clearCache();
        }
      };

  public UserSession() {}

  public UserSession(String user) {
    this.setSessionUser(user);
  }

  public UserSession(GraphqlApiPerUserCache cache, TaskService taskService) {
    // changes static, maybe better use setting
    this.cache = cache;
    this.taskService = taskService;
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
        .tx(
            db -> {
              // uplift permissions
              String activeUser = db.getActiveUser();
              try {
                db.becomeAdmin();
                db.addUser(userName);
                db.setUserPassword(userName, passWord);
              } finally {
                // always lift down again
                db.setActiveUser(activeUser);
              }
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
    return cache.getDatabase(this);
  }

  public GraphQL getGraphqlForDatabase() {
    return cache.getGraphqlForDatabase(this);
  }

  public GraphQL getGraphqlForSchema(String schemaName) {
    return cache.getGraphqlForSchema(this, schemaName);
  }

  public void clearCache() {
    cache.clearCache();
  }

  public DatabaseListener getDatabaseChangeListener() {
    return databaseListener;
  }
}
