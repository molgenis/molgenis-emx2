package org.molgenis.emx2.sql.appmigrations.datacatalogue;

import org.molgenis.emx2.Database;

public class Step0 extends CatalogueMigrationStep {
  @Override
  public void execute(Database db, String schemaName) {
    log.info("Running catalogue step 0 for schema {}", schemaName);
  }
}
