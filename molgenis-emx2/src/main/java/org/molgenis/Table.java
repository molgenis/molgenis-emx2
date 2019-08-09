package org.molgenis;

import java.util.Collection;
import java.util.List;

public interface Table {

  String MOLGENISID = "molgenisid";

  String getName();

  Schema getSchema();

  String getSchemaName();

  Table setPrimaryKey(String... columnNames) throws MolgenisException;

  List<String> getPrimaryKey();

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

  boolean unique(String... tableName);

  void removeUnique(String... name) throws MolgenisException;

  int insert(Row... row) throws MolgenisException;

  int insert(Collection<Row> rows) throws MolgenisException;

  int update(Row... row) throws MolgenisException;

  int update(Collection<Row> rows) throws MolgenisException;

  int delete(Row... row) throws MolgenisException;

  int delete(Collection<Row> rows) throws MolgenisException;

  void deleteByPrimaryKey(Object... name);

  Select select(String... path);

  Where where(String... path);

  Query query();

  List<Row> retrieve() throws MolgenisException;

  <E> List<E> retrieve(String columnName, Class<E> klazz) throws MolgenisException;

  void enableSearch();

  void enableRowLevelSecurity() throws MolgenisException;
}
