package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.rdf.CustomAssertions.adheresToShacl;

import java.io.ByteArrayOutputStream;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.fairmapper.dcat.DcatHarvestTask;
import org.molgenis.emx2.fairmapper.dcat.HarvestReport;
import org.molgenis.emx2.rdf.RdfSchemaService;
import org.molgenis.emx2.rdf.config.RdfConfig;

public class CatalogueTest extends TestLoaders {

  @Test
  void test06DataCatalogueLoader() throws Exception {
    assertEquals(24, dataCatalogue.getTableNames().size());

    // check compliance - when compliant, add: DCAT_AP_SHACL_FILES and HEALTH_RI_V2_SHACL_FILES
    adheresToShacl(dataCatalogue, "ejp-rd-vp");
    adheresToShacl(dataCatalogue, "hri-v2.0.2");
  }

  @Test
  void test06b_dcatRoundTripHarvest() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (RdfSchemaService rdf =
        new RdfSchemaService(
            "http://localhost:8080", dataCatalogue, RDFFormat.TURTLE, out, RdfConfig.semantic())) {
      rdf.getGenerator().generate(dataCatalogue);
    }
    String turtle = out.toString();
    assertFalse(turtle.isBlank());

    DcatHarvestTask harvestTask = new DcatHarvestTask(dataCatalogue, "roundtrip-test", turtle);
    HarvestReport report = harvestTask.harvestRdf(turtle);

    assertTrue(
        report.getErrors().isEmpty(),
        "Harvest should have no errors, but got: " + report.getErrors());
    assertTrue(report.getResourcesImported() > 0, "Harvest should import at least one resource");
  }

  @Test
  public void test07DataCatalogueCohortStagingLoader() {
    assertEquals(20, cohortStaging.getTableNames().size());
  }

  @Disabled
  @Test
  public void test08DataCatalogueNetworkStagingLoader() {
    assertEquals(15, networkStaging.getTableNames().size());
  }
}
