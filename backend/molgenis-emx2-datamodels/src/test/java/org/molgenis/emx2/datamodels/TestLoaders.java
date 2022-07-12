package org.molgenis.emx2.datamodels;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestLoaders {
  static Database database;
  static Schema cohortsSchema;
  static Schema stagingSchema;
  static Schema fairDataHubSchema;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    cohortsSchema = database.dropCreateSchema("CohortNetwork");
    stagingSchema = database.dropCreateSchema("CohortStaging");
    fairDataHubSchema = database.dropCreateSchema("FAIRDataHubTest");
  }

  @Test
  public void testDataCatalogueLoader() {
    AvailableDataModels.DATA_CATALOGUE.install(cohortsSchema, true);
    assertEquals(37, cohortsSchema.getTableNames().size());
  }

  @Test
  public void testFAIRDataHubLoader() {
    AvailableDataModels.FAIR_DATA_HUB.install(fairDataHubSchema, true);
    assertEquals(27, fairDataHubSchema.getTableNames().size());
  }

  @Test
  public void testDataCatalogueStagingLoader() {
    AvailableDataModels.DATA_CATALOGUE_STAGING.install(stagingSchema, true);
    assertEquals(7, stagingSchema.getTableNames().size());
  }
}
