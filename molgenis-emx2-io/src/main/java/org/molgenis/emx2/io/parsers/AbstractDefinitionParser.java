package org.molgenis.emx2.io.parsers;

import org.molgenis.emx2.io.MolgenisReaderMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractDefinitionParser<T extends AbstractDefinition> {
    static String patternString = "([a-z]+)(\\((.*?(?<!\\\\))\\))?";
    static Pattern pattern = Pattern.compile(patternString);

    public List<T> parse(Integer line, List<MolgenisReaderMessage> messages, String definition) {
        List<T> tags = new ArrayList<>();

        Matcher matcher = pattern.matcher(definition);
        String messagePrefix = "tag '"+matcher.group(1)+"' ";
        while(matcher.find()) {
            try {
                T tag = getTag(matcher.group(1).toUpperCase());

                String parameter = matcher.group(3);
                if(tag.hasParameter()) {
                    if(parameter== null) {
                        messages.add(new MolgenisReaderMessage(line, messagePrefix+"' expects parameter"));
                    } else {
                        tag.setParameterValue(parameter);
                    }
                } else {
                    if(parameter != null) {
                        messages.add(new MolgenisReaderMessage(line, messagePrefix+" does not expect parameter"));
                    }
                }

                tags.add(tag);
            } catch(IllegalArgumentException e) {
                messages.add(new MolgenisReaderMessage(line, messagePrefix +"' is unknown"));
            }
        }
        return tags;
    }

    public abstract T getTag(String upperCaseTagName);
}
