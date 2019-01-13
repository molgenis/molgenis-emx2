package org.molgenis.sql;

import java.util.Collection;

public interface SqlDatabase {
  SqlTable createTable(String name);

  Collection<SqlTable> getTables();

  SqlTable getTable(String name);

  void dropTable(String tableId);

  void close();

  SqlQuery query(String name) throws SqlDatabaseException;
}
