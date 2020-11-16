package org.molgenis.emx2.io.emx1;

import static org.molgenis.emx2.ColumnType.REF;

import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;

public class Emx1Attribute {
  public static final String ID_ATTRIBUTE = "idAttribute";
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
  private String validationExpression;
  private String visibleExpression;
  private String defaultValue;
  private String partOfAttribute;
  private String refEntity;
  private String expression;
  private String mappedBy;
  private Integer rangeMin;
  private Integer rangeMax;

  public Emx1Attribute(Column c) {
    this.setName(c.getName());
    this.setEntity(c.getTableName());
    this.setDataType(getEmx1Type(c));
    this.setIdAttribute(c.getKey() == 1);
    this.setRefEntity(c.getRefTableName());
    this.setMappedBy(c.getMappedBy());
    this.setNillable(c.isNullable());
  }

  public Emx1Attribute(Row row) {
    this.entity = get(row, "entity");
    this.name = get(row, "name");
    this.label = get(row, "label");
    this.dataType = get(row, "dataType");
    this.description = get(row, "description");
    if (row.getBoolean("nillable") != null) this.nillable = row.getBoolean("nillable");
    this.idAttribute =
        get(row, ID_ATTRIBUTE) != null
            && ("AUTO".equalsIgnoreCase(row.getString(ID_ATTRIBUTE))
                || row.getBoolean(ID_ATTRIBUTE) != null
                    && row.getBoolean(ID_ATTRIBUTE).equals(true));
    if (row.getBoolean("aggregatable") != null) this.aggregateable = row.getBoolean("aggregatable");
    if (row.getBoolean("labelAttribute") != null)
      this.labelAttribute = row.getBoolean("labelAttribute");
    if (row.getBoolean("readonly") != null) this.readonly = row.getBoolean("readonly");
    this.validationExpression = get(row, "validationExpression");
    this.visibleExpression = get(row, "visibleExpression");
    this.defaultValue = get(row, "defaultValue");
    this.partOfAttribute = get(row, "partOfAttribute");
    this.refEntity = get(row, "refEntity");
    this.mappedBy = get(row, "mappedBy");
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

  public String getValidationExpression() {
    return validationExpression;
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

  public String getVisibleExpression() {
    return visibleExpression;
  }

  public String getMappedBy() {
    return mappedBy;
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

  public void setValidationExpression(String validationExpression) {
    this.validationExpression = validationExpression;
  }

  public void setVisibleExpression(String visibleExpression) {
    this.visibleExpression = visibleExpression;
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

  public void setMappedBy(String mappedBy) {
    this.mappedBy = mappedBy;
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
    r.set("mappedBy", mappedBy);
    r.set(ID_ATTRIBUTE, idAttribute); // different by design
    r.set("nillable", nillable);
    r.set("readonly", readonly);
    r.set("partOfAttribute", partOfAttribute); // not supported by design
    r.set("labelAttribute", labelAttribute); // not supported by design
    r.set("defaultValue", defaultValue);
    r.set("expression", expression);
    r.set("validationExpression", validationExpression);
    r.set("visibleExpression", visibleExpression);
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
      case STRING:
      case UUID:
        return "varchar";
      case TEXT:
      case JSONB:
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
      case MREF:
        return "mref";
      case REFBACK:
        if (REF.equals(c.getMappedByColumn().getColumnType())) return "one_to_many";
        else return "refback unsupported in emx1";
      case BOOL_ARRAY:
      case UUID_ARRAY:
      case STRING_ARRAY:
      case TEXT_ARRAY:
      case INT_ARRAY:
      case DATE_ARRAY:
      case DATETIME_ARRAY:
      case JSONB_ARRAY:
      case DECIMAL_ARRAY:
        return "array types unsupported in emx1: " + c.getColumnType();
      default:
        return "unknown type in emx1: " + c.getColumnType();
    }
  }
}
