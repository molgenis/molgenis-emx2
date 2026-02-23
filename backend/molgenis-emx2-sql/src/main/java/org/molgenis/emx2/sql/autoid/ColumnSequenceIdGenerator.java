package org.molgenis.emx2.sql.autoid;

import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.AUTOID_RANDOMIZER_KEY_SETTING;
import static org.molgenis.emx2.Constants.MOLGENIS_ID_RANDOMIZER_KEY;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.jooq.DSLContext;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.molgenis.emx2.utils.generator.AutoIdFormat;
import org.molgenis.emx2.utils.generator.FeistelIdRandomizer;
import org.molgenis.emx2.utils.generator.IdGenerator;
import org.molgenis.emx2.utils.generator.SnowflakeIdGenerator;

public class ColumnSequenceIdGenerator implements IdGenerator {

  private static final byte[] DEFAULT_KEY =
      HexFormat.of()
          .parseHex(
              (String)
                  EnvironmentProperty.getParameter(
                      MOLGENIS_ID_RANDOMIZER_KEY, "2B7E151628AED2A6ABF7158809CF4F3C", STRING));

  private static final Pattern FUNCTION_PATTERN = Pattern.compile("(?<func>\\$\\{mg_autoid[^}]*})");
  private static final String SNOWFLAKE_PLACEHOLDER = "%sf%";
  private static final SnowflakeIdGenerator SNOWFLAKE_ID_GENERATOR =
      SnowflakeIdGenerator.getInstance();

  private final List<AutoIdFormat> formats;
  private final String format;
  private final SqlSequence sequence;
  private final byte[] key;

  public ColumnSequenceIdGenerator(Column column, DSLContext jooq) {
    if (column.getComputed() == null) {
      throw new IllegalArgumentException("Column needs to have a computed value");
    }

    key =
        column
            .getSchema()
            .findSettingValue(AUTOID_RANDOMIZER_KEY_SETTING)
            .map(String::getBytes)
            .orElse(DEFAULT_KEY);

    String computedFormat =
        column.getComputed().replace(Constants.COMPUTED_AUTOID_TOKEN, SNOWFLAKE_PLACEHOLDER);

    try (Scanner scanner = new Scanner(computedFormat)) {
      formats =
          scanner
              .findAll(FUNCTION_PATTERN)
              .map(MatchResult::group)
              .map(AutoIdFormat::fromComputedString)
              .toList();

      if (formats.isEmpty()) {
        sequence = null;
      } else {
        computedFormat = computedFormat.replaceAll(FUNCTION_PATTERN.pattern(), "%s");
        String name = getSequenceNameForColumn(column);
        if (!SqlSequence.exists(jooq, column.getSchemaName(), name)) {
          long limit = getCollectiveSequenceLimit(formats);
          sequence = SqlSequence.create(jooq, column.getSchemaName(), name, limit);
        } else {
          sequence = new SqlSequence(jooq, column.getSchemaName(), name);
        }
      }
    }

    format = computedFormat;
  }

  private static String getSequenceNameForColumn(Column column) {
    return String.join(
        "-",
        column.getSchemaName(),
        column.getTableName(),
        column.getName(),
        HexFormat.of().toHexDigits(column.getComputed().hashCode()));
  }

  private static long getCollectiveSequenceLimit(List<AutoIdFormat> formats) {
    // Sequences start counting at 1, not at 0
    return LongPack.maxPackValue(formats.stream().map(AutoIdFormat::getMaxValue).toList()) + 1;
  }

  @Override
  public String generateId() {
    String result = format.replace(SNOWFLAKE_PLACEHOLDER, SNOWFLAKE_ID_GENERATOR.generateId());

    if (!formats.isEmpty()) {
      long nextValue = sequence.getNextValue() - 1; // Sequences start counting at 1, not at 0
      long randomized = new FeistelIdRandomizer(sequence.getLimit(), key).randomize(nextValue);

      List<Long> maxValues = formats.stream().map(AutoIdFormat::getMaxValue).toList();
      List<Long> numbers = LongPack.fromValue(randomized, maxValues).numbers();

      List<String> idValues = new ArrayList<>();
      for (int i = 0; i < numbers.size(); i++) {
        AutoIdFormat currentFormat = formats.get(i);
        idValues.add(currentFormat.mapToFormat(numbers.get(i)));
      }
      return result.formatted(idValues.toArray());
    }

    return result;
  }

  public void updateSequenceForValue(String value) {
    // TODO: Remove support for multiple formats before we can validate the sequence value
    return;
  }
}
