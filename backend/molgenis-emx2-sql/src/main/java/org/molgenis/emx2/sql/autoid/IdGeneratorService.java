package org.molgenis.emx2.sql.autoid;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.utils.generator.DSLIdGenerator;
import org.molgenis.emx2.utils.generator.SnowflakeIdGenerator;

public class IdGeneratorService {

  private static final Cache<String, DSLIdGenerator> STRATEGY_CACHE =
      Caffeine.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();

  private static final Pattern FUNCTION_PATTERN = Pattern.compile("\\$\\{mg_autoid(\\([^)]*\\))?}");

  public Field<String> generateIdForColumn(Column column) {
    if (!ColumnType.AUTO_ID.equals(column.getColumnType())) {
      throw new MolgenisException("Column type needs to be " + ColumnType.AUTO_ID);
    }

    if (column.getComputed() == null) {
      return DSL.val(SnowflakeIdGenerator.getInstance().generateId());
    } else {
      return getGenerator(column).generateId();
    }
  }

  private DSLIdGenerator getGenerator(Column column) {
    String computed = column.getComputed();

    if (computed == null) {
      return () -> DSL.val(SnowflakeIdGenerator.getInstance().generateId());
    }

    DSLIdGenerator generator = STRATEGY_CACHE.getIfPresent(computed);

    if (generator == null) {
      try {
        validateComputed(computed);

        if (computed.contains(Constants.COMPUTED_AUTOID_TOKEN)) {
          generator =
              () ->
                  DSL.val(
                      computed.replace(
                          Constants.COMPUTED_AUTOID_TOKEN,
                          SnowflakeIdGenerator.getInstance().generateId()));
        } else {
          generator = new RetryingIdGenerator(column);
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
    if (FUNCTION_PATTERN.matcher(computed).results().count() > 1) {
      throw new MolgenisException(
          "Cannot generate autoid for column "
              + computed
              + " because mg_autoid can only be used once");
    }
  }
}
