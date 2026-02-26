package org.molgenis.emx2.sql.autoid;

import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.AUTOID_RANDOMIZER_KEY_SETTING;
import static org.molgenis.emx2.Constants.MOLGENIS_ID_RANDOMIZER_KEY;

import java.util.HexFormat;
import org.jooq.DSLContext;
import org.molgenis.emx2.Column;
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

  private final AutoIdFormat autoIdFormat;
  private final SqlSequence sequence;
  private final FeistelIdRandomizer randomizer;

  public ColumnSequenceIdGenerator(Column column, DSLContext jooq) {
    if (column.getComputed() == null) {
      throw new IllegalArgumentException("Column needs to have a computed value");
    }
    byte[] key =
        column
            .getSchema()
            .findSettingValue(AUTOID_RANDOMIZER_KEY_SETTING)
            .map(String::getBytes)
            .orElse(DEFAULT_KEY);

    autoIdFormat = AutoIdFormat.fromComputedString(column.getComputed());
    String name = getSequenceNameForColumn(column);
    if (!SqlSequence.exists(jooq, column.getSchemaName(), name)) {
      long limit = autoIdFormat.getMaxValue() + 1;
      sequence = SqlSequence.create(jooq, column.getSchemaName(), name, limit);
    } else {
      sequence = new SqlSequence(jooq, column.getSchemaName(), name);
    }
    randomizer = new FeistelIdRandomizer(sequence.getLimit(), key);
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
    long randomized = randomizer.randomize(nextValue);
    return autoIdFormat.mapToFormat(randomized);
  }

  public void updateSequenceForValue(String value) {
    long raw = autoIdFormat.getValue(value);
    long reversed = randomizer.reverse(raw);

    if (sequence.getCurrentValue() < reversed) {
      sequence.setCurrentValue(reversed + 1);
    }
  }
}
