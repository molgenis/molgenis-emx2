package org.molgenis.emx2;

import java.util.Collection;

public interface Database {

  Collection<String> getSchemaNames();

  Schema createSchema(String name);

  Schema getSchema(String name);

  void dropSchema(String name);

  void addUser(String name);

  boolean hasUser(String user);

  void removeUser(String name);

  void grantCreateSchema(String user);

  void tx(Transaction transaction);

  void setActiveUser(String username);

  String getActiveUser();

  void clearActiveUser();

  void clearCache();
}
