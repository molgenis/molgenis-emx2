package org.molgenis.emx2.utils.generator;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;

public class IdGeneratorService {

  private static final Cache<String, IdGenerator> STRATEGY_CACHE =
      Caffeine.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();

  public String generateIdForColumn(Column column) {
    if (!ColumnType.AUTO_ID.equals(column.getColumnType())) {
      throw new MolgenisException("Column type needs to be " + ColumnType.AUTO_ID);
    }

    if (column.getComputed() != null) {
      return new IdGeneratorService().generateId(column.getComputed());
    } else {
      return SnowflakeIdGenerator.getInstance().generateId();
    }
  }

  private String generateId(String strategy) {
    IdGenerator generator = STRATEGY_CACHE.getIfPresent(strategy);
    if (generator == null) {
      try {
        generator = FormattedIdGenerator.fromFormat(strategy);
        STRATEGY_CACHE.put(strategy, generator);
      } catch (IllegalArgumentException e) {
        throw new MolgenisException(
            "unable to generate auto-id for computed value: " + strategy, e);
      }
    }

    return generator.generateId();
  }
}
