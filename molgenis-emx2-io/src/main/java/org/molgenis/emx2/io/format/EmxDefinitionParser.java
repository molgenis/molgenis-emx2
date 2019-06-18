package org.molgenis.emx2.io.format;

import org.molgenis.emx2.io.MolgenisReaderMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmxDefinitionParser {
  private static final Pattern pattern = Pattern.compile("([a-z]+)(\\((.*?(?<!\\\\))\\))?"); //NOSONAR

  public List<EmxDefinitionTerm> parse(
      Integer line, List<MolgenisReaderMessage> messages, String definition) {
    List<EmxDefinitionTerm> tags = new ArrayList<>();

    Matcher matcher = pattern.matcher(definition);
    while (matcher.find()) {
      try {
        EmxDefinitionTerm tag = getTag(matcher.group(1).toUpperCase());
        String messagePrefix = "tag '" + matcher.group(1) + "' ";

        String parameter = matcher.group(3);
        if (tag.hasParameter()) {
          if (parameter == null) {
            messages.add(new MolgenisReaderMessage(line, messagePrefix + "' expects parameter"));
          } else {
            tag.setParameterValue(parameter);
          }
        } else {
          if (parameter != null) {
            messages.add(
                new MolgenisReaderMessage(line, messagePrefix + " does not expect parameter"));
          }
        }

        tags.add(tag);
      } catch (IllegalArgumentException e) {
        messages.add(new MolgenisReaderMessage(line, "tag '" + matcher.group(1) + "' is unknown"));
      }
    }
    return tags;
  }

  public EmxDefinitionTerm getTag(String upperCaseTagName) {
    return EmxDefinitionTerm.valueOf(upperCaseTagName);
  }
}
