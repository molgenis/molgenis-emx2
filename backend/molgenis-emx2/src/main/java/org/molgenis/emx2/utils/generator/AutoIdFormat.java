package org.molgenis.emx2.utils.generator;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.molgenis.emx2.MolgenisException;

public record AutoIdFormat(Format format, int length, String prefix, String suffix) {

  public AutoIdFormat(Format format, int length) {
    this(format, length, "", "");
  }

  private static final Pattern JS_TOKEN_PATTERN =
      Pattern.compile("\\$\\{mg_autoid\\((?<args>[^)]*)\\)}");

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

  public static Optional<AutoIdFormat> fromComputedString(String computedString) {
    Format format = Format.MIXED;
    int length = 12;

    String[] split = computedString.split(JS_TOKEN_PATTERN.pattern());
    String prefix = (split.length >= 1) ? split[0] : "";
    String suffix = (split.length == 2) ? split[1] : "";

    List<MatchResult> results = JS_TOKEN_PATTERN.matcher(computedString).results().toList();

    if (results.isEmpty()) {
      return Optional.empty();
    } else if (results.size() > 1) {
      throw new MolgenisException(
          "Invalid computed value provided, only one autoid instance is allowed");
    }

    String args = results.getFirst().group("args");
    if (args == null || args.isBlank()) {
      return Optional.of(new AutoIdFormat(format, length, prefix, suffix));
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
            throw new MolgenisException("length for auto id cannot be negative");
          }
          break;
        default:
          throw new MolgenisException("Unsupported format: " + entry.getKey());
      }
    }

    return Optional.of(new AutoIdFormat(format, length, prefix, suffix));
  }

  private static Map<String, Object> parseArgs(String args) {
    Map<String, Object> argMap = new HashMap<>();

    for (String arg : args.split(",")) {
      String[] pair = arg.split("=");
      if (pair.length != 2) {
        throw new MolgenisException("Invalid argument format: " + arg);
      }

      String key = pair[0].trim().toLowerCase();
      if (argMap.containsKey(key)) {
        throw new MolgenisException("Duplicate key: " + key);
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

    String generated = StringUtils.leftPad(builder.toString(), length, format.characters.charAt(0));
    return prefix + generated + suffix;
  }

  public long getValue(String input) {
    if (!valueCompliesToFormat(input)) {
      throw new MolgenisException("Given value does not comply with expected format");
    }

    String cleaned = input.replaceFirst(prefix, "").replaceFirst(suffix, "");
    long result = 0;
    int base = format.characters.length();

    for (int i = 0; i < cleaned.length(); i++) {
      char c = cleaned.charAt(i);
      int index = format.characters.indexOf(c);
      if (index == -1) {
        throw new MolgenisException("Invalid character in formatted value: " + c);
      }
      result = result * base + index;
    }

    return result;
  }

  public boolean valueCompliesToFormat(String value) {
    return Pattern.compile(prefix + "[" + format().getCharacters() + "]{" + length() + "}" + suffix)
        .matcher(value)
        .matches();
  }
}
