package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_PW_DEFAULT;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.EnvironmentProperty;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RdfImportApiTest extends ApiTestBase {

  private static final String SCHEMA_SOURCE = "rdf_import_source";
  private static final String SCHEMA_TARGET = "rdf_import_target";
  private static final String SCHEMA_FILE = "rdf_import_file";
  private static final String ADMIN_PASS =
      (String)
          EnvironmentProperty.getParameter(MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, ColumnType.STRING);

  @BeforeAll
  static void setup() {
    login(database.getAdminUserName(), ADMIN_PASS);
    setupSchemas();
  }

  private static void setupSchemas() {
    for (String schemaName : List.of(SCHEMA_SOURCE, SCHEMA_TARGET, SCHEMA_FILE)) {
      database.dropSchemaIfExists(schemaName);
      Schema schema = database.dropCreateSchema(schemaName);
      createCatalogueStructure(schema);
    }
    Schema source = database.getSchema(SCHEMA_SOURCE);
    source.addMember(ANONYMOUS, Privileges.VIEWER.toString());
    addDemoData(source);
  }

  private static void createCatalogueStructure(Schema schema) {
    schema.create(
        new TableMetadata("Organisations")
            .setSemantics("http://xmlns.com/foaf/0.1/Agent")
            .add(new Column("pid").setPkey())
            .add(new Column("name").setSemantics("http://xmlns.com/foaf/0.1/name"))
            .add(new Column("identifier").setSemantics("http://purl.org/dc/terms/identifier")));

    schema.create(
        new TableMetadata("Contacts")
            .setSemantics("http://www.w3.org/2006/vcard/ns#Individual")
            .add(new Column("pid").setPkey())
            .add(new Column("name").setSemantics("http://www.w3.org/2006/vcard/ns#fn"))
            .add(new Column("email").setSemantics("http://www.w3.org/2006/vcard/ns#hasEmail")));

    schema.create(
        new TableMetadata("Resources")
            .setSemantics("http://www.w3.org/ns/dcat#Dataset")
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
            new Row().setString("name", "Catalogue"), new Row().setString("name", "Cohort study"));

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

  private static void addDemoData(Schema schema) {
    schema
        .getTable("Organisations")
        .insert(new Row().setString("pid", "org-1").setString("name", "Test Research Institute"));
    schema
        .getTable("Contacts")
        .insert(new Row().setString("pid", "contact-1").setString("name", "John Doe"));
    schema
        .getTable("Resources")
        .insert(
            new Row()
                .setString("pid", "ds-1")
                .setString("title", "Test Dataset")
                .setString("description", "A test dataset for roundtrip")
                .setString("identifier", "ds-1")
                .setString("type", "Cohort study")
                .setString("publisher", "org-1")
                .setString("contactPoint", "contact-1"));
  }

  @Test
  @Order(1)
  void importRdfFile() throws Exception {
    File ttlFile =
        new File(getClass().getClassLoader().getResource("test-dcat-catalog.ttl").getFile());

    String response =
        given()
            .sessionId(sessionId)
            .multiPart("file", ttlFile, "text/turtle")
            .when()
            .post("/" + SCHEMA_FILE + "/api/rdf/import/file")
            .asString();

    Map<String, String> task = new ObjectMapper().readValue(response, Map.class);
    assertNotNull(task.get("id"), "Expected task ID in response");
    assertNotNull(task.get("url"), "Expected task URL in response");

    waitForTask(task.get("url"));

    Schema schema = database.getSchema(SCHEMA_FILE);
    List<Row> resources = schema.getTable("Resources").retrieveRows();
    assertTrue(resources.size() >= 2, "Expected at least 2 resources after file import");

    List<Row> orgs = schema.getTable("Organisations").retrieveRows();
    assertFalse(orgs.isEmpty(), "Expected organisations after file import");
  }

  @Test
  @Order(2)
  void roundtripExportThenHarvest() throws Exception {
    Schema source = database.getSchema(SCHEMA_SOURCE);
    assertFalse(
        source.getTable("Resources").retrieveRows().isEmpty(), "Source schema should have data");

    String exportUrl = "http://localhost:" + PORT + "/" + SCHEMA_SOURCE + "/api/ttl";

    String response =
        given()
            .sessionId(sessionId)
            .contentType("application/json")
            .body("{\"url\": \"" + exportUrl + "\"}")
            .when()
            .post("/" + SCHEMA_TARGET + "/api/rdf/import")
            .asString();

    Map<String, String> task = new ObjectMapper().readValue(response, Map.class);
    assertNotNull(task.get("id"), "Expected task ID in response");

    waitForTask(task.get("url"));

    Schema target = database.getSchema(SCHEMA_TARGET);
    List<Row> targetResources = target.getTable("Resources").retrieveRows();
    assertTrue(
        targetResources.size() >= 1,
        "Target should have at least 1 resource after roundtrip harvest");

    boolean hasTitle =
        targetResources.stream()
            .anyMatch(r -> r.getString("title") != null && !r.getString("title").isEmpty());
    assertTrue(hasTitle, "Imported resources should have titles");
  }

  @Test
  @Order(3)
  void verifyOntologyTermMappingAfterFileImport() {
    Schema schema = database.getSchema(SCHEMA_FILE);
    List<Row> resources = schema.getTable("Resources").retrieveRows();

    boolean healthFound = false;
    boolean societyFound = false;
    for (Row r : resources) {
      String[] themes = r.getStringArray("themes");
      if (themes != null) {
        for (String theme : themes) {
          if ("Health".equals(theme)) healthFound = true;
          if ("Society".equals(theme)) societyFound = true;
        }
      }
    }
    assertTrue(healthFound, "HEAL URI should resolve to 'Health' via ontologyTermURI");
    assertTrue(societyFound, "SOCI URI should resolve to 'Society' via alternativeIds");
  }

  private void waitForTask(String taskUrl) throws Exception {
    int count = 0;
    Response poll = given().sessionId(sessionId).when().get(taskUrl);
    while (poll.body().asString().contains("UNKNOWN")
        || poll.body().asString().contains("RUNNING")
        || poll.body().asString().contains("WAITING")) {
      if (count++ > 100) {
        fail("Task polling timed out. Last response: " + poll.body().asString());
      }
      Thread.sleep(500);
      poll = given().sessionId(sessionId).when().get(taskUrl);
    }
    assertFalse(
        poll.body().asString().contains("ERROR"),
        "Task should not have errors: " + poll.body().asString());
  }

  @AfterAll
  static void cleanup() {
    database.dropSchemaIfExists(SCHEMA_SOURCE);
    database.dropSchemaIfExists(SCHEMA_TARGET);
    database.dropSchemaIfExists(SCHEMA_FILE);
  }
}
