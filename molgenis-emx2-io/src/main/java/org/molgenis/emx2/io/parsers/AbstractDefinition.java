package org.molgenis.emx2.io.parsers;

public interface AbstractDefinition<T extends AbstractDefinition> {
    boolean hasParameter();

    String getParameterValue();

    T setParameterValue(String parameterValue);
}
