package org.molgenis.emx2.utils.generator;

import static org.molgenis.emx2.Constants.*;

import java.util.List;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class FormattedIdGenerator implements IdGenerator {

  private static final Pattern FUNCTION_PATTERN = Pattern.compile("(?<func>\\$\\{mg_autoid[^}]*})");

  private final String format;
  private final List<IdGenerator> generators;

  private FormattedIdGenerator(String format, List<IdGenerator> generators) {
    this.format = format;
    this.generators = generators;
  }

  public static FormattedIdGenerator fromFormat(String format) {
    try (Scanner scanner = new Scanner(format)) {
      List<IdGenerator> generators =
          scanner
              .findAll(FUNCTION_PATTERN)
              .map(MatchResult::group)
              .map(FormattedIdGenerator::getGeneratorForFormat)
              .toList();

      String cleanedFormat = format.replaceAll(FUNCTION_PATTERN.pattern(), "%s");

      return new FormattedIdGenerator(cleanedFormat, generators);
    }
  }

  private static IdGenerator getGeneratorForFormat(String format) {
    if (COMPUTED_AUTOID_TOKEN.equals(format)) {
      return SnowflakeIdGenerator.getInstance();
    } else {
      AutoIdFormat config = AutoIdFormat.fromComputedString(format);
      return ConfiguringIdGenerator.fromAutoIdConfig(config);
    }
  }

  @Override
  public String generateId() {
    return format.formatted(generators.stream().map(IdGenerator::generateId).toArray());
  }
}
