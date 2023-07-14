package org.molgenis.emx2.sql;

import org.molgenis.emx2.Database;

public class TestDatabaseFactory {
  private static Database db;

  public static Database getTestDatabase() {

    if (db == null) {
      db = new SqlDatabase(false);
      // default to admin user for the tests
      db.setActiveUser(db.getAdminUserName());
    }
    return db;
  }
}
