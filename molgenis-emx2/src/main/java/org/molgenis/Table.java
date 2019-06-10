package org.molgenis;

import java.util.Collection;

public interface Table {

  String getName();

  // TODO description

  // TODO identifier

  Collection<Column> getColumns();

  Column getColumn(String name);

  Column addColumn(String name, Column.Type type) throws DatabaseException;

  Column addRef(String name, Table otherTable) throws DatabaseException;

  Column addMref(String name, Table otherTable, String joinTable) throws DatabaseException;

  void removeColumn(String name) throws DatabaseException;

  Collection<Unique> getUniques();

  Unique addUnique(String... name) throws DatabaseException;

  boolean isUnique(String... tableName);

  void removeUnique(String... name) throws DatabaseException;

  String getExtend();

  void setExtend(String extend);
}
