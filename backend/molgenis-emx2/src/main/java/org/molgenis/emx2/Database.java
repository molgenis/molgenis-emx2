package org.molgenis.emx2;

import java.util.Collection;

public interface Database {

  void tx(Transaction transaction);

  Schema createSchema(String name);

  Schema dropCreateSchema(String testBatch);

  void dropSchema(String name);

  Collection<String> getSchemaNames();

  Schema getSchema(String name);

  void addUser(String name);

  boolean checkUserPassword(String name, String password);

  void setUserPassword(String name, String password);

  boolean hasUser(String user);

  void removeUser(String name);

  void setActiveUser(String username);

  String getActiveUser();

  void clearActiveUser();

  void grantCreateSchema(String user);

  void setListener(DatabaseListener listener);

  DatabaseListener getListener();

  void clearCache();
}
