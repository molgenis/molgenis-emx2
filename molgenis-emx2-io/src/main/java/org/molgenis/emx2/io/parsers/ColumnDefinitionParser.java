package org.molgenis.emx2.io.parsers;

public class ColumnDefinitionParser extends AbstractDefinitionParser<ColumnDefinition> {

    @Override
    public ColumnDefinition getTag(String upperCaseTagName) {
        return ColumnDefinition.valueOf(upperCaseTagName);
    }
}
