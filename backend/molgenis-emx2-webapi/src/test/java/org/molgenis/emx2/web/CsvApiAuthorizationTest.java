package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Constants.ANONYMOUS;
import static org.molgenis.emx2.Constants.IS_CHANGELOG_ENABLED;
import static org.molgenis.emx2.Constants.MOLGENIS_ADMIN_PW;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_PW_DEFAULT;
import static org.molgenis.emx2.web.Constants.ACCEPT_CSV;

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
 * Verifies role-based access control on {@link CsvApi}.
 */
@Tag("slow")
class CsvApiAuthorizationTest extends ApiTestBase {

  private static final String SCHEMA = "pet store csv auth";
  private static final String VIEWER = "csvauth_viewer";
  private static final String MANAGER = "csvauth_manager";

  private static final String ADMIN_PASS =
      (String)
          EnvironmentProperty.getParameter(MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, ColumnType.STRING);

  @BeforeAll
  static void setup() {
    login(database.getAdminUserName(), ADMIN_PASS);
    database.dropSchemaIfExists(SCHEMA);
    PET_STORE.getImportTask(database, SCHEMA, "", true).run();
    Schema schema = database.getSchema(SCHEMA);
    schema.getMetadata().setSetting(IS_CHANGELOG_ENABLED, "true");

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

  @Test
  void changelogAsAdmin_succeeds() {
    login(database.getAdminUserName(), ADMIN_PASS);
    assertEquals(200, getCsv("/" + SCHEMA + "/api/csv/changelog").getStatusCode());
  }

  @Test
  void changelogAsManager_succeeds() {
    login(MANAGER, MANAGER);
    assertEquals(200, getCsv("/" + SCHEMA + "/api/csv/changelog").getStatusCode());
  }

  @Test
  void changelogAsViewer_isRejected() {
    login(VIEWER, VIEWER);
    Response response = getCsv("/" + SCHEMA + "/api/csv/changelog");
    assertEquals(400, response.getStatusCode());
    assertTrue(
        response.body().asString().contains("Unauthorized"),
        () -> "expected 'Unauthorized' in body, got: " + response.body().asString());
  }

  @Test
  void changelogAnonymous_isRejected() {
    Response response = given().accept(ACCEPT_CSV).when().get("/" + SCHEMA + "/api/csv/changelog");
    assertEquals(400, response.getStatusCode());
    assertTrue(response.body().asString().contains("Unauthorized"));
  }

  // --- /api/csv/members: handler does explicit canManage ---

  @Test
  void membersAsManager_succeeds() {
    login(MANAGER, MANAGER);
    assertEquals(200, getCsv("/" + SCHEMA + "/api/csv/members").getStatusCode());
  }

  @Test
  void membersAsViewer_isRejected() {
    login(VIEWER, VIEWER);
    Response response = getCsv("/" + SCHEMA + "/api/csv/members");
    assertEquals(400, response.getStatusCode());
    assertTrue(response.body().asString().contains("Unauthorized"));
  }

  private static Response getCsv(String path) {
    return given().sessionId(sessionId).accept(ACCEPT_CSV).when().get(path);
  }
}
