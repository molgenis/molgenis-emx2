package org.molgenis.emx2;

import java.util.Collection;
import java.util.List;

public interface Database {

  void tx(Transaction transaction);

  void init();

  Schema createSchema(String name);

  Schema createSchema(String name, String description);

  Schema updateSchema(String name, String description);

  Schema dropCreateSchema(String name);

  void dropSchema(String name);

  Collection<String> getSchemaNames();

  Collection<SchemaInfo> getSchemaInfos();

  Schema getSchema(String name);

  void addUser(String name);

  boolean checkUserPassword(String name, String password);

  void setUserPassword(String name, String password);

  boolean hasUser(String user);

  List<User> getUsers(int limit, int offset);

  void removeUser(String name);

  void setActiveUser(String username);

  String getActiveUser();

  void clearActiveUser();

  void grantCreateSchema(String user);

  void setListener(DatabaseListener listener);

  DatabaseListener getListener();

  boolean inTx();

  void clearCache();

  Integer getDatabaseVersion();

  int countUsers();

  /** for testing purposes */
  String getAdminUserName();

  /**
   * check if the user that is logged in is admin
   *
   * <p>Shorthand for getAdminUserName().equals(getActiveUser()) to help understand code
   */
  boolean isAdmin();

  /**
   * change active user to admin
   *
   * <p>Shorthand for setActiveUser(getAdminUserName()) to help understand code
   */
  void becomeAdmin();
}
