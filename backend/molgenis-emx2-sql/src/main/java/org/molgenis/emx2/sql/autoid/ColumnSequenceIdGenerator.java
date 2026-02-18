package org.molgenis.emx2.sql.autoid;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.jooq.DSLContext;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.utils.generator.AutoIdFormat;
import org.molgenis.emx2.utils.generator.IdGenerator;

public class ColumnSequenceIdGenerator implements IdGenerator {

  private static final Pattern FUNCTION_PATTERN = Pattern.compile("(?<func>\\$\\{mg_autoid[^}]*})");

  private final List<AutoIdFormat> formats;
  private final String format;
  private final SqlSequence sequence;

  public ColumnSequenceIdGenerator(Column column, DSLContext jooq) {
    if (column.getComputed() == null) {
      throw new IllegalArgumentException("Column needs to have a computed value");
    }

    try (Scanner scanner = new Scanner(column.getComputed())) {
      formats =
          scanner
              .findAll(FUNCTION_PATTERN)
              .map(MatchResult::group)
              .map(AutoIdFormat::fromComputedString)
              .toList();
      format = column.getComputed().replaceAll(FUNCTION_PATTERN.pattern(), "%s");

      String name = getSequenceNameForColumn(column);
      if (!SqlSequence.exists(jooq, column.getSchemaName(), column.getName())) {
        long limit = getCollectiveSequenceLimit(formats);
        sequence = SqlSequence.create(jooq, column.getSchemaName(), name, limit);
      } else {
        sequence = new SqlSequence(jooq, column.getSchemaName(), name);
      }
    }
  }

  private static String getSequenceNameForColumn(Column column) {
    return String.join(
        "-",
        column.getSchemaName(),
        column.getName(),
        String.valueOf(column.getComputed().hashCode()));
  }

  private static long getCollectiveSequenceLimit(List<AutoIdFormat> formats) {
    // Sequences start counting at 1, not at 0
    return LongPack.maxPackValue(formats.stream().map(AutoIdFormat::getMaxValue).toList()) + 1;
  }

  @Override
  public String generateId() {
    List<Long> maxValues = formats.stream().map(AutoIdFormat::getMaxValue).toList();
    long nextValue = sequence.nextValue() - 1; // Sequences start counting at 1, not at 0
    List<Long> numbers = LongPack.fromValue(nextValue, maxValues).numbers();
    List<String> vals = new ArrayList<>();

    for (int i = 0; i < numbers.size(); i++) {
      vals.add(formats.get(i).mapToFormat(numbers.get(i)));
    }

    return format.formatted(vals.toArray());
  }
}
