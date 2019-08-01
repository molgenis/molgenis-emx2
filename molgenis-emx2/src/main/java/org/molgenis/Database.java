package org.molgenis;

import java.util.Collection;

public interface Database {
  void setDeferChecks(boolean shouldDefer);

  enum RowLevelSecurity {
    MG_EDIT_ROLE
  }

  enum Prefix {
    MGUSER_,
    MGROLE_
  }

  enum Roles {
    _VIEWER,
    _EDITOR,
    _MANAGER,
    _ADMIN
  }

  String getName();

  void setName(String name);

  Schema createSchema(String name) throws MolgenisException;

  Schema getSchema(String name) throws MolgenisException;

  Collection<String> getSchemaNames() throws MolgenisException;

  void createUser(String name) throws MolgenisException;

  void grantRole(String role, String user) throws MolgenisException;

  void transaction(Transaction transaction) throws MolgenisException;

  void transaction(String role, Transaction transaction) throws MolgenisException;

  void clearCache();
}
