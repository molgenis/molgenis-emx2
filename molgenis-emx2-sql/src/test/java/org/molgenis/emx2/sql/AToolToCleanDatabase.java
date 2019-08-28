package org.molgenis.emx2.sql;

import org.molgenis.emx2.utils.MolgenisException;

public class AToolToCleanDatabase {

  public static void main(String[] args) throws MolgenisException {
    DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }
}
