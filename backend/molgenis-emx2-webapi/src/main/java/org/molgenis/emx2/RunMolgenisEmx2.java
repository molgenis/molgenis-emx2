package org.molgenis.emx2;

import static org.molgenis.emx2.ColumnType.BOOL;
import static org.molgenis.emx2.ColumnType.INT;

import org.molgenis.emx2.datamodels.BiobankDirectoryLoader;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.datamodels.ProfileLoader;
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

    Integer port =
        (Integer) EnvironmentProperty.getParameter(Constants.MOLGENIS_HTTP_PORT, "8080", INT);
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
            new PetStoreLoader().load(schema, true);
          }

          if (INCLUDE_CATALOGUE_DEMO && db.getSchema(CATALOGUE_DEMO) == null) {
            Schema schema = db.createSchema(CATALOGUE_DEMO, "from DataCatalogue demo data loader");
            new ProfileLoader("_profiles/DataCatalogue.yaml").load(schema, true);
          }
          if (INCLUDE_DIRECTORY_DEMO && db.getSchema(DIRECTORY_DEMO) == null) {
            Schema schema = db.createSchema(DIRECTORY_DEMO, "BBMRI-ERIC Directory Demo");
            new BiobankDirectoryLoader().load(schema, true);
          }
        });

    // start
    MolgenisWebservice.start(port);
  }
}
