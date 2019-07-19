package org.molgenis;

import java.util.Collection;
import java.util.List;

public interface Table extends Identifiable {

  String getName();

  Schema getSchema();

  // TODO description

  Collection<Column> getColumns();

  Column getColumn(String name);

  Column addColumn(String name, Column.Type type) throws MolgenisException;

  Column addRef(String name, Table otherTable) throws MolgenisException;

  Column addMref(String name, Table otherTable, String mrefTable, String mrefBack)
      throws MolgenisException;

  void removeColumn(String name) throws MolgenisException;

  Collection<Unique> getUniques();

  Unique addUnique(String... name) throws MolgenisException;

  boolean isUnique(String... tableName);

  void removeUnique(String... name) throws MolgenisException;

  String getExtend();

  void setExtend(String extend);

  int insert(Collection<Row> rows) throws MolgenisException;

  int insert(Row... row) throws MolgenisException;

  int update(Row... row) throws MolgenisException;

  int update(Collection<Row> rows) throws MolgenisException;

  int delete(Row... row) throws MolgenisException;

  int delete(Collection<Row> rows) throws MolgenisException;

  void enableRowLevelSecurity() throws MolgenisException;

  Query query();

  List<Row> retrieve() throws MolgenisException;
}
