package org.molgenis.emx2.io.emx1;

import org.molgenis.emx2.Row;

public class Emx1Attribute {
  private String entity;
  private String name;
  private String label;
  private String dataType;
  private String description;
  private Boolean nillable;
  private Boolean idAttribute;
  private Boolean aggregateable;
  private Boolean labelAttribute;
  private Boolean readonly;
  private String validationExpression;
  private String visibleExpression;
  private String defaultValue;
  private String partOfAttribute;
  private String refEntity;
  private String expression;
  private String mappedBy;
  private Integer rangeMin;
  private Integer rangeMax;

  public Emx1Attribute(Row row) {
    this.entity = get(row, "entity");
    this.name = get(row, "name");
    this.label = get(row, "label");
    this.dataType = get(row, "dataType");
    this.description = get(row, "description");
    this.nillable = row.getBoolean("nillable");
    this.idAttribute =
        get(row, "idAttribute") != null
            && ("AUTO".equalsIgnoreCase(row.getString("idAttribute"))
                || row.getBoolean("idAttribute"));
    this.aggregateable = row.getBoolean("aggregatable");
    this.labelAttribute = row.getBoolean("labelAttribute");
    this.readonly = row.getBoolean("readonly");
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

  public Boolean getNillable() {
    return nillable;
  }

  public Boolean getIdAttribute() {
    return idAttribute;
  }

  public Boolean getAggregateable() {
    return aggregateable;
  }

  public Boolean getLabelAttribute() {
    return labelAttribute;
  }

  public Boolean getReadonly() {
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
}
