package org.molgenis.emx2.sql;

import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Database;

public class TestDatabaseFactory {
  private static Database db;

  public static Database getTestDatabase() {
    System.setProperty(Constants.MOLGENIS_POSTGRES_URI, "jdbc:postgresql://localhost/azure");
    System.setProperty(Constants.MOLGENIS_POSTGRES_PASS, "azure");
    System.setProperty(Constants.MOLGENIS_POSTGRES_USER, "azure");

    if (db == null) {
      db = new SqlDatabase(false);
    }
    return db;
  }
}
