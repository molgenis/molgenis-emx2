package org.molgenis.emx2.io.parsers;

public enum TableDefinition implements AbstractDefinition {
    ABSTRACT(false), LABEL(true), UNIQUE(true), EXTENDS(true);

    //TODO: FILE, CASCADE, OM, CHECK
    private boolean hasParameter;
    private String parameterValue;

    TableDefinition(Boolean hasParameter) {
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
    public TableDefinition setParameterValue(String parameterValue) {
        this.parameterValue = parameterValue;
        return this;
    }

    @Override
    public String toString() {
        if(hasParameter) {
            return this.name() + "(" + parameterValue + ")";
        }
        else {
            return this.name();
        }
    }
}
