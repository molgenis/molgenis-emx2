package org.molgenis.emx2.io.emx1;

import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;

public class Emx1Attribute {
  public static final String ID_ATTRIBUTE = "idAttribute";
  public static final String NILLABLE_NAME = "nillable";
  public static final String LABEL_ATTRIBUTE = "labelAttribute";
  public static final String READONLY_NAME = "readonly";
  private String entity;
  private String name;
  private String label;
  private String dataType;
  private String description;
  private boolean nillable = false;
  private boolean idAttribute = false;
  private boolean aggregateable = false;
  private boolean labelAttribute = false;
  private boolean readonly = false;
  private String validation;
  private String visible;
  private String defaultValue;
  private String partOfAttribute;
  private String refEntity;
  private String expression;
  private String refBack;
  private Integer rangeMin;
  private Integer rangeMax;

  public Emx1Attribute(Column c) {
    this.setName(c.getName());
    this.setEntity(c.getTableName());
    this.setDataType(getEmx1Type(c));
    this.setIdAttribute(c.getKey() == 1);
    this.setRefEntity(c.getRefTableName());
    this.setRefBack(c.getRefBack());
    this.setNillable(!c.isRequired());
  }

  public Emx1Attribute(Row row) {
    this.entity = get(row, "entity");
    this.name = get(row, "name");
    this.label = get(row, "label");
    this.dataType = get(row, "dataType");
    this.description = get(row, "description");
    if (row.getBoolean(NILLABLE_NAME) != null) this.nillable = row.getBoolean(NILLABLE_NAME);
    this.idAttribute =
        get(row, ID_ATTRIBUTE) != null
            && ("AUTO".equalsIgnoreCase(row.getString(ID_ATTRIBUTE))
                || row.getBoolean(ID_ATTRIBUTE) != null
                    && row.getBoolean(ID_ATTRIBUTE).equals(true));
    if (row.getBoolean("aggregatable") != null) this.aggregateable = row.getBoolean("aggregatable");
    if (row.getBoolean(LABEL_ATTRIBUTE) != null)
      this.labelAttribute = row.getBoolean(LABEL_ATTRIBUTE);
    if (row.getBoolean(READONLY_NAME) != null) this.readonly = row.getBoolean(READONLY_NAME);
    this.validation = get(row, "validation");
    this.visible = get(row, "visible");
    this.defaultValue = get(row, "defaultValue");
    this.partOfAttribute = get(row, "partOfAttribute");
    this.refEntity = get(row, "refEntity");
    this.refBack = get(row, "refBack");
    this.expression = get(row, "expression");
    this.rangeMax = row.getInteger("rangeMax");
    this.rangeMin = row.getInteger("rangeMi");
  }

  private String get(Row row, String name) {
    try {
      String value = row.getString(name);
      if (value == null || "".equals(value.trim())) return null;
      return value.trim();
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  public String getEntity() {
    return entity;
  }

  public String getName() {
    return name;
  }

  public String getLabel() {
    return label;
  }

  public String getDataType() {
    return dataType;
  }

  public String getDescription() {
    return description;
  }

  public boolean getNillable() {
    return nillable;
  }

  public boolean getIdAttribute() {
    return idAttribute;
  }

  public boolean getAggregateable() {
    return aggregateable;
  }

  public boolean getLabelAttribute() {
    return labelAttribute;
  }

  public boolean getReadonly() {
    return readonly;
  }

  public String getValidation() {
    return validation;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public String getPartOfAttribute() {
    return partOfAttribute;
  }

  public String getRefEntity() {
    return refEntity;
  }

  public String getExpression() {
    return expression;
  }

  public Integer getRangeMin() {
    return rangeMin;
  }

  public Integer getRangeMax() {
    return rangeMax;
  }

  public String getVisible() {
    return visible;
  }

  public String getRefBack() {
    return refBack;
  }

  public void setEntity(String entity) {
    this.entity = entity;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setNillable(boolean nillable) {
    this.nillable = nillable;
  }

  public void setIdAttribute(boolean idAttribute) {
    this.idAttribute = idAttribute;
  }

  public void setAggregateable(boolean aggregateable) {
    this.aggregateable = aggregateable;
  }

  public void setLabelAttribute(boolean labelAttribute) {
    this.labelAttribute = labelAttribute;
  }

  public void setReadonly(boolean readonly) {
    this.readonly = readonly;
  }

  public void setValidation(String validation) {
    this.validation = validation;
  }

  public void setVisible(String visible) {
    this.visible = visible;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public void setPartOfAttribute(String partOfAttribute) {
    this.partOfAttribute = partOfAttribute;
  }

  public void setRefEntity(String refEntity) {
    this.refEntity = refEntity;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public void setRefBack(String refBack) {
    this.refBack = refBack;
  }

  public void setRangeMin(Integer rangeMin) {
    this.rangeMin = rangeMin;
  }

  public void setRangeMax(Integer rangeMax) {
    this.rangeMax = rangeMax;
  }

  public Row toRow() {
    Row r = new Row();
    r.set("entity", entity);
    r.set("name", name);
    r.set("label", label); // not supported by design
    r.set("dataType", dataType);
    r.set("refEntity", refEntity);
    r.set("refBack", refBack);
    r.set(ID_ATTRIBUTE, idAttribute); // different by design
    r.set(NILLABLE_NAME, nillable);
    r.set(READONLY_NAME, readonly);
    r.set("partOfAttribute", partOfAttribute); // not supported by design
    r.set(LABEL_ATTRIBUTE, labelAttribute); // not supported by design
    r.set("defaultValue", defaultValue);
    r.set("expression", expression);
    r.set("validation", validation);
    r.set("visible", visible);
    r.set("rangeMin", rangeMin); // not supported by design
    r.set("rangeMax", rangeMax); // not supported by design
    return r;
  }

  private static String getEmx1Type(Column c) {
    switch (c.getColumnType()) {
      case BOOL:
        return "bool";
      case FILE:
        return "file";
      case STRING, UUID:
        return "varchar";
      case TEXT, JSONB:
        return "text";
      case INT:
        return "int";
      case DECIMAL:
        return "decimal";
      case DATE:
        return "date";
      case DATETIME:
        return "datetime";
      case REF:
        return "xref";
      case REF_ARRAY:
        return "mref";
      case REFBACK:
        return "refBack unsupported in emx1";
      case BOOL_ARRAY,
          UUID_ARRAY,
          STRING_ARRAY,
          TEXT_ARRAY,
          INT_ARRAY,
          DATE_ARRAY,
          DATETIME_ARRAY,
          JSONB_ARRAY,
          DECIMAL_ARRAY:
        return "array types unsupported in emx1: " + c.getColumnType();
      default:
        return "unknown type in emx1: " + c.getColumnType();
    }
  }
}
