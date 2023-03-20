package org.molgenis.emx2;

import static org.molgenis.emx2.ColumnType.BOOL;
import static org.molgenis.emx2.ColumnType.INT;

import org.molgenis.emx2.datamodels.AvailableDataModels;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.molgenis.emx2.web.MolgenisWebservice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunMolgenisEmx2 {

  private static Logger logger = LoggerFactory.getLogger(RunMolgenisEmx2.class);

  private static final boolean LOAD_ALL_DEMO_DATA =
      (Boolean)
          EnvironmentProperty.getParameter(Constants.MOLGENIS_LOAD_ALL_DEMO_DATA, false, BOOL);

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

      if (LOAD_ALL_DEMO_DATA) {
        logger.info("Loading all demo data...");
        for (AvailableDataModels model : AvailableDataModels.values()) {
          String schemaName = model.name().replaceAll("_", " ").toLowerCase();
          if (db.getSchema(schemaName) == null) {
            Schema schema = db.createSchema(schemaName);
            model.install(schema, true);
          }
        }
      } else if (db.getSchema("pet store") == null) {
        Schema schema = db.createSchema("pet store");
        new PetStoreLoader().load(schema, true);
      }

    } finally {
      // ensure to remove admin
      db.clearActiveUser();
    }

    // start
    MolgenisWebservice.start(port);
  }
}
