package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.rdf.CustomAssertions.adheresToShacl;

import org.junit.jupiter.api.Test;

public class CatalogueTest extends TestLoaders {

  @Test
  void test06DataCatalogueLoader() throws Exception {
    assertEquals(27, dataCatalogue.getTableNames().size());

    // check compliance - when compliant, add: DCAT_AP_SHACL_FILES and HEALTH_RI_V2_SHACL_FILES
    adheresToShacl(dataCatalogue, "ejp-rd-vp");
    adheresToShacl(dataCatalogue, "hri-v2.0.2");
  }

  @Test
  public void test07DataCatalogueCohortStagingLoader() {
    assertEquals(21, cohortStaging.getTableNames().size());
  }

  @Test
  public void test08DataCatalogueNetworkStagingLoader() {
    assertEquals(17, networkStaging.getTableNames().size());
  }
}
