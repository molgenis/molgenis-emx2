package org.molgenis.emx2;

import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.STRING;

public class Column {
  private TableMetadata table;
  private String columnName;
  private ColumnType columnType = STRING;

  // relationships
  private String refTable;
  private String refColumn;
  private String mappedBy;

  // options
  private boolean nullable = false;
  private String validationScript = null;
  // todo implement below
  private boolean readonly = false;
  private String description;
  private String defaultValue;
  private boolean indexed = false;
  private boolean cascadeDelete;

  public Column(Column column) {
    copy(column);
  }

  public Column(TableMetadata table, Column column) {
    this.table = table;
    // todo validate
    copy(column);
  }

  /* copy constructor to prevent changes on in progress data */
  private void copy(Column column) {
    columnName = column.columnName;
    columnType = column.columnType;
    nullable = column.nullable;
    readonly = column.readonly;
    description = column.description;
    defaultValue = column.defaultValue;
    indexed = column.indexed;
    refTable = column.refTable;
    refColumn = column.refColumn;
    mappedBy = column.mappedBy;
    validationScript = column.validationScript;
    description = column.description;
    cascadeDelete = column.cascadeDelete;
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
    if (this.refColumn == null
        && getRefTable() != null
        && getRefTable().getPrimaryKey() != null
        && getRefTable().getPrimaryKey().length == 1) {
      return getRefTable().getPrimaryKey()[0];
    }
    return this.refColumn;
  }

  public String getRefColumnNameRaw() {
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
      SchemaMetadata schema = getSchema();
      if (schema != null) {
        TableMetadata result = schema.getTableMetadata(this.refTable);
        if (result == null) {
          throw new MolgenisException(
              "Internal error",
              "Column.getRefTable failed for column '"
                  + getName()
                  + "' because refTable '"
                  + getRefTableName()
                  + "' does not exist in schema '"
                  + schema.getName()
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

  public Boolean isCascadeDelete() {
    return cascadeDelete;
  }

  public Column nullable(boolean nillable) {
    this.nullable = nillable;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Column setDescription(String description) {
    this.description = description;
    return this;
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

  public Column cascadeDelete(boolean cascadeDelete) {
    if (cascadeDelete && !REF.equals(this.columnType)) {
      throw new MolgenisException(
          "Set casecadeDelete=true failed", "Columnn " + getName() + " must be of type REF");
    }
    this.cascadeDelete = cascadeDelete;
    return this;
  }

  public Boolean isIndexed() {
    return indexed;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getName()).append(" ");
    if (REF.equals(getColumnType()))
      builder.append("ref(").append(refTable).append(",").append(refColumn).append(")");
    else if (ColumnType.REF_ARRAY.equals(getColumnType()))
      builder.append("ref_array(").append(refTable).append(",").append(refColumn).append(")");
    //    else if (ColumnType.MREF.equals(getColumnType()))
    //      builder.append("mref(").append(refTable).append(",").append(refColumn).append(")");
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

  void setTable(TableMetadata table) {
    this.table = table;
  }

  public String getTableName() {
    if (this.table != null) return this.table.getTableName();
    return null;
  }

  public String getValidation() {
    return validationScript;
  }

  public Column validation(String validationScript) {
    this.validationScript = validationScript;
    return this;
  }
}
