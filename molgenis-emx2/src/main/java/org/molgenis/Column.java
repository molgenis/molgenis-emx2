package org.molgenis;

public interface Column {

  String getName();

  Type getType();

  Boolean getNullable();

  Boolean getReadonly();

  Boolean isUnique();

  String getDefaultValue();

  String getRefTableName();

  String getRefColumnName() throws MolgenisException;

  String getReverseColumnName();

  String getReverseRefColumn();

  String getMrefJoinTableName();

  String getDescription();

  // implementation methods

  Table getTable();

  Column getRefColumn() throws MolgenisException;

  Column setNullable(boolean nillable) throws MolgenisException;

  Column setUnique(boolean unique) throws MolgenisException;

  Column primaryKey() throws MolgenisException;

  void setReadonly(boolean readonly);

  void setDescription(String description);

  void setDefaultValue(String defaultValue);

  // for fluent api in Table
  Column addColumn(String name) throws MolgenisException;

  Column addColumn(String name, Type type) throws MolgenisException;

  Column addRef(String name, String toTable) throws MolgenisException;

  Column addRef(String name, String toTable, String toColumn) throws MolgenisException;

  Column addRefArray(String name, String toTable) throws MolgenisException;

  Column addRefArray(String name, String toTable, String toColumn) throws MolgenisException;

  @Deprecated
  Column setReference(String refTable, String refColumn);

  @Deprecated
  Column setReverseReference(String reverseName, String reverseRefColumn);

  @Deprecated
  Column setJoinTable(String joinTableName);
}
