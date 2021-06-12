package org.molgenis.emx2.graphql;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.sql.SqlDatabase;

public class TestDatabaseFactory {
  private static Database db;

  public static Database getTestDatabase() {
    if (db == null) {
      db = new SqlDatabase(false);
    }
    return db;
  }
}
