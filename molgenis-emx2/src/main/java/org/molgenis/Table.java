package org.molgenis;

import java.util.Collection;

public interface Table extends Identifiable {

  String getName();

  // TODO description

  // TODO identifier

  Collection<Column> getColumns();

  Column getColumn(String name);

  Column addColumn(String name, Column.Type type) throws MolgenisException;

  Column addRef(String name, Table otherTable) throws MolgenisException;

  Column addMref(String name, Table otherTable, String joinTable) throws MolgenisException;

  void removeColumn(String name) throws MolgenisException;

  Collection<Unique> getUniques();

  Unique addUnique(String... name) throws MolgenisException;

  boolean isUnique(String... tableName);

  void removeUnique(String... name) throws MolgenisException;

  String getExtend();

  void setExtend(String extend);
}
