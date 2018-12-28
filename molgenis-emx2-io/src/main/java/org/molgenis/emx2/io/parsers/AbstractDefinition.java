package org.molgenis.emx2.io.parsers;

import java.util.List;

public interface AbstractDefinition<T extends AbstractDefinition> {

    boolean hasParameter();

    String getParameterValue();

    List<String> getParameterList();

    AbstractDefinition setParameterValue(String parameterValue);
}
