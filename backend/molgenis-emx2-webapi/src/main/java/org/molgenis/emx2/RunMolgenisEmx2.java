package org.molgenis.emx2;

import static org.molgenis.emx2.ColumnType.BOOL;
import static org.molgenis.emx2.ColumnType.INT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.molgenis.emx2.datamodels.BiobankDirectoryLoader;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.datamodels.PetStoreLoader;
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

  public static void main(String[] args) throws IOException {
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
            new PetStoreLoader(schema, true).run();
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
    logger.info("Before start");
    MolgenisWebservice.start(port);

    logger.info("After start");

    String relativeMigrationFilePath = "./apps/nuxt3-ssr/migrations/version1/migrate.py";
    String relativeRequirementsFilePath = "./apps/nuxt3-ssr/migrations/requirements.txt";
    String migrationFilePathString = "/nuxt3-ssr/migration/version1/migrate.py";

    // define commands (given tempDir as working directory)
    String createVenvCommand = "python3 -m venv venv";
    String activateCommand = "source venv/bin/activate";
    String pipUpgradeCommand = "pip3 install --upgrade pip";
    String installRequirementsCommand = "pip3 install -r " + relativeRequirementsFilePath;
    String runScriptCommand = "python3 -u " + relativeMigrationFilePath;
    String command =
        String.join(
            " && ",
            createVenvCommand,
            activateCommand,
            pipUpgradeCommand,
            installRequirementsCommand,
            runScriptCommand);
    logger.debug("Running migration script with command: {}", command);

    // String runScriptCommand = "pwd";
    ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
    builder.redirectErrorStream(true);
    Process process = builder.start();

    // Read the process output
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        // Log each line of output
        logger.info("Process output: {}", line);
      }
    }

    // Wait for the process to complete
    int exitCode = 0;
    try {
      exitCode = process.waitFor();
    } catch (InterruptedException e) {
      logger.error("Error waiting for migration process to complete", e);
      throw new RuntimeException(e);
    }
    logger.info("Migration process exited with code: {}", exitCode);
  }
}
