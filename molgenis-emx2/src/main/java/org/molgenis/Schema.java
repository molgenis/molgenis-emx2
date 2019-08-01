package org.molgenis;

import java.util.Collection;

public interface Schema {
  String getName();

  Table createTable(String name) throws MolgenisException;

  Table getTable(String name) throws MolgenisException;

  void dropTable(String tableId) throws MolgenisException;

  Collection<String> getTableNames() throws MolgenisException;

  void grantAdmin(String user);

  void grantManage(String user);

  void grantEdit(String user);

  void grantView(String user);
}
