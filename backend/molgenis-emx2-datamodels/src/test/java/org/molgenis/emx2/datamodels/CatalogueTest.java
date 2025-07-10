package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.rdf.CustomAssertions.adheresToShacl;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.rdf.RdfSchemaService;

public class CatalogueTest extends TestLoaders {

  @Test
  void test06DataCatalogueLoader() throws Exception {
    assertEquals(24, dataCatalogue.getTableNames().size());

    // create rdf in memory
    OutputStream outputStream = new ByteArrayOutputStream();
    try (RdfSchemaService rdf =
        new RdfSchemaService(
            "http://localhost:8080", dataCatalogue, RDFFormat.TURTLE, outputStream)) {
      rdf.getGenerator().generate(dataCatalogue);
    }

    // check compliance - when compliant, add: DCAT_AP_SHACL_FILES and HEALTH_RI_V2_SHACL_FILES
    //    adheresToShacl(dataCatalogue, "dcat-ap-v3");
    adheresToShacl(dataCatalogue, "fdp-v1.2");
    adheresToShacl(dataCatalogue, "hri-v1");
    //    adheresToShacl(dataCatalogue, "hri-v2");
    adheresToShacl(dataCatalogue, "ejp-rd-vp");
  }

  @Test
  public void test07DataCatalogueCohortStagingLoader() {
    assertEquals(18, cohortStaging.getTableNames().size());
  }

  @Disabled
  @Test
  public void test08DataCatalogueNetworkStagingLoader() {
    assertEquals(15, networkStaging.getTableNames().size());
  }
}
