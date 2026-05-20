package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.molgenis.emx2.Constants.ANONYMOUS;

import org.junit.jupiter.api.*;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;

class MolgenisWebserviceTest extends ApiTestBase {

  @BeforeAll
  static void setup() {
    database.setUserPassword("foo", "testtest");
  }

  @BeforeEach
  void login() {
    login("foo", "testtest");
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
    database.dropSchema(schema.getName());
  }

  @Test
  void givenSchema_whenNoMenuForRole_thenRedirectToRoot() {
    Schema schema = setupSchema(getClass().getSimpleName() + "no-match");
    database.addUser("testAnonymous");
    database.setUserPassword("testAnonymous", "testtest");
    schema.getMetadata().setSetting("menu", menuForRole(Privileges.VIEWER.toString()));
    login("testAnonymous", "testtest");
    given()
        .redirects()
        .follow(false)
        .sessionId(sessionId)
        .when()
        .get("/" + schema.getName() + "/")
        .then()
        .header("Location", "/");
    database.dropSchema(schema.getName());
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
    database.dropSchema(schema.getName());
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
    database.dropSchema(schema.getName());
  }

  private static Schema setupSchema(String schemaName) {
    database.dropCreateSchema(schemaName);
    Schema schema = database.getSchema(schemaName);
    schema.addMember("foo", Privileges.VIEWER.toString());
    return schema;
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
