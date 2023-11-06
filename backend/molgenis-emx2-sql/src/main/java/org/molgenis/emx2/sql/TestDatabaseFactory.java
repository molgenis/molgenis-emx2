package org.molgenis.emx2.sql;

import org.molgenis.emx2.Database;

public class TestDatabaseFactory {

  public static Database getTestDatabase() {
    Database db = new SqlDatabase(false);
    db.setActiveUser(db.getAdminUserName());
    return db;
    // don't share the database between tests because when setting active user that leadds to errors
  }
}
