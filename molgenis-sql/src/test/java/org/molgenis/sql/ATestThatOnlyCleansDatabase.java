package org.molgenis.sql;

import org.junit.Test;
import org.molgenis.Database;
import org.molgenis.MolgenisException;

public class ATestThatOnlyCleansDatabase {

  @Test
  public void testClean() throws MolgenisException {
    SqlDatabase db = (SqlDatabase) DatabaseFactory.getDatabase("molgenis", "molgenis");
  }
}
