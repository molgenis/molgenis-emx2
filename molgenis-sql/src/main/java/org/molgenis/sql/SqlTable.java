package org.molgenis.sql;

import java.util.Collection;

public interface SqlTable {
  String getName();

  Collection<SqlColumn> getColumns();

  SqlColumn getColumn(String name);

  SqlColumn addColumn(String name, SqlType type) throws SqlDatabaseException;

  SqlColumn addRef(String name, SqlTable otherTable) throws SqlDatabaseException;

  SqlColumn addMref(String name, SqlTable otherTable, String joinTable) throws SqlDatabaseException;

  void removeColumn(String name) throws SqlDatabaseException;

  Collection<SqlUnique> getUniques();

  SqlUnique addUnique(String... name) throws SqlDatabaseException;

  boolean isUnique(String... tableName);

  void removeUnique(String... name) throws SqlDatabaseException;

  void insert(Collection<SqlRow> rows) throws SqlDatabaseException;

  int update(Collection<SqlRow> rows) throws SqlDatabaseException;

  int delete(Collection<SqlRow> rows) throws SqlDatabaseException;

  void insert(SqlRow row) throws SqlDatabaseException;

  void update(SqlRow row) throws SqlDatabaseException;

  void delete(SqlRow row) throws SqlDatabaseException;
}
