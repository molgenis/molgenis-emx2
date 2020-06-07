package org.molgenis.emx2.json;

import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.TableMetadata;

public class Column {
  private String table;
  private String name;
  private Boolean unique = false;
  private Boolean pkey = false;
  private Boolean nullable = false;
  private String refTable = null;
  private String refColumn = null;

  private Boolean cascadeDelete = false;
  private String mappedBy = null;
  private String validation = null;
  private String description = null;
  private ColumnType columnType = ColumnType.STRING;

  public Column() {}

  public Column(org.molgenis.emx2.Column column) {
    this.table = column.getTableName();
    this.name = column.getName();
    this.unique = column.isUnique();
    this.columnType = column.getColumnType();
    this.refTable = column.getRefTableName();
    this.refColumn = column.getRefColumnName();
    this.cascadeDelete = column.isCascadeDelete();
    this.mappedBy = column.getMappedBy();
    this.validation = column.getValidation();
    this.nullable = column.isNullable();
    this.description = column.getDescription();
  }

  public org.molgenis.emx2.Column getColumnMetadata(TableMetadata tm) {
    org.molgenis.emx2.Column c = new org.molgenis.emx2.Column(tm, name);
    c.type(columnType);
    if (tm != null) c.setUnique(unique);
    c.nullable(nullable);
    c.refTable(refTable);
    c.refColumn(refColumn);
    c.cascadeDelete(cascadeDelete);
    c.mappedBy(mappedBy);
    c.validation(validation);
    c.setDescription(description);
    return c;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getUnique() {
    return unique;
  }

  public void setUnique(Boolean unique) {
    this.unique = unique;
  }

  public Boolean getPkey() {
    return pkey;
  }

  public void setPkey(Boolean pkey) {
    this.pkey = pkey;
  }

  public Boolean getNullable() {
    return nullable;
  }

  public void setNullable(Boolean nullable) {
    this.nullable = nullable;
  }

  public String getRefTable() {
    return refTable;
  }

  public void setRefTable(String refTable) {
    this.refTable = refTable;
  }

  public String getRefColumn() {
    return refColumn;
  }

  public void setRefColumn(String refColumn) {
    this.refColumn = refColumn;
  }

  public Boolean getCascadeDelete() {
    return cascadeDelete;
  }

  public void setCascadeDelete(Boolean cascadeDelete) {
    this.cascadeDelete = cascadeDelete;
  }

  public ColumnType getColumnType() {
    return columnType;
  }

  public void setColumnType(ColumnType columnType) {
    this.columnType = columnType;
  }

  public String getValidation() {
    return validation;
  }

  public void setValidation(String validation) {
    this.validation = validation;
  }

  public String getMappedBy() {
    return mappedBy;
  }

  public void setMappedBy(String mappedBy) {
    this.mappedBy = mappedBy;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
