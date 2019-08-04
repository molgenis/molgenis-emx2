package org.molgenis;

import java.util.Collection;
import java.util.List;

public interface Table extends Identifiable {

  String getName();

  Schema getSchema();

  // TODO description

  Collection<Column> getColumns();

  Column getColumn(String name) throws MolgenisException;

  Column addColumn(String name, Type type) throws MolgenisException;

  Column addRef(String name, String otherTable) throws MolgenisException;

  Column addRef(String name, String otherTable, String otherField) throws MolgenisException;

  Column addRefArray(String name, String otherTable, String otherColumn) throws MolgenisException;

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

  String getExtend();

  void setExtend(String extend);

  int insert(Collection<Row> rows) throws MolgenisException;

  int insert(Row... row) throws MolgenisException;

  int update(Row... row) throws MolgenisException;

  int update(Collection<Row> rows) throws MolgenisException;

  int delete(Row... row) throws MolgenisException;

  int delete(Collection<Row> rows) throws MolgenisException;

  void enableSearch();

  void enableRowLevelSecurity() throws MolgenisException;

  Query query();

  Select select(String... path);

  List<Row> retrieve() throws MolgenisException;

  String getSchemaName();
}
