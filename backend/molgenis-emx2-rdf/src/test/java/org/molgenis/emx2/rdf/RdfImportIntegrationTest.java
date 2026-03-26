package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskStatus;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RdfImportIntegrationTest {

  private static Schema schema;

  @BeforeAll
  static void setUp() {
    Database db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema("RdfImportIntegrationTest");

    schema.create(
        new TableMetadata("Organisations")
            .setSemantics("foaf:Agent,org:Organization")
            .add(new Column("pid").setPkey())
            .add(new Column("name").setSemantics("http://xmlns.com/foaf/0.1/name"))
            .add(new Column("identifier").setSemantics("http://purl.org/dc/terms/identifier")));

    schema.create(
        new TableMetadata("Contacts")
            .setSemantics("vcard:Individual")
            .add(new Column("pid").setPkey())
            .add(new Column("name").setSemantics("http://www.w3.org/2006/vcard/ns#fn"))
            .add(new Column("email").setSemantics("http://www.w3.org/2006/vcard/ns#hasEmail")));

    schema.create(
        new TableMetadata("Resources")
            .add(new Column("pid").setPkey())
            .add(new Column("title").setSemantics("http://purl.org/dc/terms/title"))
            .add(
                new Column("description")
                    .setType(ColumnType.TEXT)
                    .setSemantics("http://purl.org/dc/terms/description"))
            .add(
                new Column("keywords")
                    .setType(ColumnType.STRING_ARRAY)
                    .setSemantics("http://www.w3.org/ns/dcat#keyword"))
            .add(new Column("type").setType(ColumnType.ONTOLOGY).setRefTable("Resource types"))
            .add(
                new Column("themes")
                    .setType(ColumnType.ONTOLOGY_ARRAY)
                    .setRefTable("Data themes")
                    .setSemantics("http://www.w3.org/ns/dcat#theme"))
            .add(
                new Column("publisher")
                    .setType(ColumnType.REF)
                    .setRefTable("Organisations")
                    .setSemantics("http://purl.org/dc/terms/publisher"))
            .add(
                new Column("contactPoint")
                    .setType(ColumnType.REF)
                    .setRefTable("Contacts")
                    .setSemantics("http://www.w3.org/ns/dcat#contactPoint"))
            .add(new Column("identifier").setSemantics("http://purl.org/dc/terms/identifier"))
            .add(
                new Column("landingPage")
                    .setType(ColumnType.HYPERLINK)
                    .setSemantics("http://www.w3.org/ns/dcat#landingPage")));

    schema
        .getTable("Resource types")
        .insert(
            new Row()
                .setString("name", "Catalogue")
                .setString("ontologyTermURI", "http://www.w3.org/ns/dcat#Catalog"),
            new Row()
                .setString("name", "Cohort study")
                .setString("ontologyTermURI", "http://www.w3.org/ns/dcat#Dataset")
                .setStringArray(
                    "alternativeIds", new String[] {"http://www.w3.org/ns/dcat#DatasetSeries"}));

    schema
        .getTable("Data themes")
        .insert(
            new Row()
                .setString("name", "Health")
                .setString(
                    "ontologyTermURI",
                    "http://publications.europa.eu/resource/authority/data-theme/HEAL"),
            new Row()
                .setString("name", "Society")
                .setStringArray(
                    "alternativeIds",
                    new String[] {
                      "http://publications.europa.eu/resource/authority/data-theme/SOCI"
                    }));
  }

  @Test
  @org.junit.jupiter.api.Order(1)
  void fullRoundTripImportFromFile() {
    InputStream ttl = getClass().getClassLoader().getResourceAsStream("test-dcat-catalog.ttl");
    assertNotNull(ttl, "test-dcat-catalog.ttl must be on the classpath");

    RdfImportTask task = new RdfImportTask(schema, ttl, ".ttl");
    task.run();

    assertEquals(TaskStatus.COMPLETED, task.getStatus(), task.getDescription());

    List<Row> resources = schema.getTable("Resources").retrieveRows();
    assertTrue(resources.size() >= 2, "Expected at least 2 resources (catalog + datasets)");

    List<Row> orgs = schema.getTable("Organisations").retrieveRows();
    assertFalse(orgs.isEmpty(), "Expected at least 1 organisation");

    List<Row> contacts = schema.getTable("Contacts").retrieveRows();
    assertFalse(contacts.isEmpty(), "Expected at least 1 contact");
  }

  @Test
  @org.junit.jupiter.api.Order(2)
  void importedResourcesHaveExpectedData() {
    List<Row> resources = schema.getTable("Resources").retrieveRows();
    assertTrue(
        resources.stream().anyMatch(r -> "Cohort study".equals(r.getString("type"))),
        "Expected at least one resource with type 'Cohort study'");

    assertTrue(
        resources.stream().anyMatch(r -> r.getString("title") != null),
        "Expected resources to have titles");
  }

  @Test
  @org.junit.jupiter.api.Order(3)
  void importedDatasetsHaveCorrectlyMappedThemes() {
    List<Row> resources = schema.getTable("Resources").retrieveRows();

    Row cohort2 =
        resources.stream()
            .filter(r -> "cohort-2".equals(r.getString("pid")))
            .findFirst()
            .orElse(null);
    assertNotNull(cohort2, "Dataset cohort-2 should exist");

    String[] themes = cohort2.getStringArray("themes");
    assertNotNull(themes, "cohort-2 should have themes");

    List<String> themeList = List.of(themes);
    assertTrue(
        themeList.contains("Health"),
        "HEAL URI should resolve to 'Health' via ontologyTermURI, got: " + themeList);
    assertTrue(
        themeList.contains("Society"),
        "SOCI URI should resolve to 'Society' via alternativeIds, got: " + themeList);
  }

  @Test
  @org.junit.jupiter.api.Order(4)
  void importIsIdempotent() {
    int countBefore = schema.getTable("Resources").retrieveRows().size();

    InputStream ttl2 = getClass().getClassLoader().getResourceAsStream("test-dcat-catalog.ttl");
    assertNotNull(ttl2);
    RdfImportTask task2 = new RdfImportTask(schema, ttl2, ".ttl");
    task2.run();

    assertEquals(TaskStatus.COMPLETED, task2.getStatus(), task2.getDescription());

    int countAfter = schema.getTable("Resources").retrieveRows().size();
    assertEquals(countBefore, countAfter, "Second import must not create duplicates");
  }

  @Test
  @org.junit.jupiter.api.Order(5)
  void importEmptyRdfProducesNoError() {
    String emptyTurtle = "@prefix dcat: <http://www.w3.org/ns/dcat#> .\n";
    InputStream is = new ByteArrayInputStream(emptyTurtle.getBytes(StandardCharsets.UTF_8));
    RdfImportTask task = new RdfImportTask(schema, is, ".ttl");
    task.run();
    assertEquals(TaskStatus.COMPLETED, task.getStatus(), "Empty RDF should complete without error");
  }

  @Test
  @org.junit.jupiter.api.Order(6)
  void importRdfWithNoMatchingAnnotationsProducesEmptyResult() {
    Database db = TestDatabaseFactory.getTestDatabase();
    Schema noAnnotations = db.dropCreateSchema("RdfImportNoAnnotations");
    noAnnotations.create(
        new TableMetadata("Things").add(new Column("id").setPkey()).add(new Column("label")));

    InputStream ttl = getClass().getClassLoader().getResourceAsStream("test-dcat-catalog.ttl");
    RdfImportTask task = new RdfImportTask(noAnnotations, ttl, ".ttl");
    task.run();
    assertEquals(
        TaskStatus.COMPLETED,
        task.getStatus(),
        "Import with no matching annotations should complete");
    assertTrue(
        noAnnotations.getTable("Things").retrieveRows().isEmpty(),
        "No rows should be imported when no annotations match");

    db.dropSchema("RdfImportNoAnnotations");
  }

  @Test
  @org.junit.jupiter.api.Order(7)
  void importsCompositePkeyOrganisations() {
    Database compDb = TestDatabaseFactory.getTestDatabase();
    Schema compSchema = compDb.dropCreateSchema("RdfImportCompositePkey");

    compSchema.create(
        new TableMetadata("Resources")
            .setTableType(TableType.DATA)
            .setSemantics("dcat:Dataset")
            .add(new Column("id").setKey(1))
            .add(new Column("pid").setKey(2).setSemantics("http://purl.org/dc/terms/identifier"))
            .add(new Column("name").setSemantics("http://purl.org/dc/terms/title")),
        new TableMetadata("Organisations")
            .setTableType(TableType.DATA)
            .setSemantics("foaf:Agent,org:Organization")
            .add(new Column("resource").setType(ColumnType.REF).setRefTable("Resources").setKey(1))
            .add(new Column("id").setKey(1))
            .add(new Column("organisation name").setSemantics("http://xmlns.com/foaf/0.1/name"))
            .add(
                new Column("organisation pid")
                    .setSemantics("http://purl.org/dc/terms/identifier")));

    String ttl =
        "@prefix dcat: <http://www.w3.org/ns/dcat#> .\n"
            + "@prefix dcterms: <http://purl.org/dc/terms/> .\n"
            + "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n"
            + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
            + "\n"
            + "<http://ex.org/api/rdf/Resources/id=res1> rdf:type dcat:Dataset ;\n"
            + "    dcterms:title \"Test Resource\" ;\n"
            + "    dcterms:identifier \"res1\" .\n"
            + "\n"
            + "<http://ex.org/api/rdf/Organisations/id=UMCG&resource=res1> rdf:type foaf:Agent ;\n"
            + "    foaf:name \"UMCG\" ;\n"
            + "    dcterms:identifier \"https://ror.org/umcg\" .\n";

    InputStream ttlStream = new ByteArrayInputStream(ttl.getBytes(StandardCharsets.UTF_8));
    RdfImportTask task = new RdfImportTask(compSchema, ttlStream, ".ttl");
    task.run();

    assertEquals(TaskStatus.COMPLETED, task.getStatus(), task.getDescription());

    List<Row> organisations = compSchema.getTable("Organisations").retrieveRows();
    assertFalse(
        organisations.isEmpty(), "Expected at least 1 organisation after composite pkey roundtrip");
    assertEquals("UMCG", organisations.get(0).getString("id"));

    compDb.dropSchema("RdfImportCompositePkey");
  }

  @Test
  @org.junit.jupiter.api.Order(8)
  void importMalformedRdfProducesError() {
    String malformed = "this is not valid RDF at all";
    InputStream is = new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8));
    RdfImportTask task = new RdfImportTask(schema, is, ".ttl");
    assertThrows(org.molgenis.emx2.MolgenisException.class, task::run);
  }
}
