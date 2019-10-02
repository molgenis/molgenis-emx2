package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;

import java.util.Collection;

public interface Database {

  Collection<String> getSchemaNames() throws MolgenisException;

  Schema createSchema(String name) throws MolgenisException;

  Schema getSchema(String name) throws MolgenisException;

  void dropSchema(String name) throws MolgenisException;

  void addUser(String name) throws MolgenisException;

  boolean hasUser(String user);

  void removeUser(String name) throws MolgenisException;

  void transaction(Transaction transaction) throws MolgenisException;

  void setActiveUser(String username);

  String getActiveUser();

  void clearActiveUser();

  void clearCache();
}
