package org.molgenis.beans;

import org.molgenis.*;

import static org.molgenis.Type.STRING;

public class ColumnMetadata implements Column {
  private Table table;
  private String name;
  private Type type;
  private boolean nullable;
  private String refTable;
  private String refColumn;
  private String reverseName;
  private String reverseRefColumn;
  private String joinTable;
  private boolean readonly;
  private String visible;
  private String description;
  private String validation;
  private String defaultValue;

  public ColumnMetadata(Table table, String name, Type type, Boolean isNullable) {
    this.table = table;
    this.name = name;
    this.type = type;
    this.nullable = isNullable;
  }

  public ColumnMetadata(
      Table table,
      String name,
      Type type,
      String otherTable,
      String otherColumn,
      Boolean isNullable) {
    this(table, name, type, isNullable);
    this.refTable = otherTable;
    this.refColumn = otherColumn;
  }

  public ColumnMetadata(
      Table table,
      String name,
      Type type,
      String refTable,
      String refColumn,
      String reverseName,
      String reverseRefColumn,
      String joinTableName) {
    this(table, name, type, refTable, refColumn, true);
    this.reverseName = reverseName;
    this.reverseRefColumn = reverseRefColumn;
    this.joinTable = joinTableName;
  }

  @Override
  public Column addColumn(String name) throws MolgenisException {
    return this.getTable().addColumn(name, STRING);
  }

  @Override
  public Column addColumn(String name, Type type) throws MolgenisException {
    return this.getTable().addColumn(name, type);
  }

  @Override
  public Column addRef(String name, String toTable) throws MolgenisException {
    return this.getTable().addRef(name, toTable);
  }

  @Override
  public Column addRef(String name, String toTable, String toColumn) throws MolgenisException {
    return this.getTable().addRef(name, toTable, toColumn);
  }

  @Override
  public Column addRefArray(String name, String toTable) throws MolgenisException {
    return this.getTable().addRefArray(name, toTable);
  }

  @Override
  public Column addRefArray(String name, String toTable, String toColumn) throws MolgenisException {
    return this.getTable().addRef(name, toTable, toColumn);
  }

  @Override
  public void primaryKey() throws MolgenisException {
    this.table.setPrimaryKey(this.name);
  }

  @Override
  public Table getTable() {
    return table;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public Boolean isNullable() {
    return nullable;
  }

  @Override
  public String getRefTable() {
    return refTable;
  }

  @Override
  public String getRefColumn() throws MolgenisException {
    return this.refColumn;
  }

  @Override
  public String getReverseColumnName() {
    return this.reverseName;
  }

  @Override
  public String getReverseRefColumn() {
    return this.reverseRefColumn;
  }

  @Override
  public Column nullable(boolean nillable) throws MolgenisException {
    this.nullable = nillable;
    return this;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getName()).append(" ");
    if (Type.REF.equals(getType())) builder.append("ref(").append(refTable).append(")");
    else builder.append(getType().toString().toLowerCase());
    if (isNullable()) builder.append(" nullable");
    return builder.toString();
  }

  @Override
  public Boolean isReadonly() {
    return readonly;
  }

  @Override
  public void setReadonly(boolean readonly) {
    this.readonly = readonly;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  public String getVisible() {
    return visible;
  }

  public void setVisible(String visible) {
    this.visible = visible;
  }

  public String getValidation() {
    return validation;
  }

  public void setValidation(String validation) {
    this.validation = validation;
  }

  @Override
  public Boolean isUnique() {
    return getTable().unique(getName());
  }

  @Override
  public String getDefaultValue() {
    return defaultValue;
  }

  @Override
  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public Column unique() throws MolgenisException {
    getTable().addUnique(this.getName());
    return this;
  }

  @Override
  public String getJoinTable() {
    return joinTable;
  }

  protected Column setReference(String refTable, String refColumn) {
    this.refTable = refTable;
    this.refColumn = refColumn;
    return this;
  }
}
