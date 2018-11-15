package org.molgenis.emx2.io.parsers;

public class TableDefinitionParser extends AbstractDefinitionParser<TableDefinition> {

    @Override
    public TableDefinition getTag(String upperCaseTagName) {
        return TableDefinition.valueOf(upperCaseTagName);
    }
}
