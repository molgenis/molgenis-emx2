package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.datamodels.YamlWorkspaceLoader;

class TemplatesApiTest extends ApiTestBase {

  private static final String CLASSIC_TEMPLATE = "PET_STORE";
  private static final String YAML_TEMPLATE = "catalogue";
  private static final String CREATED_SCHEMA = "TemplatesApiTestCatalogue";

  @BeforeAll
  static void setup() {
    login("admin", "admin");
  }

  private static Response getTemplates() {
    return given().sessionId(sessionId).when().get("/api/templates");
  }

  @Test
  void endpointMergesClassicAndYamlTemplates() {
    Response response = getTemplates();
    response.then().statusCode(200);

    List<String> classicNames =
        response.jsonPath().getList("findAll { it.source == 'classic' }.name");
    List<String> yamlNames = response.jsonPath().getList("findAll { it.source == 'yaml' }.name");

    assertTrue(
        classicNames.contains(CLASSIC_TEMPLATE),
        "merged list must contain the classic enum template " + CLASSIC_TEMPLATE);
    assertTrue(
        yamlNames.contains(YAML_TEMPLATE),
        "merged list must contain the discovered yaml workspace template " + YAML_TEMPLATE);

    Boolean hasDemoData =
        response
            .jsonPath()
            .getBoolean(
                "find { it.source == 'yaml' && it.name == '" + YAML_TEMPLATE + "' }.hasDemoData");
    assertTrue(hasDemoData, "the catalogue yaml template carries demo: data");
  }

  @Test
  void templateDetailReturnsWovenSingleFileWireForm() {
    Response response = given().sessionId(sessionId).when().get("/api/templates/" + YAML_TEMPLATE);
    response.then().statusCode(200);
    response.then().contentType("text/yaml");

    String body = response.body().asString();
    assertTrue(body.contains("formatVersion"), "wire form must carry the bundle formatVersion key");
    assertTrue(
        body.contains("Collections"),
        "table files must be woven in (root file only references file paths)");
    assertTrue(
        body.contains("notes"),
        "the imported 'notes' column (from shared/AuditColumns.yaml) proves imports were resolved");
    assertTrue(
        body.contains("additionalSchemas"),
        "companion schemas must be inlined under additionalSchemas");
    assertTrue(
        body.contains("Countries"),
        "the CatalogueOntologies companion's Countries table proves the companion was woven inline, not just referenced");
    assertTrue(
        body.contains("catalogue/data"),
        "the complete bundle keeps the data: relative path (CSV rows are not inlined)");
    assertTrue(
        body.contains("catalogue/demo"),
        "the complete bundle keeps the demo: relative path (CSV rows are not inlined)");
  }

  @Test
  void templateListEntriesCarryUrlForYamlAndOmitForClassic() {
    Response response = getTemplates();
    response.then().statusCode(200);

    String yamlUrl =
        response
            .jsonPath()
            .getString("find { it.source == 'yaml' && it.name == '" + YAML_TEMPLATE + "' }.url");
    assertEquals(
        "/api/templates/" + YAML_TEMPLATE,
        yamlUrl,
        "yaml entries carry the url where the full template is served");

    String classicUrl =
        response
            .jsonPath()
            .getString(
                "find { it.source == 'classic' && it.name == '" + CLASSIC_TEMPLATE + "' }.url");
    assertNull(classicUrl, "classic entries have no yaml body, so no url");
    assertFalse(
        response.body().asString().contains("\"url\":null"),
        "classic url is omitted, not serialized as null noise");
  }

  @Test
  void templateDetailUnknownNameReturns404() {
    given().sessionId(sessionId).when().get("/api/templates/does-not-exist").then().statusCode(404);
  }

  @Test
  void templateDetailClassicEnumNameReturns404() {
    given()
        .sessionId(sessionId)
        .when()
        .get("/api/templates/" + CLASSIC_TEMPLATE)
        .then()
        .statusCode(404);
  }

  @Test
  void yamlTemplateIsCreatableByTheReturnedName() {
    Response response = getTemplates();
    response.then().statusCode(200);
    List<String> yamlNames = response.jsonPath().getList("findAll { it.source == 'yaml' }.name");
    assertTrue(yamlNames.contains(YAML_TEMPLATE), "endpoint must list the catalogue yaml template");

    database.dropSchemaIfExists(CREATED_SCHEMA);
    new YamlWorkspaceLoader().create(database, YAML_TEMPLATE, CREATED_SCHEMA, false);

    assertNotNull(
        database.getSchema(CREATED_SCHEMA).getTable("Collections"),
        "creating by the name the endpoint returns yields the catalogue schema");
  }
}
