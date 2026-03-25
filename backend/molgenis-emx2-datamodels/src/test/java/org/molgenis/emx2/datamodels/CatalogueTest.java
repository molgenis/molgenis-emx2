package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.rdf.CustomAssertions.adheresToShacl;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.fairmapper.dcat.DcatHarvestTask;
import org.molgenis.emx2.fairmapper.dcat.HarvestReport;
import org.molgenis.emx2.rdf.RdfSchemaService;
import org.molgenis.emx2.rdf.config.RdfConfig;

public class CatalogueTest extends TestLoaders {

  private static final String DCAT_ROUND_TRIP = "DcatRoundTrip";

  @Test
  void test06DataCatalogueLoader() throws Exception {
    assertEquals(24, dataCatalogue.getTableNames().size());

    // check compliance - when compliant, add: DCAT_AP_SHACL_FILES and HEALTH_RI_V2_SHACL_FILES
    adheresToShacl(dataCatalogue, "ejp-rd-vp");
    adheresToShacl(dataCatalogue, "hri-v2.0.2");
  }

  private static final String CATALOGUE_TYPE = "Catalogue";

  @Test
  void test06b_dcatRoundTripHarvest() throws Exception {
    String sourceRdf = exportToTurtle(dataCatalogue);
    assertFalse(sourceRdf.isBlank());

    List<Row> sourceResources = getResources(dataCatalogue);
    List<Row> sourceCatalogues = filterByDcatType(sourceResources, CATALOGUE_TYPE);

    assertFalse(sourceCatalogues.isEmpty(), "Source should have Catalogue resources");

    database.dropSchemaIfExists(DCAT_ROUND_TRIP);
    try {
      DataModels.Profile.DATA_CATALOGUE
          .getImportTask(database, DCAT_ROUND_TRIP, "roundtrip test", false)
          .run();
      Schema target = database.getSchema(DCAT_ROUND_TRIP);

      DcatHarvestTask harvestTask = new DcatHarvestTask(target, "roundtrip-test", sourceRdf);
      HarvestReport report = harvestTask.executeHarvest();

      assertTrue(report.getErrors().isEmpty(), "Harvest errors: " + report.getErrors());
      assertTrue(report.getResourcesImported() > 0, "Should import resources");

      List<Row> targetResources = getResources(target);
      List<Row> targetCatalogues = filterByDcatType(targetResources, CATALOGUE_TYPE);

      assertEquals(
          sourceCatalogues.size(),
          targetCatalogues.size(),
          "Target Catalogue count should match source");

      assertFalse(targetResources.isEmpty(), "Target should have resources after harvest");

      Row sourceCatalogue = sourceCatalogues.get(0);
      Row targetCatalogue =
          targetCatalogues.stream()
              .filter(row -> sourceCatalogue.getString("id").equals(row.getString("id")))
              .findFirst()
              .orElse(null);
      assertNotNull(
          targetCatalogue,
          "Target should have Catalogue with id: " + sourceCatalogue.getString("id"));

      assertEquals(sourceCatalogue.getString("name"), targetCatalogue.getString("name"));
      assertEquals(
          sourceCatalogue.getString("description"), targetCatalogue.getString("description"));
      assertEquals(sourceCatalogue.getString("pid"), targetCatalogue.getString("pid"));
    } finally {
      database.dropSchemaIfExists(DCAT_ROUND_TRIP);
    }
  }

  private List<Row> getResources(Schema schema) {
    return schema.getTable("Resources").retrieveRows();
  }

  private List<Row> filterByDcatType(List<Row> rows, String dcatType) {
    return rows.stream()
        .filter(row -> hasDcatType(row, dcatType))
        .sorted(Comparator.comparing(row -> row.getString("id")))
        .toList();
  }

  private boolean hasDcatType(Row row, String type) {
    return hasDcatType(row, Set.of(type));
  }

  private boolean hasDcatType(Row row, Set<String> types) {
    String[] rowTypes = row.getStringArray("type");
    if (rowTypes == null) return false;
    return Arrays.stream(rowTypes).anyMatch(types::contains);
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
