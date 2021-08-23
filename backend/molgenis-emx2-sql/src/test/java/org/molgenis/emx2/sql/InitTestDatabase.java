package org.molgenis.emx2.sql;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.molgenis.emx2.Database;

public class InitTestDatabase {

  @Test
  public void createDatabase() {
    // we want this run only once and NOT parallel for total test suite
    // AND we want run all other tests in parallel
    // so tests are in molgenis-emx2-sql-it ('integration test')
    // and 'init' only happence once, here
    Database db = new SqlDatabase(true);

    assertTrue(db.getDatabaseVersion() > 0);
  }
}
