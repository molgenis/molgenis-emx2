package org.molgenis.emx2.datamodels;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestCatalogueStagingModels {
  static Database database;
  static Schema dataCatalogue;
  static Schema cohortStaging;
  static Schema cohortStagingUMMCG;
  static Schema networkStaging;
  static Schema sharedStaging;
  static Schema sharedStagingUMCG;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();

    dataCatalogue = database.dropCreateSchema("DataCatalogue");

    sharedStaging = database.dropCreateSchema("SharedStaging");
    cohortStaging = database.dropCreateSchema("DataCatalogueCohortStaging");
    networkStaging = database.dropCreateSchema("networkStaging");

    sharedStagingUMCG = database.dropCreateSchema("SharedStagingUMCG");
    cohortStagingUMMCG = database.dropCreateSchema("UMCG");
  }

  @Test
  public void testStagingModels() {
    // precondition
    MolgenisIO.fromClasspathDirectory("datacatalogue", dataCatalogue, false);
    assertEquals(37, dataCatalogue.getTableNames().size());

    // general
    MolgenisIO.fromClasspathDirectory("DataCatalogueSharedStaging", sharedStaging, false);
    assertEquals(2, sharedStaging.getTableNames().size());

    MolgenisIO.fromClasspathDirectory("DataCatalogueNetworkStaging", networkStaging, false);
    assertEquals(12, networkStaging.getTableNames().size());

    MolgenisIO.fromClasspathDirectory("DataCatalogueCohortStaging", cohortStaging, false);
    assertEquals(17, cohortStaging.getTableNames().size());

    // UMCG
    MolgenisIO.fromClasspathDirectory("DataCatalogueSharedStagingUMCG", sharedStagingUMCG, false);
    assertEquals(4, sharedStagingUMCG.getTableNames().size());

    MolgenisIO.fromClasspathDirectory("DataCatalogueCohortStaging", cohortStagingUMMCG, false);
    assertEquals(17, cohortStagingUMMCG.getTableNames().size());
  }
}
