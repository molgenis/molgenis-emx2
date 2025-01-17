package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.DATA_CATALOGUE;
import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.SHARED_STAGING;

import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportProfileTask;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("slow")
public class TestLoaders {
  public static final String COHORT_STAGING = "CohortStaging";
  public static final String NETWORK_STAGING = "NetworkStaging";
  public static final String DATA_CATALOGUE_AGGREGATES = "AggregatesTest";
  public static final String FAIR_DATA_HUB_TEST = "FAIRDataHubTest";
  public static final String DIRECTORY_TEST = "DirectoryTest";
  public static final String DIRECTORY_STAGING = "DirectoryStaging";
  public static final String RD3_TEST = "RD3Test";
  public static final String JRC_CDE_TEST = "JRCCDETest";
  public static final String FAIR_GENOMES = "FAIRGenomesTest";
  public static final String DCAT = "DCATTest";
  public static final String PORTAL_TEST = "PortalTest";
  public static final String FAIR_DATA_POINT = "FAIRDataPointTest";
  public static final String DCAT_BASIC = "DCATBasicTest";
  public static final String PROJECT_MANAGER = "ProjectManager";
  public static final String CATALOGUE_ONTOLOGIES = "CatalogueOntologies";
  public static final String DIRECTORY_ONTOLOGIES = "DirectoryOntologies";
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
    database.dropSchemaIfExists(FAIR_DATA_HUB_TEST);
    database.dropSchemaIfExists(SHARED_STAGING);
    database.dropSchemaIfExists(DIRECTORY_TEST);
    database.dropSchemaIfExists(DIRECTORY_STAGING);
    database.dropSchemaIfExists(DIRECTORY_ONTOLOGIES);
    database.dropSchemaIfExists(RD3_TEST);
    database.dropSchemaIfExists(JRC_CDE_TEST);
    database.dropSchemaIfExists(FAIR_GENOMES);
    database.dropSchemaIfExists(DCAT);
    database.dropSchemaIfExists(DCAT_BASIC);
    database.dropSchemaIfExists(DCAT_BASIC);
    database.dropSchemaIfExists(PROJECT_MANAGER);
    database.dropSchemaIfExists(FAIR_DATA_POINT);
    database.dropSchemaIfExists(FAIR_DATA_HUB_TEST);
    database.dropSchemaIfExists(PROJECT_MANAGER);
    // delete ontologies last
    database.dropSchemaIfExists(CATALOGUE_ONTOLOGIES);
  }

  @Test
  public void test01FAIRDataHubLoader() {
    Schema fairDataHubSchema = database.createSchema(FAIR_DATA_HUB_TEST);
    DataModels.Profile.FAIR_DATA_HUB.getImportTask(fairDataHubSchema, true).run();
    assertEquals(71, fairDataHubSchema.getTableNames().size());
    String[] semantics = fairDataHubSchema.getTable("BiospecimenType").getMetadata().getSemantics();
    assertEquals("http://purl.obolibrary.org/obo/NCIT_C70699", semantics[0]);
    assertEquals("http://purl.obolibrary.org/obo/NCIT_C70713", semantics[1]);
  }

  @Test
  void test06DataCatalogueLoader() {
    Schema dataCatalogue = database.createSchema(DATA_CATALOGUE);
    DataModels.Profile.DATA_CATALOGUE.getImportTask(dataCatalogue, true).run();
    assertEquals(23, dataCatalogue.getTableNames().size());
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

  @Test
  void test10RD3Loader() {
    Schema RD3Schema = database.createSchema(RD3_TEST);
    DataModels.Profile.RD3.getImportTask(RD3Schema, true).run();
    assertEquals(27, RD3Schema.getTableNames().size());
  }

  @Test
  void test11JRCCDELoader() {
    Schema JRCCDESchema = database.createSchema(JRC_CDE_TEST);
    DataModels.Profile.JRC_COMMON_DATA_ELEMENTS.getImportTask(JRCCDESchema, true).run();
    assertEquals(12, JRCCDESchema.getTableNames().size());
  }

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
  void test14DCATLoader() {
    Schema DCATSchema = database.createSchema(DCAT);
    DataModels.Profile.DCAT.getImportTask(DCATSchema, true).run();
    assertEquals(23, DCATSchema.getTableNames().size());
  }

  @Test
  void test15DirectoryStagingLoader() {
    Schema directoryStaging = database.createSchema(DIRECTORY_STAGING);
    DataModels.Regular.BIOBANK_DIRECTORY_STAGING.getImportTask(directoryStaging, false).run();
    assertEquals(8, directoryStaging.getTableNames().size());
  }

  @Test
  void test16DCATBasic() {
    Schema DCATSchema = database.createSchema(DCAT_BASIC);
    new ImportProfileTask(DCATSchema, "_profiles/test-only/DCAT-basic.yaml", true).run();
    assertEquals(9, DCATSchema.getTableNames().size());
  }

  @Test
  void test17FAIRDataPointLoader() {
    Schema FDPSchema = database.createSchema(FAIR_DATA_POINT);
    DataModels.Profile.FAIR_DATA_POINT.getImportTask(FDPSchema, true).run();
    assertEquals(25, FDPSchema.getTableNames().size());
  }

  @Test
  void test18PortalLoader() throws URISyntaxException, IOException {
    // depends on catalogue test above
    Schema schema = database.dropCreateSchema(PORTAL_TEST);
    DataModels.Regular.RD3_V2.getImportTask(schema, false).run();
    assertEquals(94, schema.getTableNames().size());
  }

  @Test
  void test19MigrationTestLoader() {
    Schema schema = database.dropCreateSchema("MigrationTest");
    DataModels.Profile.TEST_PROFILE_MIGRATION.getImportTask(schema, true).run();
    assertEquals(1, schema.getTableNames().size());
  }
}
