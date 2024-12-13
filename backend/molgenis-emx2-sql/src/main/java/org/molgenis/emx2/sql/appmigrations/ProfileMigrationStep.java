package org.molgenis.emx2.sql.appmigrations;

import org.molgenis.emx2.Database;

public interface ProfileMigrationStep {
  void execute(Database db, String schemaName);

  Profile getProfile();
}
