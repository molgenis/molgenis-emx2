package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SessionFixationTest extends ApiTestBase {

  private static final String DOCTOR = "session-fixation-doctor";
  private static final String PATIENT = "session-fixation-patient";
  private static final String PASSWORD = "testtest";

  @BeforeAll
  static void setupUsers() {
    database.setUserPassword(DOCTOR, PASSWORD);
    database.setUserPassword(PATIENT, PASSWORD);
  }

  @Test
  void givenAuthenticatedSession_whenSignInAgain_thenSessionIdIsRotated() {
    String preLoginSessionId = signin(null, DOCTOR);
    String postLoginSessionId = signin(preLoginSessionId, PATIENT);

    assertNotNull(postLoginSessionId);
    assertNotEquals(preLoginSessionId, postLoginSessionId);
    assertTrue(sessionEmail(postLoginSessionId).contains(PATIENT));
  }

  @Test
  void givenFixedSessionId_whenVictimSignsIn_thenFixedSessionIdIsNotAuthenticated() {
    String fixedSessionId = signin(null, DOCTOR);
    signin(fixedSessionId, PATIENT);

    String sessionOfFixedId = sessionEmail(fixedSessionId);
    assertTrue(!sessionOfFixedId.contains(PATIENT) && !sessionOfFixedId.contains(DOCTOR));
  }

  @Test
  void givenAuthenticatedSession_whenSignInAgain_thenSessionGaugeIsUnchanged() {
    String firstSessionId = signin(null, DOCTOR);
    double gaugeAfterFirstSignin = MolgenisSessionHandler.sessionGauge.get();

    String secondSessionId = signin(firstSessionId, DOCTOR);
    assertEquals(gaugeAfterFirstSignin, MolgenisSessionHandler.sessionGauge.get());

    signout(secondSessionId);
    assertEquals(gaugeAfterFirstSignin - 1, MolgenisSessionHandler.sessionGauge.get());
  }

  private static String signin(String sessionId, String username) {
    if (sessionId != null) {
      given().sessionId(sessionId);
    }
    return given()
        .body(
            """
            {"query":"mutation{signin(email:\\"%s\\",password:\\"%s\\"){message}}"}
            """
                .formatted(username, PASSWORD))
        .post("api/graphql")
        .sessionId();
  }

  private static void signout(String sessionId) {
    given()
        .sessionId(sessionId)
        .body(
            """
            {"query":"mutation{signout{status}}"}
            """)
        .post("api/graphql");
  }

  private static String sessionEmail(String sessionId) {
    return given()
        .sessionId(sessionId)
        .body(
            """
            {"query":"{_session{email}}"}
            """)
        .post("api/graphql")
        .asString();
  }
}
