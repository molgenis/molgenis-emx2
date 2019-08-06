package org.molgenis.integrationtest;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.Database;
import org.molgenis.MolgenisException;
import org.molgenis.sql.DatabaseFactory;

import java.io.File;

public class TestImport {
  static Database db;

  @BeforeClass
  public static void setup() throws MolgenisException {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void loadSimpleIntoDb() throws MolgenisException {

    // get database

    // get directory to import
    File dir;
  }
}
