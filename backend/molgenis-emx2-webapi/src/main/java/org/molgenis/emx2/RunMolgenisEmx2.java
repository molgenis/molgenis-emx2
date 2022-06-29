package org.molgenis.emx2;

import static org.molgenis.emx2.ColumnType.INT;

import org.molgenis.emx2.datamodels.AvailableDataModels;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.molgenis.emx2.web.MolgenisWebservice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunMolgenisEmx2 {

  private static final Logger logger = LoggerFactory.getLogger(RunMolgenisEmx2.class);

  public static void main(String[] args) {
    logger.info("Starting MOLGENIS EMX2 Software Version=" + Version.getVersion());

    Integer port =
        (Integer) EnvironmentProperty.getParameter(Constants.MOLGENIS_HTTP_PORT, "8080", INT);
    logger.info(
        "with "
            + org.molgenis.emx2.Constants.MOLGENIS_HTTP_PORT
            + "="
            + port
            + " (change either via java properties or via ENV variables)");

    // setup database
    Database db = new SqlDatabase(true);

    // elevate privileges for init
    try {
      db.becomeAdmin();
      initSchema("pet store", AvailableDataModels.PET_STORE, true, db);
      initSchema("tasks", AvailableDataModels.TASKS, false, db);
    } finally {
      // ensure to remove admin
      db.clearActiveUser();
    }

    // start
    MolgenisWebservice.start(port);
  }

  public static void initSchema(
      String schemaName, AvailableDataModels model, boolean includeExampleData, Database db) {
    if (db.getSchema(schemaName) == null) {
      Schema schema = db.createSchema(schemaName);
      model.install(schema, includeExampleData);
    }
  }
}
