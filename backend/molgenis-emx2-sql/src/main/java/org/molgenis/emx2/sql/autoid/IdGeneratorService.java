package org.molgenis.emx2.sql.autoid;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.jooq.DSLContext;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.utils.generator.IdGenerator;
import org.molgenis.emx2.utils.generator.SnowflakeIdGenerator;

public class IdGeneratorService {

  private static final Cache<String, IdGenerator> STRATEGY_CACHE =
      Caffeine.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();

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
      return generateId(column);
    }
  }

  private String generateId(Column column) {
    IdGenerator generator = STRATEGY_CACHE.getIfPresent(column.getComputed());
    if (generator == null) {
      try {
        generator = new ColumnSequenceIdGenerator(column, jooq);
        STRATEGY_CACHE.put(column.getComputed(), generator);
      } catch (IllegalArgumentException e) {
        throw new MolgenisException(
            "unable to generate auto-id for computed value: " + column.getComputed(), e);
      }
    }

    return generator.generateId();
  }
}
