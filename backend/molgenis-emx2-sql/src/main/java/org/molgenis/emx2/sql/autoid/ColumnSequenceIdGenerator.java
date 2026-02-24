package org.molgenis.emx2.sql.autoid;

import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.AUTOID_RANDOMIZER_KEY_SETTING;
import static org.molgenis.emx2.Constants.MOLGENIS_ID_RANDOMIZER_KEY;

import java.util.HexFormat;
import java.util.List;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.jooq.DSLContext;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.molgenis.emx2.utils.generator.AutoIdFormat;
import org.molgenis.emx2.utils.generator.FeistelIdRandomizer;
import org.molgenis.emx2.utils.generator.IdGenerator;

public class ColumnSequenceIdGenerator implements IdGenerator {

  private static final byte[] DEFAULT_KEY =
      HexFormat.of()
          .parseHex(
              (String)
                  EnvironmentProperty.getParameter(
                      MOLGENIS_ID_RANDOMIZER_KEY, "2B7E151628AED2A6ABF7158809CF4F3C", STRING));

  private static final Pattern FUNCTION_PATTERN = Pattern.compile("(?<func>\\$\\{mg_autoid[^}]*})");

  private final AutoIdFormat autoIdFormat;
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

    String computedFormat = column.getComputed();
    try (Scanner scanner = new Scanner(computedFormat)) {
      List<AutoIdFormat> results =
          scanner
              .findAll(FUNCTION_PATTERN)
              .map(MatchResult::group)
              .map(AutoIdFormat::fromComputedString)
              .toList();

      if (results.isEmpty()) {
        throw new MolgenisException(
            "Invalid computed value provided, requires at least one autoid instance");
      } else if (results.size() > 1) {
        throw new MolgenisException(
            "Invalid computed value provided, only one autoid instance is allowed");
      }

      autoIdFormat = results.getFirst();
      computedFormat = computedFormat.replaceAll(FUNCTION_PATTERN.pattern(), "%s");
      String name = getSequenceNameForColumn(column);
      if (!SqlSequence.exists(jooq, column.getSchemaName(), name)) {
        long limit = autoIdFormat.getMaxValue() + 1;
        sequence = SqlSequence.create(jooq, column.getSchemaName(), name, limit);
      } else {
        sequence = new SqlSequence(jooq, column.getSchemaName(), name);
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

  @Override
  public String generateId() {
    long nextValue = sequence.getNextValue() - 1; // Sequences start counting at 1, not at 0
    long randomized = new FeistelIdRandomizer(sequence.getLimit(), key).randomize(nextValue);
    return format.formatted(autoIdFormat.mapToFormat(randomized));
  }

  public void updateSequenceForValue(String value) {
    // TODO: Remove support for multiple formats before we can validate the sequence value
  }
}
