package org.molgenis.emx2.sql.autoid;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.jooq.DSLContext;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.utils.generator.IdGenerator;
import org.molgenis.emx2.utils.generator.SnowflakeIdGenerator;

public class IdGeneratorService {

  private static final Cache<String, IdGenerator> STRATEGY_CACHE =
      Caffeine.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();

  private static final Pattern FUNCTION_PATTERN = Pattern.compile("(?<func>\\$\\{mg_autoid[^}]*})");

  private final DSLContext jooq;

  public IdGeneratorService(DSLContext jooq) {
    this.jooq = jooq;
  }

  public String generateIdForColumn(Column column) {
    if (!ColumnType.AUTO_ID.equals(column.getColumnType())) {
      throw new MolgenisException("Column type needs to be " + ColumnType.AUTO_ID);
    }

    if (column.getComputed() == null) {
      return SnowflakeIdGenerator.getInstance().generateId();
    } else {
      return getGenerator(column).generateId();
    }
  }

  private IdGenerator getGenerator(Column column) {
    String computed = column.getComputed();
    IdGenerator generator = STRATEGY_CACHE.getIfPresent(computed);

    if (generator == null) {
      try {
        validateComputed(computed);

        if (computed.contains(Constants.COMPUTED_AUTOID_TOKEN)) {
          generator =
              () ->
                  computed.replace(
                      Constants.COMPUTED_AUTOID_TOKEN,
                      SnowflakeIdGenerator.getInstance().generateId());
        } else {
          generator = new ColumnSequenceIdGenerator(column, jooq);
        }

        STRATEGY_CACHE.put(computed, generator);
      } catch (IllegalArgumentException e) {
        throw new MolgenisException(
            "unable to generate auto-id for computed value: " + computed, e);
      }
    }

    return generator;
  }

  private static void validateComputed(String computed) {
    FUNCTION_PATTERN.matcher(computed).find();
    try (Scanner scanner = new Scanner(computed)) {
      if (scanner.findAll(FUNCTION_PATTERN).count() > 1) {
        throw new MolgenisException(
            "Cannot generate autoid for column "
                + computed
                + " because mg_autoid can only be used once");
      }
    }
  }

  public void updateGeneratorForValue(Column column, String value) {
    if (!ColumnType.AUTO_ID.equals(column.getColumnType())) {
      return;
    }

    if (getGenerator(column) instanceof ColumnSequenceIdGenerator generator) {
      generator.updateSequenceForValue(value);
    }
  }
}
