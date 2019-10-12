package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;

import java.util.Collection;

public interface Database {

  Collection<String> getSchemaNames();

  Schema createSchema(String name);

  Schema getSchema(String name);

  void dropSchema(String name);

  void addUser(String name);

  void grantCreateSchema(String user);

  boolean hasUser(String user);

  void removeUser(String name);

  void transaction(Transaction transaction);

  void setActiveUser(String username);

  String getActiveUser();

  void clearActiveUser();

  void clearCache();
}
