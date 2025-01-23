package org.molgenis.emx2;

import static org.molgenis.emx2.ColumnType.BOOL;
import static org.molgenis.emx2.ColumnType.INT;

import org.molgenis.emx2.datamodels.BiobankDirectoryLoader;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.molgenis.emx2.web.MolgenisWebservice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunMolgenisEmx2 {

  public static final String CATALOGUE_DEMO = "catalogue-demo";
  public static final String DIRECTORY_DEMO = "directory-demo";
  private static Logger logger = LoggerFactory.getLogger(RunMolgenisEmx2.class);

  public static final boolean INCLUDE_CATALOGUE_DEMO =
      (Boolean)
          EnvironmentProperty.getParameter(Constants.MOLGENIS_INCLUDE_CATALOGUE_DEMO, false, BOOL);
  public static final boolean INCLUDE_DIRECTORY_DEMO =
      (Boolean)
          EnvironmentProperty.getParameter(Constants.MOLGENIS_INCLUDE_DIRECTORY_DEMO, false, BOOL);
  public static final boolean EXCLUDE_PETSTORE_DEMO =
      (Boolean)
          EnvironmentProperty.getParameter(Constants.MOLGENIS_EXCLUDE_PETSTORE_DEMO, false, BOOL);

  public static void main(String[] args) {
    logger.info("Starting MOLGENIS EMX2 Software Version=" + Version.getVersion());

    Integer port = 8080;
    if (args.length >= 1) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        logger.error("Port number should be an integer, but was: " + args[0]);
        System.exit(1);
      }
    } else {
      port = (Integer) EnvironmentProperty.getParameter(Constants.MOLGENIS_HTTP_PORT, "8080", INT);
    }

    logger.info(
        "with "
            + org.molgenis.emx2.Constants.MOLGENIS_HTTP_PORT
            + "="
            + port
            + " (change either via java properties or via ENV variables)");

    // setup database
    Database database = new SqlDatabase(true);

    // elevate privileges for init
    database.tx(
        db -> {
          db.becomeAdmin();

          if (!EXCLUDE_PETSTORE_DEMO && db.getSchema("pet store") == null) {
            Schema schema = db.createSchema("pet store");
            DataModels.Profile.PET_STORE.getImportTask(schema, true).run();
          }

          if (INCLUDE_CATALOGUE_DEMO && db.getSchema(CATALOGUE_DEMO) == null) {
            Schema schema = db.createSchema(CATALOGUE_DEMO, "from DataCatalogue demo data loader");
            DataModels.Profile.DATA_CATALOGUE.getImportTask(schema, true).run();
          }
          if (INCLUDE_DIRECTORY_DEMO && db.getSchema(DIRECTORY_DEMO) == null) {
            Schema schema = db.createSchema(DIRECTORY_DEMO, "BBMRI-ERIC Directory Demo");
            new BiobankDirectoryLoader(schema, true).setStaging(false).run();
          }
        });

    // start
    MolgenisWebservice.start(port);
  }
}
