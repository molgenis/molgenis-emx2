package org.molgenis.emx2;

import static org.molgenis.emx2.ColumnType.STRING;

public class Column {
  private TableMetadata table;

  private String columnName;
  private ColumnType columnType;
  private boolean nullable = false;
  private String refTableName;
  private String refColumn;
  private String reverseRefTableName;
  private String reverseRefColumn;
  private String joinTable;
  private boolean readonly;
  private String visible;
  private String description;
  private String validation;
  private String defaultValue;

  public Column(TableMetadata table, String columnName, ColumnType columnType) {
    // todo check not null
    this.table = table;
    this.columnName = columnName;
    this.columnType = columnType;
  }

  public Column addColumn(String name) {
    return this.getTable().addColumn(name, STRING);
  }

  public Column addColumn(String name, ColumnType columnType) {
    return this.getTable().addColumn(name, columnType);
  }

  // todo can we remove these and instead use setReference and setReverseReference?
  public Column addRef(String name, String toTable) {
    return this.getTable().addRef(name, toTable);
  }

  public Column addRef(String name, String toTable, String toColumn) {
    return this.getTable().addRef(name, toTable, toColumn);
  }

  public Column addRefArray(String name, String toTable) {
    return this.getTable().addRefArray(name, toTable);
  }

  public Column addRefArray(String name, String toTable, String toColumn) {
    return this.getTable().addRef(name, toTable, toColumn);
  }

  public Column primaryKey() {
    this.table.setPrimaryKey(this.columnName);
    return this;
  }

  public TableMetadata getTable() {
    return table;
  }

  public String getColumnName() {
    return columnName;
  }

  public ColumnType getColumnType() {
    return columnType;
  }

  public Boolean getNullable() {
    return nullable;
  }

  public String getRefTableName() {
    return refTableName;
  }

  public String getRefColumnName() {
    return this.refColumn;
  }

  public String getReverseRefTableName() {
    return this.reverseRefTableName;
  }

  public String getReverseRefColumn() {
    return this.reverseRefColumn;
  }

  public Column getRefColumn() {
    // todo: should return primary key column?
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
    if (ColumnType.REF.equals(getColumnType()))
      builder.append("ref(").append(refTableName).append(",").append(refColumn).append(")");
    else builder.append(getColumnType().toString().toLowerCase());
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

  public Boolean isUnique() {
    return getTable().isUnique(getColumnName());
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public Column setUnique(boolean unique) {
    if (unique) getTable().addUnique(this.getColumnName());
    else getTable().removeUnique(this.getColumnName());
    return this;
  }

  public String getMrefJoinTableName() {
    return joinTable;
  }

  public Column setMrefJoinTable(String joinTableName) {
    this.joinTable = joinTableName;
    return this;
  }

  public Column setReference(String refTable, String refColumn) {
    this.refTableName = refTable;
    this.refColumn = refColumn;
    return this;
  }

  public Column setReverseReference(String reverseName, String reverseRefColumn) {
    this.reverseRefTableName = reverseName;
    this.reverseRefColumn = reverseRefColumn;
    return this;
  }

  public TableMetadata addUnique(String... columnNames) {

    return getTable().addUnique(columnNames);
  }

  public TableMetadata setPrimaryKey(String... columnNames) {
    return getTable().setPrimaryKey(columnNames);
  }

  public ReferenceMultiple addRefMultiple(String... columnNames) {
    return getTable().addRefMultiple(columnNames);
  }

  public Column setPrimaryKey(boolean primaryKey) {
    if (primaryKey) this.table.setPrimaryKey(this.getColumnName());
    return this;
  }

  public boolean isPrimaryKey() {
    return table.isPrimaryKey(this.getColumnName());
  }
}
