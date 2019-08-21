package org.molgenis;

import java.util.Collection;
import java.util.List;

public interface Table {

  String getName();

  String getSchemaName();

  Schema getSchema();

  Table setPrimaryKey(String... columnNames) throws MolgenisException;

  String[] getPrimaryKey();

  List<Column> getColumns();

  Column getColumn(String name) throws MolgenisException;

  Column addColumn(String name) throws MolgenisException;

  Column addColumn(String name, Type type) throws MolgenisException;

  Column addColumn(Column column) throws MolgenisException;

  Column addRef(String name, String toTable) throws MolgenisException;

  Column addRef(String name, String toTable, String toColumn) throws MolgenisException;

  ReferenceMultiple addRefMultiple(String... name) throws MolgenisException;

  Column addRefArray(String name, String toTable) throws MolgenisException;

  Column addRefArray(String name, String toTable, String toColumn) throws MolgenisException;

  ReferenceMultiple addRefArrayMultiple(String... name) throws MolgenisException;

  Column addMref(
      String name,
      String refTable,
      String refColumn,
      String reverseName,
      String reversRefColumn,
      String joinTableName)
      throws MolgenisException;

  void removeColumn(String name) throws MolgenisException;

  Collection<Unique> getUniques();

  Unique addUnique(String... name) throws MolgenisException;

  boolean isUnique(String... tableName);

  void removeUnique(String... name) throws MolgenisException;

  int insert(Row... row) throws MolgenisException;

  int insert(Collection<Row> rows)
      throws MolgenisException; // todo: use Iterable or Iterator instead?

  int update(Row... row) throws MolgenisException;

  int update(Collection<Row> rows) throws MolgenisException; // todo: update based on secondary key.

  int delete(Row... row) throws MolgenisException;

  int delete(Collection<Row> rows) throws MolgenisException;

  void deleteByPrimaryKey(Object... name); // todo: remove?

  Select select(String... path);

  Where where(String... path);

  Query query();

  List<Row> retrieve() throws MolgenisException;

  <E> List<E> retrieve(String columnName, Class<E> klazz) throws MolgenisException;

  void enableSearch(); // todo: decide if standard on.

  void enableRowLevelSecurity() throws MolgenisException; // todo: decide if standard on
}
