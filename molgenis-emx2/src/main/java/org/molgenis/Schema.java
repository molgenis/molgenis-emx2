package org.molgenis;

import java.util.Collection;

public interface Schema {
  Table createTable(String name) throws MolgenisException;

  void dropTable(String tableId);

  Collection<String> getTables();

  Table getTable(String name);
}
