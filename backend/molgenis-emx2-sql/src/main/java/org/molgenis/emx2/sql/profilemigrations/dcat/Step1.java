package org.molgenis.emx2.sql.profilemigrations.dcat;

import org.molgenis.emx2.Database;

public class Step1 extends DCATMigrationStep {
  @Override
  public void execute(Database db, String schemaName) {
    log.info("Running directory step 1 for schema {}", schemaName);
  }
}