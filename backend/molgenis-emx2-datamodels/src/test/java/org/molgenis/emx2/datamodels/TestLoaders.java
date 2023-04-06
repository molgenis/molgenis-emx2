package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.DATA_CATALOGUE;
import static org.molgenis.emx2.datamodels.DataCatalogueLoader.CATALOGUE_ONTOLOGIES;
import static org.molgenis.emx2.datamodels.DataCatalogueNetworkStagingLoader.SHARED_STAGING;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestLoaders {
  public static final String DATA_CATALOGUE1 = "data-catalogue";
  public static final String RWE_CATALOGUE = "RWEcatalogue";
  public static final String COHORT_STAGING = "CohortStaging";
  public static final String NETWORK_STAGING = "NetworkStaging";
  public static final String COHORT_STAGING_3 = "CohortStaging3";
  public static final String NETWORK_STAGING_3 = "NetworkStaging3";
  public static final String FAIR_DATA_HUB_TEST = "FAIRDataHubTest";
  static Database database;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    // prevend previous dangling test results
    database.dropSchemaIfExists(DATA_CATALOGUE1);
    database.dropSchemaIfExists(RWE_CATALOGUE);
    database.dropSchemaIfExists(COHORT_STAGING);
    database.dropSchemaIfExists(NETWORK_STAGING);
    database.dropSchemaIfExists(COHORT_STAGING_3);
    database.dropSchemaIfExists(NETWORK_STAGING_3);
  }

  @Test
  public void test1FAIRDataHubLoader() {
    Schema fairDataHubSchema = database.dropCreateSchema(FAIR_DATA_HUB_TEST);
    AvailableDataModels.FAIR_DATA_HUB.install(fairDataHubSchema, true);
    assertEquals(36, fairDataHubSchema.getTableNames().size());
  }

  @Test
  public void test2DataCatalogueLoader() {
    // staging catalogues will create 'DataCatalogue' and
    Schema dataCatalogue = database.dropCreateSchema(DATA_CATALOGUE1);
    cleanSharedSchemas();

    AvailableDataModels.DATA_CATALOGUE.install(dataCatalogue, true);
    assertEquals(37, dataCatalogue.getTableNames().size());

    // cleanup because shared schema
    database.dropSchema(DATA_CATALOGUE1);
  }

  @Test
  public void test3RWECatalogue() {
    Schema rweCatalogue = database.dropCreateSchema(RWE_CATALOGUE);

    cleanSharedSchemas();
    AvailableDataModels.DATA_CATALOGUE.install(rweCatalogue, false);
    MolgenisIO.fromClasspathDirectory("datacatalogue/RWEcatalogue", rweCatalogue, false);

    // cleanup because shared schema
    database.dropSchemaIfExists(RWE_CATALOGUE);
  }

  @Test
  public void test4DataCatalogueCohortStagingLoader() {
    Schema cohortStaging = database.dropCreateSchema(COHORT_STAGING);
    cleanSharedSchemas();

    AvailableDataModels.DATA_CATALOGUE_COHORT_STAGING.install(cohortStaging, true);
    assertEquals(18, cohortStaging.getTableNames().size());

    // cleanup because shared schema
    database.dropSchemaIfExists(COHORT_STAGING);
  }

  @Test
  public void test5DataCatalogueNetworkStagingLoader() {
    Schema networkStaging = database.dropCreateSchema(NETWORK_STAGING);
    cleanSharedSchemas();
    AvailableDataModels.DATA_CATALOGUE_NETWORK_STAGING.install(networkStaging, true);
    assertEquals(13, networkStaging.getTableNames().size());

    // cleanup because shared schema
    database.dropSchemaIfExists(NETWORK_STAGING);
  }

  @Test
  public void test7DataCatalogueCohortStagingLoader3() {
    Schema cohortStaging3 = database.dropCreateSchema(COHORT_STAGING_3);
    cleanSharedSchemas();

    AvailableDataModels.DATA_CATALOGUE_COHORT_STAGING3.install(cohortStaging3, true);
    assertEquals(19, cohortStaging3.getTableNames().size());

    // cleanup because shared schema
    database.dropSchemaIfExists(COHORT_STAGING_3);
  }

  @Test
  public void test8DataCatalogueNetworkStagingLoader3() {
    Schema networkStaging3 = database.dropCreateSchema(NETWORK_STAGING_3);
    cleanSharedSchemas();

    AvailableDataModels.DATA_CATALOGUE_NETWORK_STAGING3.install(networkStaging3, true);
    assertEquals(16, networkStaging3.getTableNames().size());

    // cleanup because shared schema
    database.dropSchemaIfExists(NETWORK_STAGING_3);
  }

  private static void cleanSharedSchemas() {
    database.dropSchemaIfExists(DATA_CATALOGUE);
    database.dropSchemaIfExists(SHARED_STAGING);
    database.dropSchemaIfExists(CATALOGUE_ONTOLOGIES);
  }
}
