package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.rdf.SHACLComplianceTester.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.rdf.RdfSchemaService;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("slow")
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
  static Database database;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    // prevent previous dangling test results
    database.dropSchemaIfExists(PORTAL_TEST);
    database.dropSchemaIfExists(COHORT_STAGING);
    database.dropSchemaIfExists(NETWORK_STAGING);
    database.dropSchemaIfExists(DATA_CATALOGUE);
    database.dropSchemaIfExists(DATA_CATALOGUE_AGGREGATES);
    database.dropSchemaIfExists(DIRECTORY_TEST);
    database.dropSchemaIfExists(DIRECTORY_STAGING);
    database.dropSchemaIfExists(DIRECTORY_ONTOLOGIES);
    database.dropSchemaIfExists(FAIR_GENOMES);
    database.dropSchemaIfExists(PROJECT_MANAGER);
    database.dropSchemaIfExists(DASHBOARD_TEST);
    // delete ontologies last
    database.dropSchemaIfExists(CATALOGUE_ONTOLOGIES);
  }

  @Test
  void test06DataCatalogueLoader() throws Exception {
    Schema dataCatalogue = database.createSchema(DATA_CATALOGUE);
    DataModels.Profile.DATA_CATALOGUE.getImportTask(dataCatalogue, true).run();
    assertEquals(24, dataCatalogue.getTableNames().size());

    // create rdf in memory
    OutputStream outputStream = new ByteArrayOutputStream();
    try (RdfSchemaService rdf =
        new RdfSchemaService(
            "http://localhost:8080", dataCatalogue, RDFFormat.TURTLE, outputStream)) {
      rdf.getGenerator().generate(dataCatalogue);
    }

    // check compliance - when compliant, add: DCAT_AP_SHACL_FILES and HEALTH_RI_V2_SHACL_FILES
    testShaclCompliance(FAIR_DATA_POINT_SHACL_FILES, outputStream.toString());
    testShaclCompliance(HEALTH_RI_V1_SHACL_FILES, outputStream.toString());
    testShaclCompliance(EJP_RD_VP_SHACL_FILES, outputStream.toString());
  }

  @Test
  public void test07DataCatalogueCohortStagingLoader() {
    Schema cohortStaging = database.createSchema(COHORT_STAGING);
    DataModels.Profile.DATA_CATALOGUE_COHORT_STAGING.getImportTask(cohortStaging, true).run();
    assertEquals(18, cohortStaging.getTableNames().size());
  }

  @Disabled
  @Test
  public void test08DataCatalogueNetworkStagingLoader() {
    Schema networkStaging = database.createSchema(NETWORK_STAGING);
    DataModels.Profile.DATA_CATALOGUE_NETWORK_STAGING.getImportTask(networkStaging, true).run();
    assertEquals(15, networkStaging.getTableNames().size());
  }

  @Test
  public void test09DirectoryLoader() {
    Schema directory = database.createSchema(DIRECTORY_TEST);
    DataModels.Regular.BIOBANK_DIRECTORY.getImportTask(directory, true).run();
    assertEquals(13, directory.getTableNames().size());
  }

  @Disabled
  @Test
  void test12FAIRGenomesLoader() {
    Schema FAIRGenomesSchema = database.createSchema(FAIR_GENOMES);
    DataModels.Profile.FAIR_GENOMES.getImportTask(FAIRGenomesSchema, true).run();
    assertEquals(46, FAIRGenomesSchema.getTableNames().size());
  }

  @Test
  void test13ProjectManagerLoader() {
    Schema ProjectManagerSchema = database.createSchema(PROJECT_MANAGER);
    DataModels.Regular.PROJECTMANAGER.getImportTask(ProjectManagerSchema, true).run();
    assertEquals(5, ProjectManagerSchema.getTableNames().size());
  }

  @Test
  void test15DirectoryStagingLoader() {
    Schema directoryStaging = database.createSchema(DIRECTORY_STAGING);
    DataModels.Regular.BIOBANK_DIRECTORY_STAGING.getImportTask(directoryStaging, false).run();
    assertEquals(8, directoryStaging.getTableNames().size());
  }

  //  @Test
  //  void test17FAIRDataPointLoader() throws Exception {
  //    Schema FDPSchema = database.createSchema(FAIR_DATA_POINT);
  //    DataModels.Profile.FAIR_DATA_POINT.getImportTask(FDPSchema, true).run();
  //    assertEquals(25, FDPSchema.getTableNames().size());
  //
  //    // create rdf in memory
  //    OutputStream outputStream = new ByteArrayOutputStream();
  //    var rdf = new RDFService("http://localhost:8080", "/api/rdf", null);
  //    rdf.describeAsRDF(outputStream, null, null, null, FDPSchema);
  //
  //    // test compliance
  //    // testShaclCompliance(FAIR_DATA_POINT_SHACL_FILES, outputStream.toString());
  //    // testShaclCompliance(DCAT_AP_SHACL_FILES, outputStream.toString());
  //    // testShaclCompliance(HEALTH_RI_SHACL_FILES, outputStream.toString());
  //    // testShaclCompliance(EJP_RD_VP_SHACL_FILES, outputStream.toString());
  //  }
  //  @Test
  //  void test17FAIRDataPointLoader() throws Exception {
  //    Schema FDPSchema = database.createSchema(FAIR_DATA_POINT);
  //    DataModels.Profile.FAIR_DATA_POINT.getImportTask(FDPSchema, true).run();
  //    assertEquals(25, FDPSchema.getTableNames().size());
  //
  //    // create rdf in memory
  //    OutputStream outputStream = new ByteArrayOutputStream();
  //    var rdf = new RDFService("http://localhost:8080", "/api/rdf", null);
  //    rdf.describeAsRDF(outputStream, null, null, null, FDPSchema);
  //
  //    // test compliance
  //    // testShaclCompliance(FAIR_DATA_POINT_SHACL_FILES, outputStream.toString());
  //    // testShaclCompliance(DCAT_AP_SHACL_FILES, outputStream.toString());
  //    // testShaclCompliance(HEALTH_RI_SHACL_FILES, outputStream.toString());
  //    // testShaclCompliance(EJP_RD_VP_SHACL_FILES, outputStream.toString());
  //  }

  @Test
  void test18PortalLoader() throws URISyntaxException, IOException {
    // depends on catalogue test above
    Schema schema = database.dropCreateSchema(PORTAL_TEST);
    DataModels.Profile.PATIENT_REGISTRY.getImportTask(schema, false).run();
    assertEquals(49, schema.getTableNames().size());
  }

  @Test
  public void dashboardTestLoader() {
    Schema schema = database.dropCreateSchema(DASHBOARD_TEST);
    DataModels.Regular.UI_DASHBOARD.getImportTask(schema, true).run();
    assertEquals(7, schema.getTableNames().size());
  }

  @Test
  public void patientRegistryDemoTestLoader() {
    Schema schema = database.dropCreateSchema(PATIENT_REGISTRY_DEMO);
    DataModels.Regular.PATIENT_REGISTRY_DEMO.getImportTask(schema, true).run();
    assertEquals(86, schema.getTableNames().size());
  }
}
