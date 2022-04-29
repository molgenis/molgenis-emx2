package org.molgenis.emx2.json;

import static org.molgenis.emx2.graphql.GraphqlTableFieldFactory.escape;

import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.TableMetadata;

public class Column {
  private String table;
  private String id;
  private String name;
  private boolean drop = false; // needed in case of migrations
  private String oldName;
  private Integer key = 0;
  private Boolean required = false;
  private String refSchema = null;
  private String refTable = null;
  private String refLink = null;
  private String refBack = null;
  private String refLabel;
  private Integer position = null;

  // private Boolean cascadeDelete = false;
  private String validation = null;
  private String visible = null;
  private String description = null;
  private ColumnType columnType = ColumnType.STRING;
  private String[] semantics = null;

  private boolean inherited = false;

  public Column() {}

  public Column(org.molgenis.emx2.Column column, TableMetadata table) {
    this(column, table, false);
  }

  public Column(org.molgenis.emx2.Column column, TableMetadata table, boolean minimal) {
    if (!minimal) {
      this.table = column.getTableName();
      this.position = column.getPosition();
    }
    this.id = escape(column.getName());
    this.name = column.getName();
    this.oldName = column.getOldName();
    this.drop = column.isDrop();
    this.key = column.getKey();
    if (!minimal || !ColumnType.STRING.equals(column.getColumnType())) {
      this.columnType = column.getColumnType();
    }
    this.refSchema =
        column.getRefSchema().equals(column.getSchemaName()) ? null : column.getRefSchema();
    this.refTable = column.getRefTableName();
    this.refLink = column.getRefLink();
    this.refLabel = escape(column.getRefLabel());
    // this.cascadeDelete = column.isCascadeDelete();
    this.refBack = column.getRefBack();
    this.validation = column.getValidation();
    this.required = column.isRequired();
    this.description = column.getDescription();
    this.semantics = column.getSemantics();
    this.visible = column.getVisible();

    // calculated field
    if (table.getInherit() != null)
      this.inherited = table.getInheritedTable().getColumnNames().contains(column.getName());
  }

  public org.molgenis.emx2.Column getColumnMetadata(TableMetadata tm) {
    org.molgenis.emx2.Column c = new org.molgenis.emx2.Column(tm, name);
    c.setOldName(oldName);
    c.setType(columnType);
    if (drop) c.drop();
    c.setRequired(required);
    c.setRefSchema(refSchema);
    c.setRefTable(refTable);
    c.setRefLink(refLink);
    c.setRefLabel(refLabel);
    c.setKey(key);
    c.setPosition(position);
    // c.setCascadeDelete(cascadeDelete);
    c.setRefBack(refBack);
    c.setValidation(validation);
    c.setDescription(description);
    c.setSemantics(semantics);
    c.setVisible(visible);
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

  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
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

  public String getValidation() {
    return validation;
  }

  public void setValidation(String validation) {
    this.validation = validation;
  }

  public String getRefBack() {
    return refBack;
  }

  public void setRefBack(String refBack) {
    this.refBack = refBack;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String[] getSemantics() {
    return semantics;
  }

  public void setSemantics(String[] semantics) {
    this.semantics = semantics;
  }

  public String getRefLink() {
    return refLink;
  }

  public void setRefLink(String refLink) {
    this.refLink = refLink;
  }

  public String getRefLabel() {
    return refLabel;
  }

  public void setRefLabel(String refLabel) {
    this.refLabel = refLabel;
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

  public String getVisible() {
    return visible;
  }

  public void setVisible(String visible) {
    this.visible = visible;
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

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
