package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.molgenis.emx2.Constants.MOLGENIS_CONTEXT_PATH;
import static org.molgenis.emx2.Constants.MOLGENIS_METRICS_ENABLED;

import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

/**
 * Integration tests verifying that redirect Location headers include the context path prefix when
 * {@code MOLGENIS_CONTEXT_PATH} is set.
 *
 * <p>Mirrors {@link MolgenisWebserviceTest} scenarios but with {@code contextPath=/molgenis},
 * asserting that every generated redirect is prefixed accordingly.
 */
class MolgenisWebserviceContextPathTest {

  private static final int PORT = 8082;
  private static final String CONTEXT_PATH = "/molgenis";

  private static String sessionId;
  private static Database database;
  private static MolgenisWebservice service;

  @BeforeAll
  static void setupService() throws Exception {
    database = TestDatabaseFactory.getTestDatabase();
    database.setUserPassword("foo", "testtest");

    new EnvironmentVariables(
            MOLGENIS_METRICS_ENABLED, Boolean.TRUE.toString(),
            MOLGENIS_CONTEXT_PATH, CONTEXT_PATH)
        .execute(
            () -> {
              service = new MolgenisWebservice();
              service.start(PORT);
            });

    RestAssured.port = PORT;
    RestAssured.baseURI = "http://localhost";

    sessionId =
        given()
            .body(
                """
                {
                  "query":"mutation{signin(email:\\"foo\\",password:\\"testtest\\"){message}}"
                }
                """)
            .when()
            .post(CONTEXT_PATH + "/api/graphql")
            .sessionId();
  }

  @AfterAll
  static void tearDownService() {
    service.stop();
  }

  @Test
  void givenContextPath_whenGetRoot_thenRedirectIncludesContextPath() {
    given()
        .redirects()
        .follow(false)
        .when()
        .get(CONTEXT_PATH + "/")
        .then()
        .statusCode(302)
        .header("Location", CONTEXT_PATH + "/apps/central/");
  }

  @Test
  void givenContextPath_whenSchemaHasNoMenu_thenRedirectToTablesWithContextPath() {
    Schema schema = setupSchema(getClass().getSimpleName() + "no-menu");
    given()
        .redirects()
        .follow(false)
        .sessionId(sessionId)
        .when()
        .get(CONTEXT_PATH + "/" + schema.getName() + "/")
        .then()
        .header("Location", CONTEXT_PATH + "/" + schema.getName() + "/tables");
    database.dropSchema(schema.getName());
  }

  @Test
  void givenContextPath_whenSchemaHasNoMenuForRole_thenRedirectToRootWithContextPath() {
    Schema schema = setupSchema(getClass().getSimpleName() + "no-match");
    schema.getMetadata().setSetting("menu", menuForRole(Privileges.EDITOR.toString()));
    given()
        .redirects()
        .follow(false)
        .sessionId(sessionId)
        .when()
        .get(CONTEXT_PATH + "/" + schema.getName() + "/")
        .then()
        .header("Location", CONTEXT_PATH + "/");
    database.dropSchema(schema.getName());
  }

  @Test
  void givenContextPath_whenSchemaHasMenuForRole_thenRedirectToFirstItemWithContextPath() {
    Schema schema = setupSchema(getClass().getSimpleName() + "first-item");
    schema.getMetadata().setSetting("menu", menuForRole(Privileges.VIEWER.toString()));
    given()
        .redirects()
        .follow(false)
        .sessionId(sessionId)
        .when()
        .get(CONTEXT_PATH + "/" + schema.getName() + "/")
        .then()
        .header("Location", CONTEXT_PATH + "/" + schema.getName() + "/from-menu");
    database.dropSchema(schema.getName());
  }

  @Test
  void givenContextPath_whenGetCentralApp_thenServesHtml() {
    given()
        .when()
        .get(CONTEXT_PATH + "/apps/central/")
        .then()
        .statusCode(200)
        .contentType(containsString("text/html"));
  }

  @Test
  void givenContextPath_whenPostApiGraphql_thenReturns200() {
    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body("{\"query\":\"{_schemas{id}}\"}")
        .when()
        .post(CONTEXT_PATH + "/api/graphql")
        .then()
        .statusCode(200)
        .body("data._schemas", not(nullValue()));
  }

  @Test
  void givenContextPath_whenPostAppsGraphql_thenSettingsNotNull() {
    // central app makes a relative request to "graphql" which resolves to apps/central/graphql
    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body(
            "{\"query\":\"{_settings(keys: [\\\"menu\\\"]){key value}}\"}")
        .when()
        .post(CONTEXT_PATH + "/apps/central/graphql")
        .then()
        .statusCode(200)
        .body("data._settings", not(nullValue()));
  }

  @Test
  void givenContextPath_whenPostSchemaGraphql_thenReturns200() {
    Schema schema = setupSchema(getClass().getSimpleName() + "gql");
    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body("{\"query\":\"{_tables{name}}\"}")
        .when()
        .post(CONTEXT_PATH + "/" + schema.getName() + "/graphql")
        .then()
        .statusCode(200)
        .body("data._tables", not(nullValue()));
    database.dropSchema(schema.getName());
  }

  @Test
  void givenContextPath_whenGetHtmlApp_thenInjectsContextPathScript() {
    given()
        .when()
        .get(CONTEXT_PATH + "/apps/central/")
        .then()
        .statusCode(200)
        .body(containsString("window.__molgenisContextPath='" + CONTEXT_PATH + "'"));
  }

  @Test
  void givenContextPath_whenGetThemeCss_thenFontPathIncludesContextPath() {
    Schema schema = setupSchema(getClass().getSimpleName() + "theme");
    given()
        .when()
        .get(CONTEXT_PATH + "/" + schema.getName() + "/theme.css")
        .then()
        .statusCode(200)
        .contentType(containsString("text/css"))
        .body(containsString(CONTEXT_PATH + "/apps/resources/webfonts"));
    database.dropSchema(schema.getName());
  }

  @Test
  void givenContextPath_whenGetApiOpenapi_thenLinksIncludeContextPath() {
    given()
        .sessionId(sessionId)
        .when()
        .get(CONTEXT_PATH + "/api/openapi")
        .then()
        .statusCode(200)
        .body(containsString(CONTEXT_PATH + "/api/graphql"))
        .body(containsString(CONTEXT_PATH + "/api/playground.html"));
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
