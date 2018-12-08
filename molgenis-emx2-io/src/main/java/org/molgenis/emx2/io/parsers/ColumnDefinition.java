package org.molgenis.emx2.io.parsers;

import org.molgenis.emx2.EmxType;

public enum ColumnDefinition implements AbstractDefinition {
    STRING(false), INT(false), LONG(false),SELECT(true),RADIO(true),BOOL(false),DECIMAL(false),TEXT(false), DATE(false),
    DATETIME(false), MSELECT(true),CHECKBOX(true),NILLABLE(false),DEFAULT(true),UNIQUE(false),READONLY(false),VISIBLE(true), VALIDATION(true),
    UUID(false),HYPERLINK(false),EMAIL(false), HTML(false), FILE(false), ENUM(true);

     //TODO: FILE, CASCADE, OM, CHECK
    private boolean hasParameter;
    private String parameterValue;

    ColumnDefinition(Boolean hasParameter) {
        this.hasParameter = hasParameter;
    }

    @Override
    public boolean hasParameter() {
        return hasParameter;
    }

    @Override
    public String getParameterValue() {
        return parameterValue;
    }

    @Override
    public ColumnDefinition setParameterValue(String parameterValue) {
        this.parameterValue = parameterValue;
        return this;
    }

    @Override
    public String toString() {
        if(hasParameter) {
            return this.name().toLowerCase() + "(" + parameterValue + ")";
        }
        else {
            return this.name().toLowerCase();
        }
    }

    public static ColumnDefinition valueOf(EmxType type) {
        return valueOf(type.toString());
    }
}
