package org.molgenis.emx2;

import java.util.Collection;

public interface Database {

  // transactions

  void tx(Transaction transaction);

  // schema management

  Schema createSchema(String name);

  /* get Schema names without retrieving all schema contents*/
  Collection<String> getSchemaNames();

  Schema getSchema(String name);

  void dropSchema(String name);

  // user management
  // todo might needs its own interface

  void addUser(String name);

  boolean checkUserPassword(String name, String password);

  void setUserPassword(String name, String password);

  boolean hasUser(String user);

  void removeUser(String name);

  // session management

  void setActiveUser(String username);

  String getActiveUser();

  void clearActiveUser();

  void grantCreateSchema(String user);

  // change listener

  void setListener(DatabaseListener listener);

  DatabaseListener getListener();

  // brute force empty cache

  void clearCache();

  Schema dropCreateSchema(String testBatch);
}
