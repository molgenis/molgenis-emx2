package org.molgenis.emx2.sql;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.utils.generator.SnowflakeIdGenerator;

public class TestDatabaseFactory {

  public static Database getTestDatabase() {
    Database db = new SqlDatabase(SqlDatabase.ADMIN_USER);

    if (!SnowflakeIdGenerator.hasInstance()) SnowflakeIdGenerator.init("123");

    return db;
  }
}
