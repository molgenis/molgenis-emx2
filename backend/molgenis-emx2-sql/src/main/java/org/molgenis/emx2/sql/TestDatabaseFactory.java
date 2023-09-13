package org.molgenis.emx2.sql;

import org.molgenis.emx2.Database;

public class TestDatabaseFactory {
  private static Database db;

  public static Database getTestDatabase() {
    db = new SqlDatabase(false);
    db.setActiveUser(db.getAdminUserName());
    return db;
    // don't share the database between tests because different users different permissions.
  }
}
