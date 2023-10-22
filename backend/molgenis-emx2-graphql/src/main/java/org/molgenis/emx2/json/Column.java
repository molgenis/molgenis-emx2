package org.molgenis.emx2.json;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.TableMetadata;

public class Column {
  private String table;
  private String id;
  private String name;
  private List<LanguageValue> labels = new ArrayList<>();
  private boolean drop = false; // needed in case of migrations
  private String oldName;
  private Integer key = 0;
  private Boolean required = false;
  private Boolean readonly = false;
  private String defaultValue;
  private String refSchema = null;
  private String refTable = null;
  private String refLink = null;
  private String refBack = null;
  private String refLabel;
  private String refLabelDefault;
  private Integer position = null;

  // private Boolean cascadeDelete = false;
  private String validation = null;
  private String visible = null;
  private String computed = null;
  private List<LanguageValue> descriptions = new ArrayList<>();
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
    this.id = column.getIdentifier();
    this.name = column.getName();
    this.labels =
        column.getLabels().entrySet().stream()
            .map(entry -> new LanguageValue(entry.getKey(), entry.getValue()))
            .toList();
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
    this.refLabel = column.getRefLabel();
    this.refLabelDefault = column.getRefLabelDefault();
    // this.cascadeDelete = column.isCascadeDelete();
    this.refBack = column.getRefBack();
    this.validation = column.getValidation();
    this.required = column.isRequired();
    this.readonly = column.isReadonly();
    this.defaultValue = column.getDefaultValue();
    this.descriptions =
        column.getDescriptions().entrySet().stream()
            .map(entry -> new LanguageValue(entry.getKey(), entry.getValue()))
            .toList();
    this.semantics = column.getSemantics();
    this.visible = column.getVisible();
    this.computed = column.getComputed();

    // calculated field
    if (table.getInherit() != null)
      this.inherited = table.getInheritedTable().getColumnNames().contains(column.getName());
  }

  public org.molgenis.emx2.Column getColumnMetadata(TableMetadata tm) {
    org.molgenis.emx2.Column c = new org.molgenis.emx2.Column(tm, name);
    c.setOldName(oldName);
    c.setLabels(
        labels.stream()
            .filter(d -> d.value() != null)
            .collect(Collectors.toMap(LanguageValue::locale, LanguageValue::value)));
    c.setType(columnType);
    if (drop) c.drop();
    c.setRequired(required);
    c.setDefaultValue(defaultValue);
    c.setRefSchema(refSchema);
    c.setRefTable(refTable);
    c.setRefLink(refLink);
    c.setRefLabel(refLabel);
    c.setKey(key);
    c.setPosition(position);
    // c.setCascadeDelete(cascadeDelete);
    c.setRefBack(refBack);
    c.setValidation(validation);
    c.setDescriptions(
        descriptions.stream()
            .filter(d -> d.value() != null)
            .collect(Collectors.toMap(LanguageValue::locale, LanguageValue::value)));
    c.setSemantics(semantics);
    c.setVisible(visible);
    c.setComputed(computed);
    c.setReadonly(readonly);

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

  public List<LanguageValue> getDescriptions() {
    return descriptions;
  }

  public void setDescriptions(List<LanguageValue> descriptions) {
    this.descriptions = descriptions;
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

  public String getRefLabelDefault() {
    return refLabelDefault;
  }

  public void setRefLabelDefault(String refLabelDefault) {
    this.refLabelDefault = refLabelDefault;
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

  public Boolean getReadonly() {
    return readonly;
  }

  public void setReadonly(Boolean readonly) {
    this.readonly = readonly;
  }

  public void setComputed(String computed) {
    this.computed = computed;
  }

  public String getComputed() {
    return computed;
  }

  public List<LanguageValue> getLabels() {
    return labels;
  }

  public void setLabels(List<LanguageValue> labels) {
    this.labels = labels;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }
}
