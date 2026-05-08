package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Constants.ANONYMOUS;
import static org.molgenis.emx2.Constants.MOLGENIS_ADMIN_PW;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_PW_DEFAULT;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.utils.EnvironmentProperty;

/**
 * Verifies role-based access control on {@link JsonYamlApi}. The handlers don't perform an explicit
 * {@code PermissionEvaluator} check; protection comes from the transactional DB role enforcement
 * inside {@code schema.migrate}/{@code schema.discard}. These tests pin that down at the HTTP
 * boundary so a regression there doesn't pass silently.
 */
@Tag("slow")
class JsonYamlApiAuthorizationTest extends ApiTestBase {

  private static final String SCHEMA = "pet store jsonyaml auth";
  private static final String VIEWER = "jsonyamlauth_viewer";
  private static final String MANAGER = "jsonyamlauth_manager";

  private static final String ADMIN_PASS =
      (String)
          EnvironmentProperty.getParameter(MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, ColumnType.STRING);

  private static final String EMPTY_SCHEMA_JSON = "{\"name\":\"" + SCHEMA + "\",\"tables\":[]}";
  private static final String EMPTY_SCHEMA_YAML = "name: \"" + SCHEMA + "\"\ntables: []\n";

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

  // --- /api/json ---

  @Test
  void postJsonAsManager_succeeds() {
    // round-trip the live schema dump because an empty-tables payload isn't a meaningful migrate
    login(database.getAdminUserName(), ADMIN_PASS);
    String schemaJson =
        given().sessionId(sessionId).when().get("/" + SCHEMA + "/api/json").asString();

    login(MANAGER, MANAGER);
    Response response = postJson("/" + SCHEMA + "/api/json", schemaJson);
    assertEquals(200, response.getStatusCode());
  }

  @Test
  void postJsonAsViewer_isRejected() {
    login(VIEWER, VIEWER);
    assertEquals(400, postJson("/" + SCHEMA + "/api/json", EMPTY_SCHEMA_JSON).getStatusCode());
  }

  @Test
  void postJsonAnonymous_isRejected() {
    Response response =
        given()
            .contentType("application/json")
            .body(EMPTY_SCHEMA_JSON)
            .when()
            .post("/" + SCHEMA + "/api/json");
    assertEquals(400, response.getStatusCode());
  }

  @Test
  void deleteJsonAsViewer_isRejected() {
    login(VIEWER, VIEWER);
    Response response =
        given()
            .sessionId(sessionId)
            .contentType("application/json")
            .body(EMPTY_SCHEMA_JSON)
            .when()
            .delete("/" + SCHEMA + "/api/json");
    assertEquals(400, response.getStatusCode());
  }

  // --- /api/yaml: RestAssured has no encoder for application/x-yaml, so we send the raw body
  //     without a content type (matches WebApiSmokeTests.testJsonYamlApi).

  @Test
  void postYamlAsViewer_isRejected() {
    login(VIEWER, VIEWER);
    Response response =
        given()
            .sessionId(sessionId)
            .body(EMPTY_SCHEMA_YAML)
            .when()
            .post("/" + SCHEMA + "/api/yaml");
    assertEquals(400, response.getStatusCode());
  }

  private static Response postJson(String path, String body) {
    return given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body(body)
        .when()
        .post(path);
  }
}
