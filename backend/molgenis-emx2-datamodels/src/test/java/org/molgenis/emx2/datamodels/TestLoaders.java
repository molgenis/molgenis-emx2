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
  static Schema beaconv2Schema;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    cohortsSchema = database.dropCreateSchema("CohortNetwork");
    stagingSchema = database.dropCreateSchema("CohortStaging");
    beaconv2Schema = database.dropCreateSchema("BeaconTest");
  }

  @Test
  public void testDataCatalogueLoader() {
    AvailableDataModels.DATA_CATALOGUE.install(cohortsSchema, true);
    assertEquals(37, cohortsSchema.getTableNames().size());
  }

  @Test
  public void testBeaconv2Loader() {
    AvailableDataModels.BEACON_V2.install(beaconv2Schema, true);
    assertEquals(32, beaconv2Schema.getTableNames().size());
  }

  @Test
  public void testDataCatalogueStagingLoader() {
    AvailableDataModels.DATA_CATALOGUE_STAGING.install(stagingSchema, true);
    assertEquals(7, stagingSchema.getTableNames().size());
  }
}
