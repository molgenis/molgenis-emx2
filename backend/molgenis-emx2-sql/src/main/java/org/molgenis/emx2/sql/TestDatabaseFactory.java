package org.molgenis.emx2.sql;

import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;

import org.molgenis.emx2.Database;

public class TestDatabaseFactory {

  public static Database getTestDatabase() {
    return getTestDatabase(ADMIN_USER);
  }

  public static Database getTestDatabase(String user) {
    Database db = new SqlDatabase(user);
    return db;
    // don't share the database between tests because when setting active user that leadds to errors
  }
}
