package org.molgenis.emx2.io;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.sql.SqlDatabase;

public class TestDatabaseFactory {
  private static Database db;

  public static Database getDatabase() {
    if (db == null) {
      db = new SqlDatabase(false);
    }
    return db;
  }
}
