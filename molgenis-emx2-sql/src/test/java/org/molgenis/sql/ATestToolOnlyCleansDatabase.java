package org.molgenis.sql;

import org.molgenis.utils.MolgenisException;

public class ATestToolOnlyCleansDatabase {

  public static void main(String[] args) throws MolgenisException {
    DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }
}
