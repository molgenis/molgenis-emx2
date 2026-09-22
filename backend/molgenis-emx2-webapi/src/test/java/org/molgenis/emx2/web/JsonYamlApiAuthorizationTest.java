package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Constants.ANONYMOUS;
import static org.molgenis.emx2.Constants.MOLGENIS_ADMIN_PW;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_PW_DEFAULT;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.utils.EnvironmentProperty;

@Tag("slow")
class JsonYamlApiAuthorizationTest extends ApiTestBase {

  private static final String SCHEMA = JsonYamlApiAuthorizationTest.class.getSimpleName();
  private static final String VIEWER = "jsonyamlauth_viewer";
  private static final String MANAGER = "jsonyamlauth_manager";

  private static final String ADMIN_PASS =
      (String)
          EnvironmentProperty.getParameter(MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, ColumnType.STRING);

  private static final String EMPTY_SCHEMA = "{ }";
  private static final String NON_EXISTING_SCHEMA = "nonexisting";

  @BeforeAll
  static void setup() {
    login(database.getAdminUserName(), ADMIN_PASS);
    database.dropSchemaIfExists(SCHEMA);
    PET_STORE.getImportTask(database, SCHEMA, "", true).run();
    Schema schema = database.getSchema(SCHEMA);

    database.setUserPassword(VIEWER, VIEWER);
    database.setUserPassword(MANAGER, MANAGER);
    schema.addMember(VIEWER, Privileges.VIEWER.toString());
    schema.addMember(MANAGER, Privileges.MANAGER.toString());
    schema.addMember(ANONYMOUS, Privileges.VIEWER.toString());
  }

  @AfterAll
  static void teardown() {
    database.becomeAdmin();
    database.dropSchemaIfExists(SCHEMA);
  }

  @Nested
  class JsonTest {

    @Test
    void getJsonAsViewer_succeeds() {
      login(VIEWER, VIEWER);
      Response response = given().sessionId(sessionId).when().get("/" + SCHEMA + "/api/json");
      assertEquals(200, response.getStatusCode());
    }

    @Test
    void getJsonAnonymous_succeeds() {
      Response response = given().when().get("/" + SCHEMA + "/api/json");
      assertEquals(200, response.getStatusCode());
    }

    @Test
    void postJsonAsManager_succeeds() {
      // round-trip the live schema dump because an empty-tables payload isn't a meaningful migrate
      login(database.getAdminUserName(), ADMIN_PASS);
      String schemaJson =
          given().sessionId(sessionId).when().get("/" + SCHEMA + "/api/json").asString();

      login(MANAGER, MANAGER);
      Response response = postJson("/" + SCHEMA + "/api/json", schemaJson);
      assertEquals(200, response.getStatusCode());
      assertEquals(
          "{ \"message\": \"add/update metadata success\" }", response.getBody().asString());
    }

    @Test
    void postJsonAsViewer_isRejected() {
      login(VIEWER, VIEWER);
      Response response = postJson("/" + SCHEMA + "/api/json", EMPTY_SCHEMA);
      assertEquals(400, response.getStatusCode());
      assertEquals(
          errorMessage("Schema not found or insufficient access"), response.getBody().asString());
    }

    @Test
    void deleteJsonAsManager_succeeds() {
      login(MANAGER, MANAGER);
      Response response = deleteJson("/" + SCHEMA + "/api/json", EMPTY_SCHEMA);
      assertEquals(200, response.getStatusCode());
      assertEquals(
          "{ \"message\": \"removed metadata items success\" }", response.getBody().asString());
    }

    @Test
    void deleteJsonAsViewer_isRejected() {
      login(VIEWER, VIEWER);
      Response response = deleteJson("/" + SCHEMA + "/api/json", EMPTY_SCHEMA);
      assertEquals(400, response.getStatusCode());
      assertEquals(
          errorMessage("Schema not found or insufficient access"), response.getBody().asString());
    }
  }

  @Nested
  class YamlTest {

    @Test
    void getYamlAsViewer_succeeds() {
      login(VIEWER, VIEWER);
      Response response = given().sessionId(sessionId).when().get("/" + SCHEMA + "/api/yaml");
      assertEquals(200, response.getStatusCode());
    }

    @Test
    void getYamlAnonymous_succeeds() {
      Response response = given().when().get("/" + SCHEMA + "/api/yaml");
      assertEquals(200, response.getStatusCode());
    }

    @Test
    void postYamlAsManager_succeeds() {
      // round-trip the live schema dump because an empty-tables payload isn't a meaningful migrate
      login(database.getAdminUserName(), ADMIN_PASS);
      String schemaYaml =
          given().sessionId(sessionId).when().get("/" + SCHEMA + "/api/yaml").asString();

      login(MANAGER, MANAGER);
      Response response = postYaml("/" + SCHEMA + "/api/yaml", schemaYaml);
      assertEquals(200, response.getStatusCode());
      assertEquals(
          "{ \"message\": \"add/update metadata success\" }", response.getBody().asString());
    }

    @Test
    void postYamlAsViewer_isRejected() {
      login(VIEWER, VIEWER);
      Response response = postYaml("/" + SCHEMA + "/api/yaml", EMPTY_SCHEMA);
      assertEquals(400, response.getStatusCode());
      assertEquals(
          errorMessage("Schema not found or insufficient access"), response.getBody().asString());
    }

    @Test
    void deleteYamlAsManager_succeeds() {
      login(MANAGER, MANAGER);
      Response response = deleteYaml("/" + SCHEMA + "/api/yaml", EMPTY_SCHEMA);
      assertEquals(200, response.getStatusCode());
      assertEquals("{ \"message\": \"remove metadata success\" }", response.getBody().asString());
    }

    @Test
    void deleteYamlAsViewer_isRejected() {
      login(VIEWER, VIEWER);
      Response response = deleteYaml("/" + SCHEMA + "/api/yaml", EMPTY_SCHEMA);
      assertEquals(400, response.getStatusCode());
      assertEquals(
          errorMessage("Schema not found or insufficient access"), response.getBody().asString());
    }
  }

  @Nested
  class NullSchemaTest {

    @Test
    void getJsonNullSchema_isRejected() {
      login(MANAGER, MANAGER);
      Response response =
          given().sessionId(sessionId).when().get("/" + NON_EXISTING_SCHEMA + "/api/json");
      assertEquals(400, response.getStatusCode());
      assertEquals(
          errorMessage("Schema not found or insufficient access"), response.getBody().asString());
    }

    @Test
    void postJsonNullSchema_isRejected() {
      login(MANAGER, MANAGER);
      Response response = postJson("/" + NON_EXISTING_SCHEMA + "/api/json", EMPTY_SCHEMA);
      assertEquals(400, response.getStatusCode());
      assertEquals(
          errorMessage("Schema not found or insufficient access"), response.getBody().asString());
    }

    @Test
    void deleteJsonNullSchema_isRejected() {
      login(MANAGER, MANAGER);
      Response response = deleteJson("/" + NON_EXISTING_SCHEMA + "/api/json", EMPTY_SCHEMA);
      assertEquals(400, response.getStatusCode());
      assertEquals(
          errorMessage("Schema not found or insufficient access"), response.getBody().asString());
    }

    @Test
    void getYamlNullSchema_isRejected() {
      login(MANAGER, MANAGER);
      Response response =
          given().sessionId(sessionId).when().get("/" + NON_EXISTING_SCHEMA + "/api/yaml");
      assertEquals(400, response.getStatusCode());
      assertEquals(
          errorMessage("Schema not found or insufficient access"), response.getBody().asString());
    }

    @Test
    void postYamlNullSchema_isRejected() {
      login(MANAGER, MANAGER);
      Response response = postYaml("/" + NON_EXISTING_SCHEMA + "/api/yaml", EMPTY_SCHEMA);
      assertEquals(400, response.getStatusCode());
      assertEquals(
          errorMessage("Schema not found or insufficient access"), response.getBody().asString());
    }

    @Test
    void deleteYamlNullSchema_isRejected() {
      login(MANAGER, MANAGER);
      Response response = deleteYaml("/" + NON_EXISTING_SCHEMA + "/api/yaml", EMPTY_SCHEMA);
      assertEquals(400, response.getStatusCode());
      assertEquals(
          errorMessage("Schema not found or insufficient access"), response.getBody().asString());
    }
  }

  private static Response postJson(String path, String body) {
    return given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body(body)
        .when()
        .post(path);
  }

  private static Response deleteJson(String path, String body) {
    return given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body(body)
        .when()
        .delete(path);
  }

  private static Response postYaml(String path, String body) {
    return given().sessionId(sessionId).body(body).when().post(path);
  }

  private static Response deleteYaml(String path, String body) {
    return given().sessionId(sessionId).body(body).when().delete(path);
  }

  private static String errorMessage(String message) {
    return """
      {
        "errors" : [
          {
            "message" : "%s"
          }
        ]
      }"""
        .formatted(message);
  }
}
