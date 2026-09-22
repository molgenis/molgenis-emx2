package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.molgenis.emx2.MolgenisException;

public record PostgresVersion(int majorVersion) {

  private static final int SUPPORTED_VERSION = 15;

  public static PostgresVersion fromDslContext(DSLContext dsl) {
    Integer version =
        dsl.select(DSL.field("current_setting('server_version_num') as version"))
            .fetchOne(0, Integer.class);

    if (version == null) {
      throw new MolgenisException("Unable to query PostgreSQL version");
    }

    return new PostgresVersion(version / 10_000);
  }

  public boolean isSupported() {
    return SUPPORTED_VERSION == majorVersion;
  }
}
