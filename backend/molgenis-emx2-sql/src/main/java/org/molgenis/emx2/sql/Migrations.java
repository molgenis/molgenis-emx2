package org.molgenis.emx2.sql;

import static org.molgenis.emx2.sql.MetadataUtils.MOLGENIS;

import java.io.IOException;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Migrations {
  private static Logger logger = LoggerFactory.getLogger(Migrations.class);

  public static void initOrMigrate(SqlDatabase db) {
    int version = MetadataUtils.getVersion(db.getJooq());

    // migration steps
    if (version < 0) MetadataUtils.init(db.getJooq());
    if (version < 1)
      executeMigrationFile(db, "migration1.sql", "upgraded MOLGENIS.version_metadata");

    // if cannot migrate then throw a MolgenisException

    // update version, change this number if you create breaking change
    updateDatabaseVersion(db, 1);
  }

  private static void executeMigrationFile(SqlDatabase db, String sqlFile, String message) {
    try {
      String sql = new String(Migrations.class.getResourceAsStream(sqlFile).readAllBytes());
      db.getJooq().execute(sql);
      logger.debug(message + "(file = " + sqlFile);
    } catch (IOException e) {
      throw new MolgenisException(e.getMessage());
    }
  }

  private static void updateDatabaseVersion(SqlDatabase db, int newVersion) {
    MetadataUtils.setVersion(db.getJooq(), newVersion);
  }
}
