package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.molgenis.emx2.sql.MetadataUtils.*;

import com.google.common.collect.ImmutableMap;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Profile;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.model.ProfileSchema;
import org.molgenis.emx2.sql.profilemigrations.ProfileMigrationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfileMigrations {
  private static final Integer DATA_CATALOGUE_CURRENT_PROFILE_STEP = 2;
  private static final Integer DCAT_CURRENT_PROFILE_STEP = 0;
  private static final Integer MIGRATION_TEST_CURRENT_PROFILE_STEP = 0;

  public static final ImmutableMap<Profile, Integer> profileVersions =
      ImmutableMap.of(
          Profile.DATA_CATALOGUE,
          DATA_CATALOGUE_CURRENT_PROFILE_STEP,
          Profile.DCAT,
          DCAT_CURRENT_PROFILE_STEP,
          Profile.MIGRATION_TEST,
          MIGRATION_TEST_CURRENT_PROFILE_STEP);
  private static final Logger log = LoggerFactory.getLogger(ProfileMigrations.class);

  public List<ProfileSchema> runAppSchemaMigrations(SqlDatabase db) {

    List<ProfileSchema> profileSchemas =
        db
            .getJooq()
            .select(TABLE_SCHEMA, SCHEMA_PROFILE, SCHEMA_MIGRATION_STEP)
            .from(table(name(MOLGENIS, "schema_metadata")))
            .where(SCHEMA_PROFILE.isNotNull(), SCHEMA_MIGRATION_STEP.isNotNull())
            .fetch()
            .stream()
            .map(ProfileSchema::new)
            .toList();
    List<ProfileSchema> updatedProfileSchemas = new ArrayList<>();
    for (ProfileSchema profileSchema : profileSchemas) {
      db.tx(
          database -> {
            try {
              int currentSchemaVersion = profileSchema.appMigrationVersion();
              // keep migrating until we are at the latest version

              while (currentSchemaVersion
                  < profileVersions.getOrDefault(profileSchema.profile(), currentSchemaVersion)) {
                log.info("Migrating profile schema: {}", profileSchema);

                ProfileMigrationStep migration =
                    loadMigration(profileSchema.profile(), currentSchemaVersion);
                migration.execute(db, profileSchema.schemaName());
                currentSchemaVersion++;
              }
              // update the version in the database
              ProfileSchema updated =
                  setProfileMigrationStep(db, profileSchema, currentSchemaVersion);
              updatedProfileSchemas.add(updated);
            } catch (MolgenisException e) {
              throw new MolgenisException(
                  "Error migrating profile schema: " + profileSchema + " " + e.getMessage(), e);
            }
          });
    }
    return updatedProfileSchemas;
  }

  private static ProfileSchema setProfileMigrationStep(
      SqlDatabase db, ProfileSchema profileSchema, int currentSchemaVersion) {
    Schema schema =
        db.setSchemaProfileVersion(
            db.getSchema(profileSchema.schemaName()),
            profileSchema.profile(),
            currentSchemaVersion);
    return new ProfileSchema(
        schema.getMetadata().getName(),
        schema.getMetadata().getProfile(),
        schema.getMetadata().getProfileMigrationStep());
  }

  private ProfileMigrationStep loadMigration(Profile profile, int step) {
    String folderName = profile.name().toLowerCase().replace("_", "");
    Class<?> aClass;
    try {
      aClass =
          Class.forName("org.molgenis.emx2.sql.profilemigrations." + folderName + ".Step" + step);
    } catch (ClassNotFoundException e) {
      throw new MolgenisException("No migration found for profile " + profile + " step " + step, e);
    }
    try {
      return (ProfileMigrationStep) aClass.getConstructors()[0].newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new MolgenisException(
          "Error instantiating migration for profile " + profile + " step " + step, e);
    }
  }
}
