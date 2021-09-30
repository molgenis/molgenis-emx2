package org.molgenis.emx2.sql;

import java.io.IOException;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Migrations {
  // version the current software needs to work
  private static final int SOFTWARE_DATABASE_VERSION = 3;
  private static Logger logger = LoggerFactory.getLogger(Migrations.class);

  public static synchronized void initOrMigrate(SqlDatabase db) {
    db.tx(
        // transaction ensures migration succeeds completely or is rolled back completely
        tdb -> {
          int version = MetadataUtils.getVersion(db.getJooq());

          // -1 getVersion indicates schema does not exist
          if (version < 0) MetadataUtils.init(((SqlDatabase) tdb).getJooq());

          // migration steps to update from a previous version to SOFTWARE_DATABASE_VERSION
          // idea is that all steps need to be run when initializing empty server
          // this ensures 'new' and 'updated' servers are equal and same logic is not specified 2x
          // (one for 'new' and one for 'migration')
          if (version < 1)
            executeMigrationFile(
                tdb,
                "migration1.sql",
                "database migration: MOLGENIS.version_metadata tracks int database version number checks against code instead of string version number from released jar");
          if (version < 2)
            executeMigrationFile(
                tdb,
                "migration2.sql",
                "database migration: role names are made case-sensitive matching schema names, to fix issue where roles where conflicting between schemas with same uppercase(name)");
          if (version < 3)
            executeMigrationFile(
                tdb,
                "migration3.sql",
                "database migration: add description column to MOLGENIS.schema_metadata to store schema description");

          // if cannot migrate then throw a MolgenisException. This happens in case of breaking
          // change for database backend.

          // if success, update version to SOFTWARE_DATABASE_VERSION
          updateDatabaseVersion((SqlDatabase) tdb, SOFTWARE_DATABASE_VERSION);
        });
  }

  static void executeMigrationFile(Database db, String sqlFile, String message) {
    try {
      String sql = new String(Migrations.class.getResourceAsStream(sqlFile).readAllBytes());
      ((SqlDatabase) db).getJooq().execute(sql);
      logger.debug(message + "(file = " + sqlFile);
    } catch (IOException e) {
      throw new MolgenisException(e.getMessage());
    }
  }

  private static void updateDatabaseVersion(SqlDatabase db, int newVersion) {
    MetadataUtils.setVersion(db.getJooq(), newVersion);
  }
}
