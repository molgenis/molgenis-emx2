package org.molgenis.emx2.sql.appmigrations;

import org.molgenis.emx2.Database;

public interface AppMigrationStep {
  void execute(Database db, String schemaName);

  App getApp();
}
