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
  }

  @Test
  public void test1FAIRDataHubLoader() {
    Schema fairDataHubSchema = database.dropCreateSchema("FAIRDataHubTest");
    AvailableDataModels.FAIR_DATA_HUB.install(fairDataHubSchema, true);
    assertEquals(34, fairDataHubSchema.getTableNames().size());
  }

  @Test
  public void test2DataCatalogueLoader() {
    cleanSharedSchemas();
    Schema dataCatalogue =
        database.dropCreateSchema(
            "catalogue"); // staging catalogues will create 'DataCatalogue' and

    AvailableDataModels.DATA_CATALOGUE.install(dataCatalogue, true);
    assertEquals(37, dataCatalogue.getTableNames().size());
  }

  @Test
  public void test3RWECatalogue() {
    Schema rweCatalogue = database.dropCreateSchema("RWEcatalogue");
    AvailableDataModels.DATA_CATALOGUE.install(rweCatalogue, false);
    MolgenisIO.fromClasspathDirectory("datacatalogue/RWEcatalogue", rweCatalogue, false);
  }

  @Test
  public void test4DataCatalogueCohortStagingLoader() {
    database.dropSchemaIfExists(SHARED_STAGING);
    Schema cohortStaging = database.dropCreateSchema("CohortStaging");
    AvailableDataModels.DATA_CATALOGUE_COHORT_STAGING.install(cohortStaging, true);
    assertEquals(18, cohortStaging.getTableNames().size());
  }

  @Test
  public void test5DataCatalogueNetworkStagingLoader() {
    Schema networkStaging = database.dropCreateSchema("NetworkStaging");
    AvailableDataModels.DATA_CATALOGUE_NETWORK_STAGING.install(networkStaging, true);
    assertEquals(13, networkStaging.getTableNames().size());
  }

  @Test
  public void test6StagingModels() {
    //    cleanSharedSchemas();
    //    Schema cohortStagingUMMCG = database.dropCreateSchema("UMCG");
    //    // UMCG
    //    MolgenisIO.fromClasspathDirectory("datacatalogue/stagingCohorts", cohortStagingUMMCG,
    // false);
    //    assertEquals(17, cohortStagingUMMCG.getTableNames().size());
  }

  @Test
  public void test7DataCatalogueCohortStagingLoader3() {
    cleanSharedSchemas();
    Schema cohortStaging3 = database.dropCreateSchema("CohortStaging");
    AvailableDataModels.DATA_CATALOGUE_COHORT_STAGING3.install(cohortStaging3, true);
    assertEquals(17, cohortStaging3.getTableNames().size());
  }

  @Test
  public void test8DataCatalogueNetworkStagingLoader3() {
    Schema networkStaging3 = database.dropCreateSchema("NetworkStaging");
    AvailableDataModels.DATA_CATALOGUE_NETWORK_STAGING3.install(networkStaging3, true);
    assertEquals(13, networkStaging3.getTableNames().size());
  }

  private void cleanSharedSchemas() {
    database.dropSchemaIfExists(CATALOGUE_ONTOLOGIES);
    database.dropSchemaIfExists(DATA_CATALOGUE);
  }
}
