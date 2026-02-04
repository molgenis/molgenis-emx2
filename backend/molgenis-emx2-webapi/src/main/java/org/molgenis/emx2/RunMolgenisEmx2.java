package org.molgenis.emx2;

import static org.molgenis.emx2.ColumnType.BOOL;
import static org.molgenis.emx2.ColumnType.INT;

import org.molgenis.emx2.datamodels.BiobankDirectoryLoader;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.datamodels.PatientRegistryDemoLoader;
import org.molgenis.emx2.io.ImportDataTask;
import org.molgenis.emx2.io.SchemaLoaderSettings;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvFilesClasspath;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.tasks.Task;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.molgenis.emx2.web.MolgenisWebservice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunMolgenisEmx2 {

  public static final String CATALOGUE_DEMO = "catalogue-demo";
  public static final String DIRECTORY_DEMO = "directory-demo";
  public static final String PET_STORE = "pet store";
  public static final String CATALOGUE_ONTOLOGIES = "CatalogueOntologies";
  private static final String ONTOLOGY_LOCATION = "/_ontologies";
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
  public static final boolean INCLUDE_TYPE_TEST_DEMO =
      (Boolean)
          EnvironmentProperty.getParameter(Constants.MOLGENIS_INCLUDE_TYPE_TEST_DEMO, false, BOOL);

  public static final boolean INCLUDE_PATIENT_REGISTRY_DEMO =
      (Boolean)
          EnvironmentProperty.getParameter(
              Constants.MOLGENIS_INCLUDE_PATIENT_REGISTRY_DEMO, false, BOOL);

  public static final boolean UPDATE_ONTOLOGIES =
      (Boolean)
          EnvironmentProperty.getParameter(
              Constants.MOLGENIS_UPDATE_ONTOLOGIES, false, BOOL);

  public static void main(String[] args) {
    logger.info("Starting MOLGENIS EMX2 Software Version=" + Version.getVersion());

    Integer port;
    if (args.length >= 1) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        logger.warn("Port number should be an integer, but was: {}", args[0]);
        port =
            (Integer) EnvironmentProperty.getParameter(Constants.MOLGENIS_HTTP_PORT, "8080", INT);
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

          if (!EXCLUDE_PETSTORE_DEMO && db.getSchema(PET_STORE) == null) {
            DataModels.Profile.PET_STORE.getImportTask(db, PET_STORE, "", true).run();
            Schema schema = db.getSchema(PET_STORE);
            schema.getDatabase().setUserPassword("customer", "customer");
            schema.getDatabase().setUserPassword("shopmanager", "shopmanager");
            schema.getDatabase().setUserPassword("shopowner", "shopowner");
            schema.getDatabase().setUserPassword("shopviewer", "shopviewer");
          }

          if (INCLUDE_TYPE_TEST_DEMO && db.getSchema("type test") == null) {
            DataModels.Profile.TYPE_TEST.getImportTask(db, "type test", "", true).run();
          }

          if (INCLUDE_CATALOGUE_DEMO && db.getSchema(CATALOGUE_DEMO) == null) {
            DataModels.Profile.DATA_CATALOGUE
                .getImportTask(db, CATALOGUE_DEMO, "from DataCatalogue demo data loader", true)
                .run();
          }
          if (INCLUDE_DIRECTORY_DEMO && db.getSchema(DIRECTORY_DEMO) == null) {
            new BiobankDirectoryLoader(
                    new SchemaLoaderSettings(db, DIRECTORY_DEMO, "BBMRI-ERIC Directory Demo", true))
                .setStaging(false)
                .run();
          }

          if (INCLUDE_PATIENT_REGISTRY_DEMO && db.getSchema("patient registry demo") == null) {
            new PatientRegistryDemoLoader(
                    new SchemaLoaderSettings(db, "patient registry demo", "", true))
                .run();
          }

          if (UPDATE_ONTOLOGIES && db.getSchema(CATALOGUE_ONTOLOGIES) != null) {
            Schema ontologySchema = db.getSchema(CATALOGUE_ONTOLOGIES);
            TableStore store = new TableStoreForCsvFilesClasspath(ONTOLOGY_LOCATION);
            Task ontologyTask =
                    new ImportDataTask(ontologySchema, store, false)
                            .setDescription("Import ontologies from profile");
            ontologyTask.run();
          }
        });

    // start
    MolgenisWebservice.start(port);
  }
}
