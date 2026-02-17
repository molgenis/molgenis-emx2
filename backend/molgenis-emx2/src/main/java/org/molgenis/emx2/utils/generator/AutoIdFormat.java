package org.molgenis.emx2.utils.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public record AutoIdFormat(Format format, int length) {

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

  public static AutoIdFormat fromComputedString(String computedString) {
    Format format = Format.MIXED;
    int length = 12;

    Matcher matcher = JS_TOKEN_PATTERN.matcher(computedString);

    if (!matcher.find()) {
      return new AutoIdFormat(format, length);
    }

    String args = matcher.group("args");
    if (args == null || args.isBlank()) {
      return new AutoIdFormat(format, length);
    }

    Map<String, Object> argMap = parseArgs(args);

    for (Map.Entry<String, Object> entry : argMap.entrySet()) {
      switch (entry.getKey()) {
        case "format":
          format = Format.valueOf(entry.getValue().toString().toUpperCase());
          break;
        case "length":
          length = Integer.parseInt(entry.getValue().toString());
          if (length <= 0) {
            throw new IllegalArgumentException("length for auto id cannot be negative");
          }
          break;
        default:
          throw new IllegalArgumentException("Unsupported format: " + entry.getKey());
      }
    }

    return new AutoIdFormat(format, length);
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

  public long getMaxValue() {
    long result = 1;
    int characterLength = format.characters.length();
    long limit = Long.MAX_VALUE / (characterLength);

    for (int i = 0; i < length; i++) {
      if (result > limit) {
        return Long.MAX_VALUE;
      }

      result *= characterLength;
    }

    return result - 1;
  }

  public String mapToFormat(long value) {
    StringBuilder builder = new StringBuilder();

    long n = value;
    int base = format.characters.length();

    while (n > 0) {
      long remainder = n % base;
      builder.insert(0, format.getCharacters().charAt((int) remainder));
      n = n / base;
    }

    return StringUtils.leftPad(builder.toString(), length, format.characters.charAt(0));
  }
}
