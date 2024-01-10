package org.molgenis.emx2.json;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.TableMetadata;

public class Column {
  private String table; // name
  private String id;
  private String name;
  private String label;
  private String description;
  private List<LanguageValue> labels = new ArrayList<>();
  private boolean drop = false; // needed in case of migrations
  private String oldName;
  private Integer key = 0;
  private String required = null;
  private Boolean readonly = false;
  private String defaultValue;
  private String refSchemaId = null;
  private String refSchemaName = null;
  private String refTableId = null;
  private String refTableName = null;
  private String refLinkId = null;
  private String refLinkName = null;
  private String refBackId = null;
  private String refBackName = null;
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
    this.label = column.getLabel();
    this.description = column.getDescriptions().get("en");
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
    if (column.isReference()) {
      this.refSchemaId =
          column.getRefSchemaName().equals(column.getSchemaName())
              ? null
              : column.getRefSchemaName();
      this.refSchemaName = column.getRefSchemaName();
      this.refTableId = column.getRefTable().getIdentifier();
      this.refTableName = column.getRefTableName();
      if (column.getRefLinkColumn() != null) {
        this.refLinkId = column.getRefLinkColumn().getIdentifier();
        this.refLinkName = column.getRefLink();
      }
      if (column.getRefBack() != null) {
        this.refBackId = column.getRefBackColumn().getIdentifier();
        this.refBackName = column.getRefBack();
      }
    }
    this.refLabel = column.getRefLabel();
    this.refLabelDefault = column.getRefLabelDefault();
    // this.cascadeDelete = column.isCascadeDelete();
    this.validation = column.getValidation();
    this.setRequired(column.getRequired());
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
    if (table.getInheritName() != null)
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
    c.setRefSchemaName(refSchemaName);
    c.setRefTable(refTableName);
    c.setRefLink(refLinkName);
    c.setRefLabel(refLabel);
    c.setKey(key);
    c.setPosition(position);
    // c.setCascadeDelete(cascadeDelete);
    c.setRefBack(refBackName);
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

  public boolean isRequired() {
    return required != null && required.equals("true");
  }

  public void setRequired(Boolean required) {
    this.required = required.toString();
  }

  public void setRequired(String required) {
    this.required = required;
  }

  public String getRequired() {
    return this.required;
  }

  public String getRefTableId() {
    return refTableId;
  }

  public void setRefTableId(String refTableId) {
    this.refTableId = refTableId;
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

  public String getRefBackId() {
    return refBackId;
  }

  public void setRefBackId(String refBackId) {
    this.refBackId = refBackId;
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

  public String getRefLinkId() {
    return refLinkId;
  }

  public void setRefLinkId(String refLinkId) {
    this.refLinkId = refLinkId;
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

  public String getRefSchemaId() {
    return refSchemaId;
  }

  public void setRefSchemaId(String refSchemaId) {
    this.refSchemaId = refSchemaId;
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

  public String getRefSchemaName() {
    return refSchemaName;
  }

  public void setRefSchemaName(String refSchemaName) {
    this.refSchemaName = refSchemaName;
  }

  public String getRefTableName() {
    return refTableName;
  }

  public void setRefTableName(String refTableName) {
    this.refTableName = refTableName;
  }

  public String getRefLinkName() {
    return refLinkName;
  }

  public void setRefLinkName(String refLinkName) {
    this.refLinkName = refLinkName;
  }

  public String getRefBackName() {
    return refBackName;
  }

  public void setRefBackName(String refBackName) {
    this.refBackName = refBackName;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
