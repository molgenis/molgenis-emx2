package org.molgenis.emx2.sql;

import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Database;

public class TestDatabaseFactory {
  private static Database db;

  public static Database getTestDatabase() {
    System.setProperty(
        Constants.MOLGENIS_POSTGRES_URI, "jdbc:postgresql://localhost/molgenis_cloud");
    System.setProperty(Constants.MOLGENIS_POSTGRES_PASS, "molgenis_cloud");
    System.setProperty(Constants.MOLGENIS_POSTGRES_USER, "molgenis_cloud");

    if (db == null) {
      db = new SqlDatabase(true);
    }
    return db;
  }
}
