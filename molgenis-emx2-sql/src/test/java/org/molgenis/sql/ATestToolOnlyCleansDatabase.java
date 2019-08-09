package org.molgenis.sql;

import org.molgenis.MolgenisException;

public class ATestToolOnlyCleansDatabase {

  public static void main(String[] args) throws MolgenisException {
    DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }
}
