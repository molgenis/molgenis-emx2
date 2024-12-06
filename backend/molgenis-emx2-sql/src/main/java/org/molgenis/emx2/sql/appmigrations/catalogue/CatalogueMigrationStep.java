package org.molgenis.emx2.sql.appmigrations.catalogue;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.sql.appmigrations.App;
import org.molgenis.emx2.sql.appmigrations.AppMigrationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CatalogueMigrationStep implements AppMigrationStep {

  protected static final Logger log = LoggerFactory.getLogger(CatalogueMigrationStep.class);

  public abstract void execute(Database db, String schemaName);

  @Override
  public App getApp() {
    return App.CATALOGUE;
  }
}
