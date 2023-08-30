package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.DATA_CATALOGUE;
import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.SHARED_STAGING;
import static org.molgenis.emx2.datamodels.DataCatalogueLoader.CATALOGUE_ONTOLOGIES;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
  }

  @Test
  public void test1FAIRDataHubLoader() {
    Schema fairDataHubSchema = database.createSchema(FAIR_DATA_HUB_TEST);
    AvailableDataModels.FAIR_DATA_HUB.install(fairDataHubSchema, true);
    assertEquals(62, fairDataHubSchema.getTableNames().size());
  }

  @Test
  public void test2DataCatalogueLoader() {
    Schema dataCatalogue = database.createSchema(DATA_CATALOGUE);
    AvailableDataModels.DATA_CATALOGUE.install(dataCatalogue, true);
    assertEquals(32, dataCatalogue.getTableNames().size());
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
    assertEquals(33, networkStaging.getTableNames().size());
  }
}
