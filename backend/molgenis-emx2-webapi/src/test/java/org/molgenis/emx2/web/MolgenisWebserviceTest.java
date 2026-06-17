package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.molgenis.emx2.Constants.ANONYMOUS;

import java.net.URI;
import java.net.URL;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.utils.EnvironmentProperty;

class MolgenisWebserviceTest extends ApiTestBase {

  @BeforeAll
  static void login() {
    database.setUserPassword("foo", "testtest");
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
    schema.getMetadata().setSetting("menu", menuForRole(Privileges.EDITOR.toString()));
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

  @Test
  void testSettingServiceUrl() {
    MolgenisWebservice webservice = new MolgenisWebservice();
    assertTrue(webservice.hostUrl.toString().startsWith("http:"));
    assertTrue(webservice.hostUrl.toString().endsWith(":8081"));
  }

  @Test
  void shouldUseConfiguredServiceUrl() throws Exception {
    try (MockedStatic<EnvironmentProperty> envMock = mockStatic(EnvironmentProperty.class)) {
      envMock
          .when(
              () ->
                  EnvironmentProperty.getParameter(
                      org.molgenis.emx2.Constants.MOLGENIS_SERVICE_URL, null, ColumnType.STRING))
          .thenReturn("https://example.org");

      assertEquals(new URI("https://example.org").toURL(), new MolgenisWebservice().hostUrl);
    }
  }

  @Test
  void malformedServiceUrlShouldThrowException() throws Exception {
    try (MockedStatic<EnvironmentProperty> envMock = mockStatic(EnvironmentProperty.class)) {
      envMock
          .when(
              () ->
                  EnvironmentProperty.getParameter(
                      org.molgenis.emx2.Constants.MOLGENIS_SERVICE_URL, null, ColumnType.STRING))
          .thenReturn("\"http://example.com/foo bar\""); // contains a spave in the host name

      Assertions.assertThrows(
          MolgenisException.class,
          () -> {
            URL url = new MolgenisWebservice().hostUrl;
          });
    }
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
