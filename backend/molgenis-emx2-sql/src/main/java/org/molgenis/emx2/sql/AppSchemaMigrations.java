package org.molgenis.emx2.sql;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.molgenis.emx2.sql.appmigrations.App;
import org.molgenis.emx2.sql.appmigrations.AppMigrationStep;
import org.molgenis.emx2.sql.model.AppSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppSchemaMigrations {
  private static final int CATALOG_APP_VERSION = 2;
  private static final int DIRECTORY_APP_VERSION = 0;

  private static final Map<App, Integer> appVersions =
      Map.of(
          App.CATALOGUE, CATALOG_APP_VERSION,
          App.DIRECTORY, DIRECTORY_APP_VERSION);
  private static final Logger log = LoggerFactory.getLogger(AppSchemaMigrations.class);

  private static final String QUERY =
      """
    select
      table_schema,
      app,
      app_migration_version
    from
      "MOLGENIS"."schema_metadata"
    where
      app IS NOT NULL
     AND
      app_migration_version IS NOT NULL
    """;

  public void runAppSchemaMigrations(SqlDatabase db) {

    var appSchemas = db.getJooq().fetch(QUERY).stream().map(AppSchema::new).toList();
    for (var appSchema : appSchemas) {
      int currentSchemaVersion = appSchema.appMigrationVersion();
      // keep migrating until we are at the latest version
      while (currentSchemaVersion < appVersions.get(appSchema.app())) {
        log.info("Migrating app schema: {}", appSchema);
        try {
          AppMigrationStep migration = loadMigration(appSchema.app(), currentSchemaVersion);
          migration.execute(db, appSchema.schemaName());
          currentSchemaVersion++;
        } catch (Exception e) {
          log.error("Error migrating app schema: {}", appSchema, e);
          break;
        }
      }
      // update the version in the database
      db.getJooq()
          .execute(
              "update \"MOLGENIS\".\"schema_metadata\" set app_migration_version = "
                  + currentSchemaVersion
                  + " where table_schema = '"
                  + appSchema.schemaName()
                  + "'");
    }
  }

  private AppMigrationStep loadMigration(App app, int version)
      throws ClassNotFoundException,
          InvocationTargetException,
          InstantiationException,
          IllegalAccessException {
    Class<?> aClass =
        Class.forName(
            "org.molgenis.emx2.sql.appmigrations." + app.name().toLowerCase() + ".Step" + version);
    return (AppMigrationStep) aClass.getConstructors()[0].newInstance();
  }
}
