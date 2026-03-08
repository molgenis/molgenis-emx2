package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.rdf.CustomAssertions.adheresToShacl;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Schema;
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
    String sourceRdf = exportToTurtle(dataCatalogue);
    assertFalse(sourceRdf.isBlank());

    int sourceResourceCount = dataCatalogue.getTable("Resources").retrieveRows().size();

    database.dropSchemaIfExists("DcatRoundTrip");
    try {
      DataModels.Profile.DATA_CATALOGUE
          .getImportTask(database, "DcatRoundTrip", "roundtrip test", false)
          .run();
      Schema target = database.getSchema("DcatRoundTrip");

      DcatHarvestTask harvestTask = new DcatHarvestTask(target, "roundtrip-test", sourceRdf);
      HarvestReport report = harvestTask.harvestRdf(sourceRdf);

      assertTrue(
          report.getErrors().isEmpty(),
          "Harvest should have no errors, but got: " + report.getErrors());
      assertTrue(
          report.getResourcesImported() > 0,
          "Harvest should import at least one resource, warnings: " + report.getWarnings());

      int targetResourceCount = target.getTable("Resources").retrieveRows().size();
      assertTrue(
          targetResourceCount >= sourceResourceCount - 5,
          "Target should have close to the same number of resources as source (got "
              + targetResourceCount
              + " vs "
              + sourceResourceCount
              + "); resources not exported as DCAT (e.g. type=Network) cannot be harvested");

      String targetRdf = exportToTurtle(target);
      assertFalse(targetRdf.isBlank());

      Model sourceModel = Rio.parse(new StringReader(sourceRdf), "", RDFFormat.TURTLE);
      Model targetModel = Rio.parse(new StringReader(targetRdf), "", RDFFormat.TURTLE);
      assertTrue(targetModel.size() > 0, "Target RDF export should not be empty");
      assertTrue(
          targetModel.size() >= sourceModel.size() / 10,
          "Target RDF should have at least 10% of the statements of source (got "
              + targetModel.size()
              + " vs "
              + sourceModel.size()
              + "); the harvest only imports DCAT resource fields, not linked data like Contacts or Ontologies");
    } finally {
      database.dropSchemaIfExists("DcatRoundTrip");
    }
  }

  private String exportToTurtle(Schema schema) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (RdfSchemaService rdf =
        new RdfSchemaService(
            "http://localhost:8080", schema, RDFFormat.TURTLE, out, RdfConfig.semantic())) {
      rdf.getGenerator().generate(schema);
    }
    return out.toString();
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
