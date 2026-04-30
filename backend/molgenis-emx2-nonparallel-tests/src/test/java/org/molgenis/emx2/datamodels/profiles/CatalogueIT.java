package org.molgenis.emx2.datamodels.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.rdf.CustomAssertions.adheresToShacl;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.datamodels.TestLoaders;

public class CatalogueIT extends TestLoaders {

  @Test
  void test06DataCatalogueLoader() throws Exception {
    assertEquals(27, dataCatalogue.getTableNames().size());

    adheresToShacl(dataCatalogue, "ejp-rd-vp");
    adheresToShacl(dataCatalogue, "hri-v2.0.2");
  }

  @Test
  public void test07DataCatalogueCohortStagingLoader() {
    assertEquals(21, cohortStaging.getTableNames().size());
  }

  @Test
  public void test08DataCatalogueRWEStagingLoader() {
    assertEquals(19, rweStaging.getTableNames().size());
  }

  @Test
  public void test08DataCatalogueNetworkStagingLoader() {
    assertEquals(16, networkStaging.getTableNames().size());
  }
}
