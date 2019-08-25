package org.molgenis.emx2.io.legacyformat;

public class AttributesFileRow {

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
  private String validationExepression;
  private String visibleExpression;
  private String defaultValue;
  private String partOfAttribute;
  private String refEntity;
  private String expression;
  private Integer rangeMin;
  private Integer rangeMax;

  public String getEntity() {
    return entity;
  }

  public void setEntity(String entity) {
    this.entity = entity;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Boolean getNillable() {
    return nillable;
  }

  public void setNillable(Boolean nillable) {
    this.nillable = nillable;
  }

  public Boolean getIdAttribute() {
    return idAttribute;
  }

  public void setIdAttribute(Boolean idAttribute) {
    this.idAttribute = idAttribute;
  }

  public Boolean getAggregateable() {
    return aggregateable;
  }

  public void setAggregateable(Boolean aggregateable) {
    this.aggregateable = aggregateable;
  }

  public Boolean getLabelAttribute() {
    return labelAttribute;
  }

  public void setLabelAttribute(Boolean labelAttribute) {
    this.labelAttribute = labelAttribute;
  }

  public Boolean getReadonly() {
    return readonly;
  }

  public void setReadonly(Boolean readonly) {
    this.readonly = readonly;
  }

  public String getValidationExepression() {
    return validationExepression;
  }

  public void setValidationExepression(String validationExepression) {
    this.validationExepression = validationExepression;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public String getPartOfAttribute() {
    return partOfAttribute;
  }

  public void setPartOfAttribute(String partOfAttribute) {
    this.partOfAttribute = partOfAttribute;
  }

  public String getRefEntity() {
    return refEntity;
  }

  public void setRefEntity(String refEntity) {
    this.refEntity = refEntity;
  }

  public String getExpression() {
    return expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public Integer getRangeMin() {
    return rangeMin;
  }

  public void setRangeMin(Integer rangeMin) {
    this.rangeMin = rangeMin;
  }

  public Integer getRangeMax() {
    return rangeMax;
  }

  public void setRangeMax(Integer rangeMax) {
    this.rangeMax = rangeMax;
  }

  public String getVisibleExpression() {
    return visibleExpression;
  }

  public void setVisibleExpression(String visibleExpression) {
    this.visibleExpression = visibleExpression;
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
    if (validationExepression != null)
      builder.append(" validationExpression='").append(validationExepression).append("'");
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
