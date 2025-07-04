package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.ANONYMOUS;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.tasks.TaskService;

public class GraphqlSession {
  private String userName;
  private Database database;
  private TaskService taskService;

  protected GraphqlSession() {
    // for subclasses that know what they are doing
  }

  public GraphqlSession(Database database) {
    this.database = database;
  }

  public GraphqlSession(Database database, TaskService taskService) {
    this(database);
    this.taskService = taskService;
  }

  public String getSessionUser() {
    return userName;
  }

  public void signOut() {
    this.database.clearActiveUser();
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

  public Database getDatabase() {
    return database;
  }

  public void signIn(String userName, String passWord) {
    if (getDatabase().hasUser(userName)
        && getDatabase().checkUserPassword(userName, passWord)
        && getDatabase().getUser(userName).getEnabled()) {
      this.database.setActiveUser(userName);
      this.userName = userName;
      clearCache();
    } else {
      throw new MolgenisException(
          String.format(
              "Sign in as '%s' failed: user or password unknown or user disabled", userName));
    }
  }

  public void setSessionUser(String user) {
    clearCache();
    this.userName = user;
  }

  public void clearCache() {
    // not yet implemented here
  }

  public TaskService getTaskService() {
    return taskService;
  }
}
