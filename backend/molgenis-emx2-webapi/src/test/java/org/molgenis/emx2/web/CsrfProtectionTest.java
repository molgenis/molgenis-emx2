package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CsrfProtectionTest extends ApiTestBase {

  private static final String USER = "csrf-protection-user";
  private static final String PASSWORD = "testtest";
  // shape of a cross-site <form enctype="text/plain"> that smuggles a JSON body
  private static final String TEXT_PLAIN_FORM_BODY =
      "{\"query\":\"mutation{changePassword(password:\\\"pwned1234\\\"){message}}\",\"x\":\"=\"}";

  @BeforeAll
  static void setupUser() {
    database.setUserPassword(USER, PASSWORD);
  }

  @Test
  void givenSignin_thenSessionCookieIsSameSiteLax() {
    Response response = signin();
    String setCookie = response.getHeader("Set-Cookie");
    assertTrue(setCookie.contains("SameSite=Lax"), setCookie);
  }

  @Test
  void givenCrossSiteOrigin_whenTextPlainMutation_thenRejected() {
    String session = signin().sessionId();
    given()
        .sessionId(session)
        .header("Origin", "https://evil.example")
        .contentType("text/plain")
        .body(TEXT_PLAIN_FORM_BODY)
        .post("api/graphql")
        .then()
        .statusCode(403);
    assertTrue(database.checkUserPassword(USER, PASSWORD));
  }

  @Test
  void givenCrossSiteOrigin_whenMultipartMutation_thenRejected() {
    String session = signin().sessionId();
    given()
        .sessionId(session)
        .header("Origin", "https://evil.example")
        .multiPart("query", "mutation{changePassword(password:\"pwned1234\"){message}}")
        .multiPart("variables", "{}")
        .post("api/graphql")
        .then()
        .statusCode(403);
    assertTrue(database.checkUserPassword(USER, PASSWORD));
  }

  @Test
  void givenSameOrigin_whenQuery_thenAllowed() {
    String session = signin().sessionId();
    given()
        .sessionId(session)
        .header("Origin", "http://localhost:" + port)
        .body("{\"query\":\"{_session{email}}\"}")
        .post("api/graphql")
        .then()
        .statusCode(200);
  }

  private static Response signin() {
    return given()
        .body(
            """
            {"query":"mutation{signin(email:\\"%s\\",password:\\"%s\\"){message}}"}
            """
                .formatted(USER, PASSWORD))
        .post("api/graphql");
  }
}
