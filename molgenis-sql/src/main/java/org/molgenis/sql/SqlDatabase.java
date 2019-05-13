package org.molgenis.sql;

import java.util.Collection;

public interface SqlDatabase {
  SqlTable createTable(String name) throws SqlDatabaseException;

  void dropTable(String tableId);

  Collection<SqlTable> getTables();

  SqlTable getTable(String name);

  void close();

  SqlQuery query(String name) throws SqlDatabaseException;
}
