package org.molgenis.emx2.sql.profilemigrations.datacatalogue;

import org.molgenis.emx2.Profile;
import org.molgenis.emx2.sql.profilemigrations.ProfileMigrationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CatalogueMigrationStep implements ProfileMigrationStep {

  protected static final Logger log = LoggerFactory.getLogger(CatalogueMigrationStep.class);

  @Override
  public Profile getProfile() {
    return Profile.DATA_CATALOGUE;
  }
}