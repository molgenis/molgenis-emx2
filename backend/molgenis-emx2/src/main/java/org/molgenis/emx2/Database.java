package org.molgenis.emx2;

import java.util.Collection;

public interface Database {

  void tx(Transaction transaction);

  void init();

  Schema createSchema(String name);

  Schema dropCreateSchema(String name);

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

  boolean inTx();

  void clearCache();

  String getDatabaseVersion();
}
