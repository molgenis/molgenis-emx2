package org.molgenis.emx2.sql.profilemigrations.dcat;

import org.molgenis.emx2.Profile;
import org.molgenis.emx2.sql.profilemigrations.ProfileMigrationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DCATMigrationStep implements ProfileMigrationStep {

  protected static final Logger log = LoggerFactory.getLogger(DCATMigrationStep.class);

  @Override
  public Profile getProfile() {
    return Profile.DCAT;
  }
}
