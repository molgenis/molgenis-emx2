package org.molgenis.emx2.sql.appmigrations.directory;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.sql.appmigrations.App;
import org.molgenis.emx2.sql.appmigrations.AppMigrationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DirectoryMigrationStep implements AppMigrationStep {

  protected static final Logger log = LoggerFactory.getLogger(DirectoryMigrationStep.class);

  public abstract void execute(Database db, String schemaName);

  @Override
  public App getApp() {
    return App.DIRECTORY;
  }
}
