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
  public static final String DATA_CATALOGUE3 = "data-catalogue";
  public static final String COHORT_STAGING_3 = "CohortStaging3";
  public static final String NETWORK_STAGING_3 = "NetworkStaging3";
  public static final String FAIR_DATA_HUB_TEST = "FAIRDataHubTest";
  static Database database;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    // prevend previous dangling test results
    database.dropSchemaIfExists(DATA_CATALOGUE3);
    database.dropSchemaIfExists(COHORT_STAGING_3);
    database.dropSchemaIfExists(NETWORK_STAGING_3);
  }

  @Test
  public void test1FAIRDataHubLoader() {
    Schema fairDataHubSchema = database.dropCreateSchema(FAIR_DATA_HUB_TEST);
    AvailableDataModels.FAIR_DATA_HUB.install(fairDataHubSchema, true);
    assertEquals(34, fairDataHubSchema.getTableNames().size());
  }

  @Test
  public void test2DataCatalogueLoader() {
    Schema dataCatalogue = database.dropCreateSchema(DATA_CATALOGUE3);
    AvailableDataModels.DATA_CATALOGUE3.install(dataCatalogue, true);
    assertEquals(33, dataCatalogue.getTableNames().size());
  }

  @Test
  public void test7DataCatalogueCohortStagingLoader3() {
    Schema cohortStaging3 = database.dropCreateSchema(COHORT_STAGING_3);
    AvailableDataModels.DATA_CATALOGUE_COHORT_STAGING3.install(cohortStaging3, true);
    assertEquals(19, cohortStaging3.getTableNames().size());
  }

  @Test
  public void test8DataCatalogueNetworkStagingLoader3() {
    Schema networkStaging3 = database.dropCreateSchema(NETWORK_STAGING_3);
    AvailableDataModels.DATA_CATALOGUE_NETWORK_STAGING3.install(networkStaging3, true);
    assertEquals(16, networkStaging3.getTableNames().size());
  }
}
