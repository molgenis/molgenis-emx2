package org.molgenis.emx2.sql;

import static org.molgenis.emx2.sql.MetadataUtils.MOLGENIS;

import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Migrations {
  private static Logger logger = LoggerFactory.getLogger(Migrations.class);

  public static void initOrMigrate(SqlDatabase db) {
    int version = getDatabaseVersion(db);

    if (version < 0) {
      // steps to clean database
      MetadataUtils.init(db.getJooq());
      executeFile(db, "migration1.sql");
    } else {
      // steps for migrations go here
      if (version < 1) executeFile(db, "migration1.sql");
    }

    // if cannot migrate then throw a MolgenisException

    // update version, change this number if you create breaking change
    updateDatabaseVersion(db, 1);
  }

  private static void executeFile(SqlDatabase db, String sqlFile) {
    try {
      String sql = new String(Migrations.class.getResourceAsStream(sqlFile).readAllBytes());
      db.getJooq().execute(sql);
      logger.debug("applied migration: " + sqlFile);
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException(dae);
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage());
    }
  }

  /**
   * returns -1 if no schema exists <br>
   * returns 0 if no version number exists
   */
  private static int getDatabaseVersion(SqlDatabase db) {
    if (db.getJooq().meta().getSchemas(MOLGENIS).size() == 0) {
      return -1;
    }
    return MetadataUtils.getVersion(db.getJooq());
  }

  private static void updateDatabaseVersion(SqlDatabase db, int newVersion) {
    MetadataUtils.setVersion(db.getJooq(), newVersion);
  }
}
