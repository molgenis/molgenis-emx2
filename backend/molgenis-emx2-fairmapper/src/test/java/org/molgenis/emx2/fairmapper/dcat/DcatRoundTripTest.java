package org.molgenis.emx2.fairmapper.dcat;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.fairmapper.rdf.JsonLdFrameGenerator;
import org.molgenis.emx2.rdf.RdfSchemaService;
import org.molgenis.emx2.rdf.config.RdfConfig;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("slow")
class DcatRoundTripTest {

  static Database database;
  static Schema source;
  static Schema target;
  static String turtleExport;
  static int sourceResourceCount;

  private static final Path FIXTURE_DIR =
      Path.of("src/test/resources/org/molgenis/emx2/fairmapper/dcat");
  private static final ObjectMapper PRETTY_MAPPER =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();

    database.dropSchemaIfExists("dcat_roundtrip_source");
    DataModels.Profile.DATA_CATALOGUE
        .getImportTask(database, "dcat_roundtrip_source", "source catalogue", true)
        .run();
    source = database.getSchema("dcat_roundtrip_source");

    database.dropSchemaIfExists("dcat_roundtrip_target");
    DataModels.Profile.DATA_CATALOGUE
        .getImportTask(database, "dcat_roundtrip_target", "target catalogue", true)
        .run();
    target = database.getSchema("dcat_roundtrip_target");
  }

  @Test
  void t01_sourceHasData() {
    Table resources = source.getTable("Resources");
    assertNotNull(resources);
    List<Row> rows = resources.retrieveRows();
    assertFalse(rows.isEmpty(), "Source should have demo resources");
    sourceResourceCount = rows.size();
  }

  @Test
  void t02_exportSourceAsTurtle() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (RdfSchemaService rdf =
        new RdfSchemaService(
            "http://localhost:8080", source, RDFFormat.TURTLE, out, RdfConfig.semantic())) {
      rdf.getGenerator().generate(source);
    }
    turtleExport = out.toString();
    assertNotNull(turtleExport);
    assertFalse(turtleExport.isBlank());
    assertTrue(
        turtleExport.contains("dcat") || turtleExport.contains("dcterms"),
        "Turtle should contain DCAT/DCTerms predicates");
  }

  @Test
  void t02b_writeSemanticExportFixture() throws Exception {
    assertNotNull(turtleExport, "Turtle export from t02 must exist");
    Files.writeString(FIXTURE_DIR.resolve("roundtrip-semantic-export.ttl"), turtleExport);
  }

  @Test
  void t02c_writeFrameFixture() throws Exception {
    JsonLdFrameGenerator frameGenerator = new JsonLdFrameGenerator();
    JsonNode frame = frameGenerator.generate(source.getMetadata());
    String prettyFrame = PRETTY_MAPPER.writeValueAsString(frame);
    Files.writeString(FIXTURE_DIR.resolve("roundtrip-frame.jsonld"), prettyFrame);
  }

  @Test
  void t03_targetHasDemoDataBefore() {
    Table resources = target.getTable("Resources");
    assertNotNull(resources, "Target should have Resources table from profile");
    List<Row> rows = resources.retrieveRows();
    assertFalse(rows.isEmpty(), "Target should have demo data before harvest");
  }

  @Test
  void t04_harvestImportsData() throws Exception {
    assertNotNull(turtleExport, "Turtle export from t02 must exist");

    DcatHarvestTask harvestTask = new DcatHarvestTask(target, "roundtrip-test", turtleExport);
    HarvestReport report = harvestTask.harvestRdf(turtleExport);

    assertTrue(
        report.getErrors().isEmpty(),
        "Harvest should have no errors, but got: " + report.getErrors());
    assertTrue(report.getResourcesImported() > 0, "Harvest should import at least one resource");
  }

  @Test
  void t05_targetHasDataAfter() {
    Table resources = target.getTable("Resources");
    assertNotNull(resources);
    List<Row> rows = resources.retrieveRows();
    assertFalse(rows.isEmpty(), "Target should have resources after harvest");
  }

  @AfterAll
  static void cleanup() {
    database.dropSchemaIfExists("dcat_roundtrip_source");
    database.dropSchemaIfExists("dcat_roundtrip_target");
  }
}
