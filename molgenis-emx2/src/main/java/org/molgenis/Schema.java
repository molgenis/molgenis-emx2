package org.molgenis;

import java.util.Collection;

public interface Schema {
  String getName();

  Table createTable(String name) throws MolgenisException;

  void dropTable(String tableId);

  Collection<Table> getTables();

  Table getTable(String name) throws MolgenisException;

  Query query(String name) throws MolgenisException;
}
