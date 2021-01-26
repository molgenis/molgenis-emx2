package org.molgenis.emx2.json;

import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.TableMetadata;

public class Column {
  private String table;
  private String name;
  private Integer key = 0;
  private Boolean nullable = false;
  private String refSchema = null;
  private String refTable = null;
  private String[] refFrom = new String[0];
  private String[] refTo = new String[0];
  private String refJsTemplate;

  private Boolean cascadeDelete = false;
  private String mappedBy = null;
  private String validation = null;
  private String form = null;
  private String visible = null;
  private String description = null;
  private ColumnType columnType = ColumnType.STRING;
  private String jsonldType = null;

  private boolean inherited = false;

  public Column() {}

  public Column(org.molgenis.emx2.Column column, TableMetadata table) {
    this.table = column.getTableName();
    this.name = column.getName();
    this.key = column.getKey();
    this.columnType = column.getColumnType();
    this.refSchema =
        column.getRefSchema().equals(column.getSchemaName()) ? null : column.getRefSchema();
    this.refTable = column.getRefTableName();
    this.refFrom = column.getRefFrom();
    this.refTo = column.getRefTo();
    this.refJsTemplate = column.getRefJsTemplate();
    this.cascadeDelete = column.isCascadeDelete();
    this.mappedBy = column.getMappedBy();
    this.validation = column.getValidationScript();
    this.nullable = column.isNullable();
    this.description = column.getDescription();
    this.jsonldType = column.getJsonldType();
    this.visible = column.getVisible();
    this.form = column.getForm();

    // calculated field
    if (table.getInherit() != null)
      this.inherited = table.getInheritedTable().getColumnNames().contains(column.getName());
  }

  public org.molgenis.emx2.Column getColumnMetadata(TableMetadata tm) {
    org.molgenis.emx2.Column c = new org.molgenis.emx2.Column(tm, name);
    c.setType(columnType);
    c.setNullable(nullable);
    c.setRefSchema(refSchema);
    c.setRefTable(refTable);
    c.setRefFrom(refFrom);
    c.setRefTo(refTo);
    c.setRefJsTemplate(refJsTemplate);
    c.setKey(key);
    c.setCascadeDelete(cascadeDelete);
    c.setMappedBy(mappedBy);
    c.setValidationScript(validation);
    c.setDescription(description);
    c.setJsonldType(jsonldType);
    c.setVisible(visible);
    c.setForm(form);
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

  public String getJsonldType() {
    return jsonldType;
  }

  public void setJsonldType(String jsonldType) {
    this.jsonldType = jsonldType;
  }

  public String[] getRefFrom() {
    return refFrom;
  }

  public void setRefFrom(String[] refFrom) {
    this.refFrom = refFrom;
  }

  public String[] getRefTo() {
    return refTo;
  }

  public void setRefTo(String[] refTo) {
    this.refTo = refTo;
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

  public String getForm() {
    return form;
  }

  public void setForm(String form) {
    this.form = form;
  }

  public String getVisible() {
    return visible;
  }

  public void setVisible(String visible) {
    this.visible = visible;
  }
}
