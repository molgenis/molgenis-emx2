package org.molgenis.emx2;

import static org.molgenis.emx2.ColumnType.STRING;

public class Column {
  private TableMetadata table;
  private String columnName;
  private ColumnType columnType;

  // options
  private boolean nullable = false;
  private boolean readonly;
  private String description;
  private String defaultValue;
  private Boolean indexed;

  // relationships
  private String refTable;
  private String refColumn;
  private String mappedBy;

  public Column(TableMetadata table, String columnName, ColumnType columnType) {
    this.table = table;
    this.columnName = columnName;
    this.columnType = columnType;
  }

  public Column primaryKey() {
    this.table.setPrimaryKey(this.columnName);
    return this;
  }

  public Column setPrimaryKey(boolean primaryKey) {
    if (primaryKey) this.table.setPrimaryKey(this.getName());
    return this;
  }

  public boolean isPrimaryKey() {
    return this.getName().equals(this.table.getPrimaryKey());
  }

  public TableMetadata getTable() {
    return table;
  }

  public String getName() {
    return columnName;
  }

  public ColumnType getColumnType() {
    return columnType;
  }

  public String getRefColumnName() {
    return this.refColumn;
  }

  public String getRefTableName() {
    return this.refTable;
  }

  public Column getRefColumn() {
    if (getRefColumnName() == null) {
      return null;
    } else
      return getTable()
          .getSchema()
          .getTableMetadata(getRefTableName())
          .getColumn(getRefColumnName());
  }

  public Boolean isUnique() {
    return getTable().isUnique(getName());
  }

  public Column setUnique(boolean unique) {
    if (unique) getTable().addUnique(this.getName());
    else getTable().removeUnique(this.getName());
    return this;
  }

  public Boolean getReadonly() {
    return readonly;
  }

  public void setReadonly(boolean readonly) {
    this.readonly = readonly;
  }

  public Boolean getNullable() {
    return nullable;
  }

  public Column setNullable(boolean nillable) {
    this.nullable = nillable;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public String getMappedBy() {
    return mappedBy;
  }

  public Column setMappedBy(String columnName) {
    this.mappedBy = columnName;
    return this;
  }

  public Column setReference(String refTable, String refColumn) {
    this.refTable = refTable;
    this.refColumn = refColumn;
    return this;
  }

  public Column setReference(String toTable, String toColumn, String via) {
    setReference(toTable, toColumn);
    this.mappedBy = via;
    return this;
  }

  public Column setIndexed(boolean indexed) {
    this.indexed = indexed;
    return this;
  }

  public Boolean getIndexed() {
    return indexed;
  }

  protected void setTable(TableMetadata tableMetadata) {
    this.table = tableMetadata;
  }

  // factory methods

  public Column addColumn(String name) {
    return this.getTable().addColumn(name, STRING);
  }

  public Column addColumn(String name, ColumnType columnType) {
    return this.getTable().addColumn(name, columnType);
  }

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

  public Column addRefBack(String name, String toTable, String viaColumn) {
    return this.getTable().addRefBack(name, toTable, null, viaColumn);
  }

  public Column addRefBack(String name, String toTable, String toColumn, String viaColumn) {
    return this.getTable().addRefBack(name, toTable, toColumn, viaColumn);
  }

  public TableMetadata addUnique(String... columnNames) {
    return getTable().addUnique(columnNames);
  }

  public TableMetadata setPrimaryKey(String columnNames) {
    return getTable().setPrimaryKey(columnNames);
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getName()).append(" ");
    if (ColumnType.REF.equals(getColumnType()))
      builder.append("ref(").append(refTable).append(",").append(refColumn).append(")");
    else if (ColumnType.REF_ARRAY.equals(getColumnType()))
      builder.append("ref_array(").append(refTable).append(",").append(refColumn).append(")");
    else if (ColumnType.MREF.equals(getColumnType()))
      builder.append("mref(").append(refTable).append(",").append(refColumn).append(")");
    else builder.append(getColumnType().toString().toLowerCase());
    if (Boolean.TRUE.equals(getNullable())) builder.append(" nullable");
    return builder.toString();
  }
}
