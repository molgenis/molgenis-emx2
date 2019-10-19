package org.molgenis.emx2.legacy.format;

import org.molgenis.emx2.Row;

public class Attribute {
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
  private Integer rangeMin;
  private Integer rangeMax;

  public Attribute(Row row) {
    this.entity = get(row, "entity");
    this.name = get(row, "name");
    this.label = get(row, "label");
    this.dataType = get(row, "dataType");
    this.description = get(row, "description");
    this.nillable = row.getBoolean("nillable");
    this.idAttribute = "AUTO".equals(row.getString("idAttribute"));
    this.aggregateable = row.getBoolean("aggregatable");
    this.labelAttribute = row.getBoolean("labelAttribute");
    this.readonly = row.getBoolean("readonly");
    this.validationExpression = get(row, "validationExpression");
    this.visibleExpression = get(row, "visibleExpression");
    this.defaultValue = get(row, "defaultValue");
    this.partOfAttribute = get(row, "partOfAttribute");
    this.refEntity = get(row, "refEntity");
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

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("AttributesFileRow(");
    builder.append("entity='").append(entity).append("'");
    builder.append(" name='").append(name).append("'");
    writeType(builder);
    writeLabel(builder);
    writeNillable(builder);
    writeIdAttribute(builder);
    writeAggregateable(builder);
    writeLabelAttribute(builder);
    writeReadonly(builder);
    writeValidationExpression(builder);
    writeDefaultValue(builder);
    writePartOfAttribute(builder);
    writeRefEntity(builder);
    writeExpression(builder);
    writeRangeMin(builder);
    writeMax(builder);
    writeVisbleExpression(builder);
    writeDescription(builder);
    builder.append(")");
    return builder.toString();
  }

  public void writeDescription(StringBuilder builder) {
    if (description != null) {
      builder.append(" description='").append(description).append("'");
    }
  }

  public void writeVisbleExpression(StringBuilder builder) {
    if (visibleExpression != null)
      builder.append(" visibleExpression='").append(visibleExpression).append("'");
  }

  public void writeMax(StringBuilder builder) {
    if (rangeMax != null) builder.append(" rangeMax=").append(rangeMax);
  }

  public void writeRangeMin(StringBuilder builder) {
    if (rangeMin != null) builder.append(" rangeMin=").append(rangeMin);
  }

  public void writeExpression(StringBuilder builder) {
    if (expression != null) builder.append(" expression='").append(expression).append("'");
  }

  public void writeRefEntity(StringBuilder builder) {
    if (refEntity != null) builder.append(" refEntity='").append(refEntity).append("'");
  }

  public void writePartOfAttribute(StringBuilder builder) {
    if (partOfAttribute != null)
      builder.append(" partOfAttribute='").append(partOfAttribute).append("'");
  }

  public void writeDefaultValue(StringBuilder builder) {
    if (defaultValue != null) builder.append(" defaultValue='").append(defaultValue).append("'");
  }

  public void writeValidationExpression(StringBuilder builder) {
    if (validationExpression != null)
      builder.append(" validationExpression='").append(validationExpression).append("'");
  }

  public void writeReadonly(StringBuilder builder) {
    if (Boolean.TRUE.equals(readonly)) builder.append(" readonly");
  }

  public void writeLabelAttribute(StringBuilder builder) {
    if (Boolean.TRUE.equals(labelAttribute)) builder.append(" labelAttribute");
  }

  public void writeAggregateable(StringBuilder builder) {
    if (Boolean.TRUE.equals(aggregateable)) builder.append(" aggregateable");
  }

  public void writeIdAttribute(StringBuilder builder) {
    if (Boolean.TRUE.equals(idAttribute)) builder.append(" idAttribute");
  }

  public void writeNillable(StringBuilder builder) {
    if (Boolean.TRUE.equals(nillable)) builder.append(" nillable");
  }

  public void writeLabel(StringBuilder builder) {
    if (label != null) builder.append(" label='").append(label).append("'");
  }

  public void writeType(StringBuilder builder) {
    if (dataType != null) builder.append(" type='").append(dataType).append("'");
  }
}
