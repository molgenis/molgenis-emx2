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

  @BeforeAll
  public void setup() {
    if (database == null) {

      database = TestDatabaseFactory.getTestDatabase();
      // prevent previous dangling test results
      database.dropSchemaIfExists(PORTAL_TEST);
      //      database.dropSchemaIfExists(COHORT_STAGING);
      //      database.dropSchemaIfExists(NETWORK_STAGING);
      //      database.dropSchemaIfExists(DATA_CATALOGUE);
      //      database.dropSchemaIfExists(DATA_CATALOGUE_AGGREGATES);
      //      database.dropSchemaIfExists(DIRECTORY_TEST);
      //      database.dropSchemaIfExists(DIRECTORY_STAGING);
      //      database.dropSchemaIfExists(DIRECTORY_ONTOLOGIES);
      //      database.dropSchemaIfExists(FAIR_GENOMES);
      //      database.dropSchemaIfExists(PROJECT_MANAGER);
      //      database.dropSchemaIfExists(DASHBOARD_TEST);
      //      database.dropSchemaIfExists(PATIENT_REGISTRY_DEMO);
      //      database.dropSchemaIfExists(PATIENT_REGISTRY);
      //      // delete ontologies last
      //      database.dropSchemaIfExists(CATALOGUE_ONTOLOGIES);
      //
      //      dataCatalogue = database.createSchema(DATA_CATALOGUE);
      //      DataModels.Profile.DATA_CATALOGUE.getImportTask(dataCatalogue, true).run();
      //      cohortStaging = database.createSchema(COHORT_STAGING);
      //      DataModels.Profile.DATA_CATALOGUE_COHORT_STAGING.getImportTask(cohortStaging,
      // true).run();
      //      networkStaging = database.createSchema(NETWORK_STAGING);
      //      DataModels.Profile.DATA_CATALOGUE_NETWORK_STAGING.getImportTask(networkStaging,
      // true).run();
      //      directory = database.createSchema(DIRECTORY_TEST);
      //      DataModels.Regular.BIOBANK_DIRECTORY.getImportTask(directory, true).run();
      //      projectManagerSchema = database.createSchema(PROJECT_MANAGER);
      //      DataModels.Regular.PROJECTMANAGER.getImportTask(projectManagerSchema, true).run();
      //      directoryStaging = database.createSchema(DIRECTORY_STAGING);
      //      DataModels.Regular.BIOBANK_DIRECTORY_STAGING.getImportTask(directoryStaging,
      // false).run();
      //      dashboard = database.dropCreateSchema(DASHBOARD_TEST);
      //      DataModels.Regular.UI_DASHBOARD.getImportTask(dashboard, true).run();
      patientRegistry = database.getSchema(PATIENT_REGISTRY);
      //      DataModels.Profile.PATIENT_REGISTRY.getImportTask(patientRegistry, true).run();
      //      patientRegistryDemo = database.dropCreateSchema(PATIENT_REGISTRY_DEMO);
      //      DataModels.Regular.PATIENT_REGISTRY_DEMO.getImportTask(patientRegistryDemo,
      // true).run();
      // This profile is broken
      //      FAIRGenomesSchema = database.createSchema(FAIR_GENOMES);
      //      DataModels.Profile.FAIR_GENOMES.getImportTask(FAIRGenomesSchema, true).run();
    }
  }
}
