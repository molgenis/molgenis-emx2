package org.molgenis.emx2.sql.profilemigrations.dcat;

import org.molgenis.emx2.Database;

public class Step0 extends DCATMigrationStep {

  @Override
  public void execute(Database database, String schemaName) {
    System.out.println("Running directory step 0");
    log.info("Running directory step 0 for schema {}", schemaName);
  }
}
