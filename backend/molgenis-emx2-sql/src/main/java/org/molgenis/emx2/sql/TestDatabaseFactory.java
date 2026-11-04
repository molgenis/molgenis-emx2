package org.molgenis.emx2.sql;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.utils.generator.SnowflakeIdGenerator;

public final class TestDatabaseFactory {

  private TestDatabaseFactory() {
    // no instances
  }

  public static Database getTestDatabase() {
    Database db = new SqlDatabase(false);
    db.setActiveUser(db.getAdminUserName());

    if (!SnowflakeIdGenerator.hasInstance()) {
      SnowflakeIdGenerator.init("123");
    }

    return db;
  }
}
