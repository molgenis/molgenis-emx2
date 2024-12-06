package org.molgenis.emx2.sql.appmigrations.directory;

import org.molgenis.emx2.Database;

public class Step0 extends DirectoryMigrationStep {

  @Override
  public void execute(Database database, String schemaName) {
    System.out.println("Running directory step 0");
    log.info("Running directory step 0 for schema {}", schemaName);
  }
}
