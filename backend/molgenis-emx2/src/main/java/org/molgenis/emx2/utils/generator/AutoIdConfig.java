package org.molgenis.emx2.utils.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record AutoIdConfig(Format format, int length) {

  private static final Pattern JS_TOKEN_PATTERN =
      Pattern.compile("\\$\\{mg_autoid(\\((?<args>.*)\\))?}");

  public enum Format {
    LETTERS("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"),
    NUMBERS("0123456789"),
    MIXED(LETTERS.getCharacters() + NUMBERS.getCharacters());

    private final String characters;

    Format(String characters) {
      this.characters = characters;
    }

    public String getCharacters() {
      return characters;
    }
  }

  public static AutoIdConfig fromComputedString(String computedString) {
    Format format = Format.MIXED;
    int length = 12;

    Matcher matcher = JS_TOKEN_PATTERN.matcher(computedString);

    if (!matcher.find()) {
      return new AutoIdConfig(format, length);
    }

    String args = matcher.group("args");
    if (args == null) {
      return new AutoIdConfig(format, length);
    }

    Map<String, Object> argMap = parseArgs(args);

    for (Map.Entry<String, Object> stringObjectEntry : argMap.entrySet()) {
      switch (stringObjectEntry.getKey()) {
        case "format":
          format = Format.valueOf(stringObjectEntry.getValue().toString().toUpperCase());
          break;
        case "length":
          length = Integer.parseInt(stringObjectEntry.getValue().toString());
          if (length <= 0) {
            throw new IllegalArgumentException("length for auto id cannot be negative");
          }
          break;
        default:
          throw new IllegalArgumentException("Unsupported format: " + stringObjectEntry.getKey());
      }
    }

    return new AutoIdConfig(format, length);
  }

  private static Map<String, Object> parseArgs(String args) {
    Map<String, Object> argMap = new HashMap<>();

    for (String arg : args.split(",")) {
      String[] pair = arg.split("=");
      if (pair.length != 2) {
        throw new IllegalArgumentException("Invalid argument format: " + arg);
      }

      String key = pair[0].trim().toLowerCase();
      if (argMap.containsKey(key)) {
        throw new IllegalArgumentException("Duplicate key: " + key);
      }

      argMap.put(key, pair[1].trim());
    }

    return argMap;
  }
}
