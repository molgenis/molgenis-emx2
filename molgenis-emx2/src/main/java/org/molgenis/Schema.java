package org.molgenis;

import java.util.Collection;

public interface Schema {

  String getName();

  Collection<String> getTableNames() throws MolgenisException;

  Table getTable(String name) throws MolgenisException;

  Table createTableIfNotExists(String name) throws MolgenisException;

  void dropTable(String tableId) throws MolgenisException;

  void grantAdmin(String user);

  void grantManage(String user) throws MolgenisException;

  void grantEdit(String user) throws MolgenisException;

  void grantView(String user) throws MolgenisException;

  Query query(String tableName) throws MolgenisException;
}
