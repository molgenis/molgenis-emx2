package org.molgenis.emx2.datamodels;

import org.junit.jupiter.api.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("slow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestLoaders {

  public static final String DATA_CATALOGUE = "catalogue";
  public static final String COHORT_STAGING = "CohortStaging";
  public static final String NETWORK_STAGING = "NetworkStaging";
  public static final String DATA_CATALOGUE_AGGREGATES = "AggregatesTest";
  public static final String DIRECTORY_TEST = "DirectoryTest";
  public static final String DIRECTORY_STAGING = "DirectoryStaging";
  public static final String FAIR_GENOMES = "FAIRGenomesTest";
  public static final String PORTAL_TEST = "PortalTest";
  public static final String PROJECT_MANAGER = "ProjectManager";
  public static final String CATALOGUE_ONTOLOGIES = "CatalogueOntologies";
  public static final String DIRECTORY_ONTOLOGIES = "DirectoryOntologies";
  public static final String DASHBOARD_TEST = "UiDashboardTest";
  public static final String PATIENT_REGISTRY_DEMO = "patientRegistryDemo";
  public static final String PATIENT_REGISTRY = "patientRegistry";
  public static final String PAGES_SCHEMA = "pagesSchema";

  protected static Database database;

  protected static Schema dataCatalogue;
  protected static Schema cohortStaging;
  protected static Schema networkStaging;
  protected static Schema directory;
  protected static Schema FAIRGenomesSchema;
  protected static Schema projectManagerSchema;
  protected static Schema directoryStaging;
  protected static Schema dashboard;
  protected static Schema patientRegistryDemo;
  protected static Schema patientRegistry;
  protected static Schema pagesSchema;

  @BeforeAll
  public void setup() {
    if (database == null) {

      database = TestDatabaseFactory.getTestDatabase();
      // prevent previous dangling test results
      database.dropSchemaIfExists(PORTAL_TEST);
      database.dropSchemaIfExists(COHORT_STAGING);
      database.dropSchemaIfExists(NETWORK_STAGING);
      database.dropSchemaIfExists(DATA_CATALOGUE);
      database.dropSchemaIfExists(DATA_CATALOGUE_AGGREGATES);
      database.dropSchemaIfExists(DIRECTORY_TEST);
      database.dropSchemaIfExists(DIRECTORY_STAGING);
      database.dropSchemaIfExists(FAIR_GENOMES);
      database.dropSchemaIfExists(PROJECT_MANAGER);
      database.dropSchemaIfExists(DASHBOARD_TEST);
      database.dropSchemaIfExists(PATIENT_REGISTRY_DEMO);
      database.dropSchemaIfExists(PATIENT_REGISTRY);
      database.dropSchemaIfExists(PAGES_SCHEMA);

      // delete ontologies last
      database.dropSchemaIfExists(DIRECTORY_ONTOLOGIES);
      database.dropSchemaIfExists(CATALOGUE_ONTOLOGIES);

      DataModels.Profile.DATA_CATALOGUE.getImportTask(database, DATA_CATALOGUE, "test", true).run();
      dataCatalogue = database.getSchema(DATA_CATALOGUE);
      DataModels.Profile.DATA_CATALOGUE_COHORT_STAGING
          .getImportTask(database, COHORT_STAGING, "test", true)
          .run();
      cohortStaging = database.getSchema(COHORT_STAGING);
      DataModels.Profile.DATA_CATALOGUE_NETWORK_STAGING
          .getImportTask(database, NETWORK_STAGING, "test", true)
          .run();
      networkStaging = database.getSchema(NETWORK_STAGING);
      DataModels.Regular.PROJECTMANAGER
          .getImportTask(database, PROJECT_MANAGER, "test", true)
          .run();
      projectManagerSchema = database.getSchema(PROJECT_MANAGER);
      DataModels.Regular.BIOBANK_DIRECTORY_STAGING
          .getImportTask(database, DIRECTORY_STAGING, "test", false)
          .run();
      directoryStaging = database.getSchema(DIRECTORY_STAGING);
      DataModels.Regular.UI_DASHBOARD.getImportTask(database, DASHBOARD_TEST, "test", true).run();
      dashboard = database.getSchema(DASHBOARD_TEST);
      DataModels.Profile.PATIENT_REGISTRY
          .getImportTask(database, PATIENT_REGISTRY, "test", true)
          .run();
      patientRegistry = database.getSchema(PATIENT_REGISTRY);
      DataModels.Regular.PATIENT_REGISTRY_DEMO
          .getImportTask(database, PATIENT_REGISTRY_DEMO, "test", true)
          .run();
      patientRegistryDemo = database.getSchema(PATIENT_REGISTRY_DEMO);
      DataModels.Profile.MG_CMS.getImportTask(database, PAGES_SCHEMA, "test", true).run();
      pagesSchema = database.getSchema(PAGES_SCHEMA);
      // This profile is broken
      //      FAIRGenomesSchema = database.createSchema(FAIR_GENOMES);
      //      DataModels.Profile.FAIR_GENOMES.getImportTask(FAIRGenomesSchema, true).run();
    }
  }
}
