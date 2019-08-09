package org.molgenis.sql;

import org.junit.Test;
import org.molgenis.MolgenisException;

public class ATestThatOnlyCleansDatabase {

  @Test
  public void testClean() throws MolgenisException {
    SqlDatabase db = (SqlDatabase) DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }
}
