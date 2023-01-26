package org.molgenis.emx2.datamodels;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.DATA_CATALOGUE;
import static org.molgenis.emx2.datamodels.DataCatalogueLoader.CATALOGUE_ONTOLOGIES;
import static org.molgenis.emx2.datamodels.DataCatalogueNetworkStagingLoader.SHARED_STAGING;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.OrderWith;
import org.junit.runner.manipulation.Alphanumeric;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@OrderWith(Alphanumeric.class)
public class TestLoaders {
  static Database database;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    // prevend previous dangling test results
    database.dropSchemaIfExists("catalogue");
    database.dropSchemaIfExists("RWEcatalogue");
    database.dropSchemaIfExists("CohortStaging");
    database.dropSchemaIfExists("CohortStaging");
    database.dropSchemaIfExists("NetworkStaging");
    database.dropSchemaIfExists("CohortStaging3");
    database.dropSchemaIfExists("NetworkStaging3");
  }

  @Test
  public void test1FAIRDataHubLoader() {
    Schema fairDataHubSchema = database.dropCreateSchema("FAIRDataHubTest");
    AvailableDataModels.FAIR_DATA_HUB.install(fairDataHubSchema, true);
    assertEquals(34, fairDataHubSchema.getTableNames().size());
  }

  @Test
  public void test2DataCatalogueLoader() {
    // staging catalogues will create 'DataCatalogue' and
    Schema dataCatalogue = database.dropCreateSchema("catalogue");
    cleanSharedSchemas();

    AvailableDataModels.DATA_CATALOGUE.install(dataCatalogue, true);
    assertEquals(37, dataCatalogue.getTableNames().size());

    // cleanup because shared schema
    database.dropSchema("catalogue");
  }

  @Test
  public void test3RWECatalogue() {
    Schema rweCatalogue = database.dropCreateSchema("RWEcatalogue");

    cleanSharedSchemas();
    AvailableDataModels.DATA_CATALOGUE.install(rweCatalogue, false);
    MolgenisIO.fromClasspathDirectory("datacatalogue/RWEcatalogue", rweCatalogue, false);

    // cleanup because shared schema
    database.dropSchemaIfExists("RWEcatalogue");
  }

  @Test
  public void test4DataCatalogueCohortStagingLoader() {
    Schema cohortStaging = database.dropCreateSchema("CohortStaging");
    cleanSharedSchemas();

    AvailableDataModels.DATA_CATALOGUE_COHORT_STAGING.install(cohortStaging, true);
    assertEquals(18, cohortStaging.getTableNames().size());

    // cleanup because shared schema
    database.dropSchemaIfExists("CohortStaging");
  }

  @Test
  public void test5DataCatalogueNetworkStagingLoader() {
    Schema networkStaging = database.dropCreateSchema("NetworkStaging");
    cleanSharedSchemas();
    AvailableDataModels.DATA_CATALOGUE_NETWORK_STAGING.install(networkStaging, true);
    assertEquals(13, networkStaging.getTableNames().size());

    // cleanup because shared schema
    database.dropSchemaIfExists("NetworkStaging");
  }

  @Test
  public void test7DataCatalogueCohortStagingLoader3() {
    Schema cohortStaging3 = database.dropCreateSchema("CohortStaging3");
    cleanSharedSchemas();

    AvailableDataModels.DATA_CATALOGUE_COHORT_STAGING3.install(cohortStaging3, true);
    assertEquals(17, cohortStaging3.getTableNames().size());

    // cleanup because shared schema
    database.dropSchemaIfExists("CohortStaging3");
  }

  @Test
  public void test8DataCatalogueNetworkStagingLoader3() {
    Schema networkStaging3 = database.dropCreateSchema("NetworkStaging3");
    cleanSharedSchemas();

    AvailableDataModels.DATA_CATALOGUE_NETWORK_STAGING3.install(networkStaging3, true);
    assertEquals(13, networkStaging3.getTableNames().size());

    // cleanup because shared schema
    database.dropSchemaIfExists("NetworkStaging3");
  }

  private static void cleanSharedSchemas() {
    database.dropSchemaIfExists(DATA_CATALOGUE);
    database.dropSchemaIfExists(SHARED_STAGING);
    database.dropSchemaIfExists(CATALOGUE_ONTOLOGIES);
  }
}
