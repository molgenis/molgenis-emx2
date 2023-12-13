package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;

class InitTestDatabase {

  @Test
  void createDatabase() {
    // we want this run only once and NOT parallel for total test suite
    // AND we want run all other tests in parallel
    // so tests are in molgenis-emx2-sql-it ('integration test')
    // and 'init' only happens once, here
    System.out.println("INITIALIZING DATABASE");
    Database db = new SqlDatabase(true);
    assertTrue(db.getDatabaseVersion() > 0);
  }
}
