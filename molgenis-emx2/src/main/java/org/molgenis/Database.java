package org.molgenis;

import java.util.Collection;

public interface Database {

  String getName();

  void setName(String name);

  Schema createSchema(String name) throws MolgenisException;

  void createUser(String name) throws MolgenisException;

  Schema getSchema(String name) throws MolgenisException;

  void grantRoleToUser(String role, String user) throws MolgenisException;

  void transaction(Transaction transaction) throws MolgenisException;

  void transaction(Transaction transaction, String role) throws MolgenisException;
}
