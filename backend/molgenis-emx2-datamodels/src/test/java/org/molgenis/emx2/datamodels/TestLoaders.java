package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestLoaders {
  public static final String DATA_CATALOGUE = "data-catalogue";
  public static final String COHORT_STAGING = "CohortStaging";
  public static final String NETWORK_STAGING = "NetworkStaging";
  public static final String FAIR_DATA_HUB_TEST = "FAIRDataHubTest";
  static Database database;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    // prevend previous dangling test results
    database.dropSchemaIfExists(DATA_CATALOGUE);
    database.dropSchemaIfExists(COHORT_STAGING);
    database.dropSchemaIfExists(NETWORK_STAGING);
  }

  @Test
  public void test1FAIRDataHubLoader() {
    Schema fairDataHubSchema = database.dropCreateSchema(FAIR_DATA_HUB_TEST);
    AvailableDataModels.FAIR_DATA_HUB.install(fairDataHubSchema, true);
    assertEquals(36, fairDataHubSchema.getTableNames().size());
  }

  @Test
  public void test2DataCatalogueLoader() {
    Schema dataCatalogue = database.dropCreateSchema(DATA_CATALOGUE);
    AvailableDataModels.DATA_CATALOGUE.install(dataCatalogue, true);
    assertEquals(33, dataCatalogue.getTableNames().size());
  }

  @Test
  public void test7DataCatalogueCohortStagingLoader() {
    Schema cohortStaging = database.dropCreateSchema(COHORT_STAGING);
    AvailableDataModels.DATA_CATALOGUE_COHORT_STAGING.install(cohortStaging, true);
    assertEquals(19, cohortStaging.getTableNames().size());
  }

  @Test
  public void test8DataCatalogueNetworkStagingLoader() {
    Schema networkStaging = database.dropCreateSchema(NETWORK_STAGING);
    AvailableDataModels.DATA_CATALOGUE_NETWORK_STAGING.install(networkStaging, true);
    assertEquals(16, networkStaging.getTableNames().size());
  }
}
