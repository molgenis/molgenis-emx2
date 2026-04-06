package org.molgenis.emx2.io.emx2;

import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateNameNormalizer {

  private static final Logger log = LoggerFactory.getLogger(TemplateNameNormalizer.class);

  private TemplateNameNormalizer() {}

  public static String normalize(String originalName) {
    if (originalName == null) {
      throw new MolgenisException("Template name must not be null");
    }

    String result = originalName.toLowerCase();
    result = result.replaceAll("[^a-z0-9_]", "_");
    result = result.replaceAll("_+", "_");
    result = result.replaceAll("^_+|_+$", "");

    if (result.isEmpty()) {
      throw new MolgenisException(
          "Template name '" + originalName + "' normalized to an empty string");
    }

    if (!Character.isLetter(result.charAt(0))) {
      result = "s_" + result;
    }

    if (!result.equals(originalName)) {
      log.info("normalized template name '{}' -> '{}'", originalName, result);
    }

    return result;
  }

  public static boolean isValidIdentifier(String name) {
    return name != null && name.matches("[a-z][a-z0-9_]*");
  }
}
