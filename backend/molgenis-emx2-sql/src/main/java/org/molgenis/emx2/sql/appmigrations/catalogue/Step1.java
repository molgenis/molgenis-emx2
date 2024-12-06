package org.molgenis.emx2.sql.appmigrations.catalogue;

import org.molgenis.emx2.Database;

public class Step1 extends CatalogueMigrationStep {
  @Override
  public void execute(Database db, String schemaName) {
    log.info("Running catalogue step 1 for schema {}", schemaName);
  }
}
