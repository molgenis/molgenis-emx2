package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
