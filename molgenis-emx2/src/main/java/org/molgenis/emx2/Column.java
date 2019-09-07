package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;

import static org.molgenis.emx2.Type.STRING;

public class Column {
  private TableMetadata table;

  private String columnName;
  private Type type;
  private boolean nullable = false;
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

  public Column(TableMetadata table, String columnName, Type type) {
    this.table = table;
    this.columnName = columnName;
    this.type = type;
  }

  public Column addColumn(String name) throws MolgenisException {
    return this.getTable().addColumn(name, STRING);
  }

  public Column addColumn(String name, Type type) throws MolgenisException {
    return this.getTable().addColumn(name, type);
  }

  public Column addRef(String name, String toTable) throws MolgenisException {
    return this.getTable().addRef(name, toTable);
  }

  public Column addRef(String name, String toTable, String toColumn) throws MolgenisException {
    return this.getTable().addRef(name, toTable, toColumn);
  }

  public Column addRefArray(String name, String toTable) throws MolgenisException {
    return this.getTable().addRefArray(name, toTable);
  }

  public Column addRefArray(String name, String toTable, String toColumn) throws MolgenisException {
    return this.getTable().addRef(name, toTable, toColumn);
  }

  public Column primaryKey() throws MolgenisException {
    this.table.setPrimaryKey(this.columnName);
    return this;
  }

  public TableMetadata getTable() {
    return table;
  }

  public String getColumnName() {
    return columnName;
  }

  public Type getType() {
    return type;
  }

  public Boolean getNullable() {
    return nullable;
  }

  public String getRefTableName() {
    return refTable;
  }

  public String getRefColumnName() {
    return this.refColumn;
  }

  public String getReverseColumnName() {
    return this.reverseName;
  }

  public String getReverseRefColumn() {
    return this.reverseRefColumn;
  }

  public Column getRefColumn() throws MolgenisException {
    if (getRefColumnName() == null) return null;
    else
      return getTable()
          .getSchema()
          .getTableMetadata(getRefTableName())
          .getColumn(getRefColumnName());
  }

  public Column setNullable(boolean nillable) {
    this.nullable = nillable;
    return this;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getColumnName()).append(" ");
    if (Type.REF.equals(getType()))
      builder.append("ref(").append(refTable).append(",").append(refColumn).append(")");
    else builder.append(getType().toString().toLowerCase());
    if (Boolean.TRUE.equals(getNullable())) builder.append(" setNullable");
    return builder.toString();
  }

  public Boolean getReadonly() {
    return readonly;
  }

  public void setReadonly(boolean readonly) {
    this.readonly = readonly;
  }

  public String getDescription() {
    return description;
  }

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

  public Boolean isUnique() {
    return getTable().isUnique(getColumnName());
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public Column setUnique(boolean unique) throws MolgenisException {
    if (unique) getTable().addUnique(this.getColumnName());
    else getTable().removeUnique(this.getColumnName());
    return this;
  }

  public String getMrefJoinTableName() {
    return joinTable;
  }

  public Column setReference(String refTable, String refColumn) {
    this.refTable = refTable;
    this.refColumn = refColumn;
    return this;
  }

  public Column setReverseReference(String reverseName, String reverseRefColumn) {
    this.reverseName = reverseName;
    this.reverseRefColumn = reverseRefColumn;
    return this;
  }

  public Column setJoinTable(String joinTableName) {
    this.joinTable = joinTableName;
    return this;
  }

  public TableMetadata addUnique(String... columnNames) throws MolgenisException {

    return getTable().addUnique(columnNames);
  }

  public TableMetadata setPrimaryKey(String... columnNames) throws MolgenisException {
    return getTable().setPrimaryKey(columnNames);
  }

  public ReferenceMultiple addRefMultiple(String... columnNames) throws MolgenisException {
    return getTable().addRefMultiple(columnNames);
  }

  public Column setPrimaryKey(boolean primaryKey) throws MolgenisException {
    if (primaryKey) this.table.setPrimaryKey(this.getColumnName());
    else throw new UnsupportedOperationException();
    return this;
  }

  public boolean isPrimaryKey() {
    return table.isPrimaryKey(this.getColumnName());
  }
}
