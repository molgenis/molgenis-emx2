package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.molgenis.emx2.Constants.ANONYMOUS;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.RunMolgenisEmx2;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
class MolgenisWebserviceTest {

  private static String sessionId;
  private static Database db;
  private static final int PORT = 8081; // other than default so we can see effect

  @BeforeAll
  static void setupService() throws Exception {
    db = TestDatabaseFactory.getTestDatabase();

    // start web service for testing, including env variables
    new EnvironmentVariables(
            org.molgenis.emx2.Constants.MOLGENIS_METRICS_ENABLED, Boolean.TRUE.toString())
        .execute(
            () -> {
              RunMolgenisEmx2.main(new String[] {String.valueOf(PORT)});
            });

    // set default rest assured settings
    RestAssured.port = PORT;
    RestAssured.baseURI = "http://localhost";

    db.setUserPassword("foo", "testtest");
    setupSession();
  }

  private static Schema setupSchema(String schemaName) {
    db.dropCreateSchema(schemaName);
    Schema schema = db.getSchema(schemaName);
    schema.addMember("foo", Privileges.VIEWER.toString());
    return schema;
  }

  private static void setupSession() {
    sessionId =
        given()
            .body(
                "{\"query\":\"mutation{signin("
                    + "email:\\\"foo\\\","
                    + "password:\\\"testtest\\\""
                    + "){message}}\"}")
            .when()
            .post("api/graphql")
            .sessionId();
  }

  @Test
  void givenSchema_whenNoMenu_thenRedirectToTables() {
    Schema schema = setupSchema(getClass().getSimpleName() + "no-menu");
    given()
        .redirects()
        .follow(false)
        .sessionId(sessionId)
        .when()
        .get("/" + schema.getName() + "/")
        .then()
        .header("Location", "/" + schema.getName() + "/tables");
    db.dropSchema(schema.getName());
  }

  @Test
  void givenSchema_whenNoMenuForRole_thenRedirectToRoot() {
    Schema schema = setupSchema(getClass().getSimpleName() + "no-match");
    schema.getMetadata().setSetting("menu", menuForRole(Privileges.EDITOR.toString()));
    given()
        .redirects()
        .follow(false)
        .sessionId(sessionId)
        .when()
        .get("/" + schema.getName() + "/")
        .then()
        .header("Location", "/");
    db.dropSchema(schema.getName());
  }

  @Test
  void givenSchema_whenMenuForRole_thenRedirectToFirstItem() {
    Schema schema = setupSchema(getClass().getSimpleName() + "first-item");
    schema.getMetadata().setSetting("menu", menuForRole(Privileges.VIEWER.toString()));
    given()
        .redirects()
        .follow(false)
        .sessionId(sessionId)
        .when()
        .get("/" + schema.getName() + "/")
        .then()
        .header("Location", "/" + schema.getName() + "/from-menu");
    db.dropSchema(schema.getName());
  }

  @Test
  void givenSchemaWithAnonymousUser_whenInsufficientRoleForMenu_thenRedirectToTables() {
    Schema schema = setupSchema(getClass().getSimpleName() + "anonymous");
    schema.addMember(ANONYMOUS, Privileges.VIEWER.toString());
    schema.getMetadata().setSetting("menu", menuForRole(Privileges.EDITOR.toString()));
    given()
        .redirects()
        .follow(false)
        .sessionId(sessionId)
        .when()
        .get("/" + schema.getName() + "/")
        .then()
        .header("Location", "/" + schema.getName() + "/tables");
    db.dropSchema(schema.getName());
  }

  private String menuForRole(String role) {
    return """
          [
            {
              "label": "Tables",
              "href": "from-menu",
              "role": "%s",
              "key": "y0768",
              "submenu": []
            }
          ]
          """
        .formatted(role);
  }
}
