package org.molgenis.emx2.sql;

import org.junit.Test;

public class TestDatabaseFactory {

  @Test
  public void createDatabase() {
    // we want this run only once and NOT parallel for total test suite
    // AND we want run all other tests in parallel
    // so tests are in molgenis-emx2-sql-it ('integration test')
    // and 'init' only happence once, here
    new SqlDatabase(true);
  }
}
