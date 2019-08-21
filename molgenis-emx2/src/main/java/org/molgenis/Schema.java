package org.molgenis;

import java.util.Collection;

public interface Schema {

  Collection<String> getTableNames() throws MolgenisException;

  String getName();

  Table getTable(String name) throws MolgenisException;

  Table createTableIfNotExists(String name) throws MolgenisException;

  void dropTable(String tableId) throws MolgenisException;

  void grant(Permission permission, String role) throws MolgenisException;

  void revokePermission(Permission permission, String role) throws MolgenisException;

  Query query(String tableName) throws MolgenisException;
}
