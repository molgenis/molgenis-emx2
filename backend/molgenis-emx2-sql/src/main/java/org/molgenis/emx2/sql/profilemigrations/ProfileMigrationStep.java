package org.molgenis.emx2.sql.profilemigrations;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Profile;

public interface ProfileMigrationStep {
  void execute(Database db, String schemaName);

  Profile getProfile();
}
