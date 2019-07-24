package org.molgenis.integrationtest;

import org.junit.Test;
import org.molgenis.Database;
import org.molgenis.MolgenisException;
import org.molgenis.sql.DatabaseFactory;

import java.io.File;

public class TestImport {

  @Test
  public void loadSimpleIntoDb() throws MolgenisException {

    // get database
    Database db = DatabaseFactory.getDatabase();

    // get directory to import
    File dir;
  }
}
