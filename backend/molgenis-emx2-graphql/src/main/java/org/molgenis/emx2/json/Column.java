package org.molgenis.emx2.json;

import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.TableMetadata;

public class Column {
  private String table;
  private String name;
  private boolean drop = false; // needed in case of migrations
  private String oldName;
  private Integer key = 0;
  private Boolean nullable = false;
  private String refSchema = null;
  private String refTable = null;
  private String refLink = null;
  private String mappedBy = null;
  private String refJsTemplate;
  private Integer position = null;

  // private Boolean cascadeDelete = false;
  private String validationExpression = null;
  private String visibleExpression = null;
  private String description = null;
  private ColumnType columnType = ColumnType.STRING;
  private String columnFormat = null;
  private String jsonldType = null;

  private boolean inherited = false;

  public Column() {}

  public Column(org.molgenis.emx2.Column column, TableMetadata table) {
    this.table = column.getTableName();
    this.name = column.getName();
    this.oldName = column.getOldName();
    this.drop = column.isDrop();
    this.key = column.getKey();
    this.columnType = column.getColumnType();
    this.columnFormat = column.getColumnFormat();
    this.refSchema =
        column.getRefSchema().equals(column.getSchemaName()) ? null : column.getRefSchema();
    this.refTable = column.getRefTableName();
    this.refLink = column.getRefLink();
    this.refJsTemplate = column.getRefJsTemplate();
    this.position = column.getPosition();
    // this.cascadeDelete = column.isCascadeDelete();
    this.mappedBy = column.getMappedBy();
    this.validationExpression = column.getValidationExpression();
    this.nullable = column.isNullable();
    this.description = column.getDescription();
    this.jsonldType = column.getJsonldType();
    this.visibleExpression = column.getVisibleExpression();
    this.columnFormat = column.getColumnFormat();

    // calculated field
    if (table.getInherit() != null)
      this.inherited = table.getInheritedTable().getColumnNames().contains(column.getName());
  }

  public org.molgenis.emx2.Column getColumnMetadata(TableMetadata tm) {
    org.molgenis.emx2.Column c = new org.molgenis.emx2.Column(tm, name);
    c.setOldName(oldName);
    c.setType(columnType);
    if (drop) c.drop();
    c.setColumnFormat(columnFormat);
    c.setNullable(nullable);
    c.setRefSchema(refSchema);
    c.setRefTable(refTable);
    c.setRefLink(refLink);
    c.setRefJsTemplate(refJsTemplate);
    c.setKey(key);
    c.setPosition(position);
    // c.setCascadeDelete(cascadeDelete);
    c.setMappedBy(mappedBy);
    c.setValidationExpression(validationExpression);
    c.setDescription(description);
    c.setJsonldType(jsonldType);
    c.setVisibleExpression(visibleExpression);
    c.setColumnFormat(columnFormat);
    // ignore inherited
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

  public Integer getKey() {
    return key;
  }

  public void setKey(Integer key) {
    this.key = key;
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

  //  public Boolean getCascadeDelete() {
  //    return cascadeDelete;
  //  }
  //
  //  public void setCascadeDelete(Boolean cascadeDelete) {
  //    this.cascadeDelete = cascadeDelete;
  //  }

  public ColumnType getColumnType() {
    return columnType;
  }

  public void setColumnType(ColumnType columnType) {
    this.columnType = columnType;
  }

  public String getValidationExpression() {
    return validationExpression;
  }

  public void setValidationExpression(String validationExpression) {
    this.validationExpression = validationExpression;
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

  public String getJsonldType() {
    return jsonldType;
  }

  public void setJsonldType(String jsonldType) {
    this.jsonldType = jsonldType;
  }

  public String getRefLink() {
    return refLink;
  }

  public void setRefLink(String refLink) {
    this.refLink = refLink;
  }

  public String getRefJsTemplate() {
    return refJsTemplate;
  }

  public void setRefJsTemplate(String refJsTemplate) {
    this.refJsTemplate = refJsTemplate;
  }

  public boolean isInherited() {
    return inherited;
  }

  public void setInherited(boolean inherited) {
    this.inherited = inherited;
  }

  public String getRefSchema() {
    return refSchema;
  }

  public void setRefSchema(String refSchema) {
    this.refSchema = refSchema;
  }

  public String getColumnFormat() {
    return columnFormat;
  }

  public void setColumnFormat(String columnFormat) {
    this.columnFormat = columnFormat;
  }

  public String getVisibleExpression() {
    return visibleExpression;
  }

  public void setVisibleExpression(String visibleExpression) {
    this.visibleExpression = visibleExpression;
  }

  public String getOldName() {
    return oldName;
  }

  public void setOldName(String oldName) {
    this.oldName = oldName;
  }

  public boolean getDrop() {
    return drop;
  }

  public void setDrop(boolean drop) {
    this.drop = drop;
  }

  public Integer getPosition() {
    return position;
  }

  public void setPosition(Integer position) {
    this.position = position;
  }
}
