package org.molgenis.emx2;

import static org.molgenis.emx2.ColumnType.STRING;

public class Column {
  private TableMetadata table;
  private String columnName;
  private ColumnType columnType = STRING;

  // options
  private boolean nullable = false;
  private boolean readonly = false;
  private String description;
  private String defaultValue;
  private boolean indexed = false;

  // relationships
  private String refTable;
  private String refColumn;
  private String mappedBy;
  private boolean pkey;

  public Column(Column column) {
    copy(column);
  }

  public Column(TableMetadata table, Column column) {
    this.table = table;
    // todo validate
    copy(column);
  }

  private void copy(Column column) {
    columnName = column.getName();
    columnType = column.getColumnType();
    nullable = column.isNullable();
    readonly = column.isReadonly();
    description = column.getDescription();
    defaultValue = column.getDefaultValue();
    indexed = column.isIndexed();
    refTable = column.getRefTableName();
    refColumn = column.getRefColumnName();
    pkey = column.isPrimaryKey();
    mappedBy = column.getMappedBy();
  }

  public static Column column(String name) {
    return new Column(name);
  }

  public Column(String columnName) {
    this.columnName = columnName;
  }

  public Column(TableMetadata table, String columnName) {
    this.table = table;
    this.columnName = columnName;
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
    if (this.refColumn == null && getRefTable() != null) {
      return getRefTable().getPrimaryKey();
    }
    return this.refColumn;
  }

  public Column getRefColumn() {
    Column result = null;
    if (getRefColumnName() != null) {
      result = getRefTable().getColumn(getRefColumnName());
    }
    if (result == null)
      throw new MolgenisException(
          "Internal error",
          "Column.getRefColumn failed for column '"
              + getName()
              + "' because refColumn '"
              + getRefColumnName()
              + "' could not be found in table '"
              + getRefTableName()
              + "'");
    return result;
  }

  private SchemaMetadata getSchema() {
    if (getTable() != null) {
      return getTable().getSchema();
    }
    return null;
  }

  public String getRefTableName() {
    return this.refTable;
  }

  public TableMetadata getRefTable() {
    if (this.refTable != null && getTable() != null) {
      // self relation
      if (this.refTable.equals(getTable().getTableName())) {
        return getTable(); // this table
      }
      // other relation
      if (getSchema() != null) {
        TableMetadata result = getSchema().getTableMetadata(this.refTable);
        if (result == null) {
          throw new MolgenisException(
              "Internal error",
              "Column.getRefTable failed for column '"
                  + getName()
                  + "' because refTable '"
                  + getRefTableName()
                  + "' does not exist in schema '"
                  + getSchema().getName()
                  + "'");
        }
        return result;
      }
    }
    return null;
  }

  public Boolean isUnique() {
    return getTable().isUnique(getName());
  }

  public Column setUnique(boolean unique) {
    if (unique) getTable().addUnique(this.getName());
    else getTable().removeUnique(this.getName());
    return this;
  }

  public Boolean isReadonly() {
    return readonly;
  }

  public Column setReadonly(boolean readonly) {
    this.readonly = readonly;
    return this;
  }

  public Boolean isNullable() {
    return nullable;
  }

  public Column nullable(boolean nillable) {
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

  public Column mappedBy(String columnName) {
    this.mappedBy = columnName;
    return this;
  }

  public Column index(boolean indexed) {
    this.indexed = indexed;
    return this;
  }

  public Boolean isIndexed() {
    return indexed;
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
    if (Boolean.TRUE.equals(isNullable())) builder.append(" nullable");
    return builder.toString();
  }

  public Column type(ColumnType type) {
    if (type == null) {
      throw new MolgenisException("Add column failed", "Type was null for column " + getName());
    }
    this.columnType = type;
    return this;
  }

  public Column refTable(String refTable) {
    this.refTable = refTable;
    return this;
  }

  public Column refColumn(String refColumn) {
    this.refColumn = refColumn;
    return this;
  }

  public boolean isPrimaryKey() {
    return this.pkey;
  }

  public Column pkey(boolean pkey) {
    this.pkey = pkey;
    return this;
  }

  void setTable(TableMetadata table) {
    this.table = table;
  }

  public String getTableName() {
    if (this.table != null) return this.table.getTableName();
    return null;
  }
}
