package org.molgenis;

public interface Column {
  Column addColumn(String name) throws MolgenisException;

  Column addColumn(String name, Type type) throws MolgenisException;

  Table getTable();

  String getName();

  Type getType();

  Boolean isNullable();

  Boolean isReadonly();

  Boolean isUnique();

  String getDefaultValue();

  String getRefTable();

  String getRefColumn() throws MolgenisException;

  String getReverseColumnName();

  String getReverseRefColumn();

  String getJoinTable();

  String getDescription();

  Column nullable(boolean nillable) throws MolgenisException;

  void setReadonly(boolean readonly);

  void setDescription(String description);

  void setDefaultValue(String defaultValue);

  Column unique() throws MolgenisException;
}
