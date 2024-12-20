package org.molgenis.emx2.sql;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.molgenis.emx2.Profile;
import org.molgenis.emx2.sql.appmigrations.ProfileMigrationStep;
import org.molgenis.emx2.sql.model.ProfileSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfileMigrations {
  private static final int DATA_CATALOGUE_CURRENT_PROFILE_STEP = 2;
  private static final int DCAT_CURRENT_PROFILE_STEP = 0;
  private static final int PET_STORE_CURRENT_PROFILE_STEP = 0;

  private static final Map<Profile, Integer> profileVersions =
      Map.of(
          Profile.DATA_CATALOGUE,
          DATA_CATALOGUE_CURRENT_PROFILE_STEP,
          Profile.DCAT,
          DCAT_CURRENT_PROFILE_STEP,
          Profile.PET_STORE,
          PET_STORE_CURRENT_PROFILE_STEP);
  private static final Logger log = LoggerFactory.getLogger(ProfileMigrations.class);

  private static final String QUERY =
      """
    select
      table_schema,
      profile,
      profile_migration_step
    from
      "MOLGENIS"."schema_metadata"
    where
      profile IS NOT NULL
     AND
      profile_migration_step IS NOT NULL
    """;

  public void runAppSchemaMigrations(SqlDatabase db) {

    var profileSchemas = db.getJooq().fetch(QUERY).stream().map(ProfileSchema::new).toList();
    for (var profileSchema : profileSchemas) {
      int currentSchemaVersion = profileSchema.appMigrationVersion();
      // keep migrating until we are at the latest version
      while (currentSchemaVersion < profileVersions.get(profileSchema.profile())) {
        log.info("Migrating profile schema: {}", profileSchema);
        try {
          ProfileMigrationStep migration =
              loadMigration(profileSchema.profile(), currentSchemaVersion);
          migration.execute(db, profileSchema.schemaName());
          currentSchemaVersion++;
        } catch (Exception e) {
          log.error("Error migrating profile schema: {}", profileSchema, e);
          break;
        }
      }
      // update the version in the database
      db.getJooq()
          .execute(
              "update \"MOLGENIS\".\"schema_metadata\" set profile_migration_step = "
                  + currentSchemaVersion
                  + " where table_schema = '"
                  + profileSchema.schemaName()
                  + "'");
    }
  }

  private ProfileMigrationStep loadMigration(Profile profile, int step)
      throws ClassNotFoundException,
          InvocationTargetException,
          InstantiationException,
          IllegalAccessException {
    Class<?> aClass =
        Class.forName(
            "org.molgenis.emx2.sql.appmigrations." + profile.name().toLowerCase() + ".Step" + step);
    return (ProfileMigrationStep) aClass.getConstructors()[0].newInstance();
  }
}
