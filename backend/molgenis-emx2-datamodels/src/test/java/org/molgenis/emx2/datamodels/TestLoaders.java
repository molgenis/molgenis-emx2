package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.DATA_CATALOGUE;
import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.SHARED_STAGING;

import org.junit.jupiter.api.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("slow")
public class TestLoaders {
  public static final String COHORT_STAGING = "CohortStaging";
  public static final String NETWORK_STAGING = "NetworkStaging";
  public static final String FAIR_DATA_HUB_TEST = "FAIRDataHubTest";
  public static final String DIRECTORY_TEST = "DirectoryTest";
  public static final String RD3_TEST = "RD3Test";
  public static final String JRC_CDE_TEST = "JRCCDETest";
  public static final String FAIR_GENOMES = "FAIRGenomesTest";
  public static final String CATALOGUE_ONTOLOGIES = "CatalogueOntologies";

  static Database database;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    // prevend previous dangling test results
    database.dropSchemaIfExists(COHORT_STAGING);
    database.dropSchemaIfExists(NETWORK_STAGING);
    database.dropSchemaIfExists(DATA_CATALOGUE);
    database.dropSchemaIfExists(FAIR_DATA_HUB_TEST);
    database.dropSchemaIfExists(SHARED_STAGING);
    database.dropSchemaIfExists(CATALOGUE_ONTOLOGIES);
    database.dropSchemaIfExists(DIRECTORY_TEST);
    database.dropSchemaIfExists(RD3_TEST);
    database.dropSchemaIfExists(JRC_CDE_TEST);
    database.dropSchemaIfExists(FAIR_GENOMES);
  }

  @Test
  public void test1FAIRDataHubLoader() {
    Schema fairDataHubSchema = database.createSchema(FAIR_DATA_HUB_TEST);
    AvailableDataModels.FAIR_DATA_HUB.install(fairDataHubSchema, true);
    assertEquals(68, fairDataHubSchema.getTableNames().size());
    String[] semantics = fairDataHubSchema.getTable("BiospecimenType").getMetadata().getSemantics();
    assertEquals("http://purl.obolibrary.org/obo/NCIT_C70699", semantics[0]);
    assertEquals("http://purl.obolibrary.org/obo/NCIT_C70713", semantics[1]);
  }

  @Test
  public void test2DataCatalogueLoader() {
    Schema dataCatalogue = database.createSchema(DATA_CATALOGUE);
    AvailableDataModels.DATA_CATALOGUE.install(dataCatalogue, true);
    assertEquals(33, dataCatalogue.getTableNames().size());

    // test composite pkey having refs that are linked via refLink
    dataCatalogue
        .getTable("Variables")
        .groupBy()
        .select(s("count"), s("resource", s("name")), s("dataset", s("name")))
        .retrieveJSON();
  }

  @Test
  public void test7DataCatalogueCohortStagingLoader() {
    Schema cohortStaging = database.createSchema(COHORT_STAGING);
    AvailableDataModels.DATA_CATALOGUE_COHORT_STAGING.install(cohortStaging, true);
    assertEquals(19, cohortStaging.getTableNames().size());
  }

  @Test
  public void test8DataCatalogueNetworkStagingLoader() {
    Schema networkStaging = database.createSchema(NETWORK_STAGING);
    AvailableDataModels.DATA_CATALOGUE_NETWORK_STAGING.install(networkStaging, true);
    assertEquals(16, networkStaging.getTableNames().size());
  }

  @Test
  public void test9DirectoryLoader() {
    Schema networkStaging = database.createSchema(DIRECTORY_TEST);
    AvailableDataModels.BIOBANK_DIRECTORY.install(networkStaging, true);
    assertEquals(32, networkStaging.getTableNames().size());
  }

  @Test
  void test10RD3Loader() {
    Schema RD3Schema = database.createSchema(RD3_TEST);
    AvailableDataModels.RD3.install(RD3Schema, true);
    assertEquals(27, RD3Schema.getTableNames().size());
  }

  @Test
  void test11JRCCDELoader() {
    Schema JRCCDESchema = database.createSchema(JRC_CDE_TEST);
    AvailableDataModels.JRC_COMMON_DATA_ELEMENTS.install(JRCCDESchema, true);
    assertEquals(12, JRCCDESchema.getTableNames().size());
  }

  @Test
  void test12FAIRGenomesLoader() {
    Schema FAIRGenomesSchema = database.createSchema(FAIR_GENOMES);
    AvailableDataModels.FAIR_GENOMES.install(FAIRGenomesSchema, true);
    assertEquals(46, FAIRGenomesSchema.getTableNames().size());
  }
}
