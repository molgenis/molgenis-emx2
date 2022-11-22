package org.molgenis.emx2.datamodels;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestLoaders {
  static Database database;
  static Schema fairDataHubSchema;
  static Schema dataCatalogue;
  static Schema rweCatalogue;

  // staging models
  static Schema cohortStaging;
  static Schema networkStaging;

  // umcg
  static Schema cohortStagingUMMCG;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    dataCatalogue =
        database.dropCreateSchema("catalogue"); // staging catalogues will create 'DataCatalogue'
    rweCatalogue = database.dropCreateSchema("RWEcatalogue");

    cohortStaging = database.dropCreateSchema("CohortStaging");
    networkStaging = database.dropCreateSchema("NetworkStaging");
    fairDataHubSchema = database.dropCreateSchema("FAIRDataHubTest");

    // umcg
    cohortStagingUMMCG = database.dropCreateSchema("UMCG");
  }

  @Test
  public void testDataCatalogueLoader() {
    AvailableDataModels.DATA_CATALOGUE.install(dataCatalogue, true);
    assertEquals(30, dataCatalogue.getTableNames().size());
  }

  @Test
  public void testRWECatalogue() {
    AvailableDataModels.DATA_CATALOGUE.install(rweCatalogue, false);
    MolgenisIO.fromClasspathDirectory("datacatalogue/RWEcatalogue", rweCatalogue, false);
  }

  @Test
  public void testFAIRDataHubLoader() {
    AvailableDataModels.FAIR_DATA_HUB.install(fairDataHubSchema, true);
    assertEquals(33, fairDataHubSchema.getTableNames().size());
  }

  @Test
  public void testDataCatalogueCohortStagingLoader() {
    AvailableDataModels.DATA_CATALOGUE_COHORT_STAGING.install(cohortStaging, true);
    assertEquals(17, cohortStaging.getTableNames().size());
  }

  @Test
  public void testDataCatalogueNetworkStagingLoader() {
    AvailableDataModels.DATA_CATALOGUE_NETWORK_STAGING.install(networkStaging, true);
    assertEquals(13, networkStaging.getTableNames().size());
  }

  @Test
  public void testStagingModels() {
    // UMCG
    MolgenisIO.fromClasspathDirectory("datacatalogue/stagingCohorts", cohortStagingUMMCG, false);
    assertEquals(17, cohortStagingUMMCG.getTableNames().size());
  }
}
