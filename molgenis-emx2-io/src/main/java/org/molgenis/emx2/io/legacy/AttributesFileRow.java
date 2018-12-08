package org.molgenis.emx2.io.legacy;

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
        if (dataType != null) builder.append(" type='").append(dataType).append("'");
        if (label != null) builder.append(" label='").append(label).append("'");
        if (nillable) builder.append(" nillable");
        if (idAttribute) builder.append(" idAttribute");
        if (aggregateable) builder.append(" aggregateable");
        if (labelAttribute) builder.append(" labelAttribute");
        if (readonly) builder.append(" readonly");
        if (validationExepression != null)
            builder.append(" validationExpression='").append(validationExepression).append("'");
        if (defaultValue != null) builder.append(" defaultValue='").append(defaultValue).append("'");
        if (partOfAttribute != null) builder.append(" partOfAttribute='").append(partOfAttribute).append("'");
        if (refEntity != null) builder.append(" refEntity='").append(refEntity).append("'");
        if (expression != null) builder.append(" expression='").append(expression).append("'");
        if (rangeMin != null) builder.append(" rangeMin=").append(rangeMin);
        if (rangeMax != null) builder.append(" rangeMax=").append(rangeMax);
        if (visibleExpression != null) builder.append(" visibleExpression='").append(visibleExpression).append("'");
        if (description != null) builder.append(" description='").append(description).append("'");
        builder.append(")");
        return builder.toString();
    }
}
