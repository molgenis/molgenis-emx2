package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("slow")
class HpcApiAuthRotationE2ETest extends ApiTestBase {

  private static final String HPC_ENABLED_SETTING = "MOLGENIS_HPC_ENABLED";
  private static final String HPC_CREDENTIALS_KEY_SETTING = "MOLGENIS_HPC_CREDENTIALS_KEY";
  private static final String TEST_CREDENTIALS_KEY =
      "hpc-auth-rotation-key-0123456789abcdef0123456789abcd";
  private static final String WORKER_ID = "auth-rotation-worker";
  private static String previousEnabled;
  private static String previousCredentialsKey;

  @BeforeAll
  static void configureHpcSettings() {
    previousEnabled = database.getSetting(HPC_ENABLED_SETTING);
    previousCredentialsKey = database.getSetting(HPC_CREDENTIALS_KEY_SETTING);
    database.setSetting(HPC_ENABLED_SETTING, "true");
    database.setSetting(HPC_CREDENTIALS_KEY_SETTING, TEST_CREDENTIALS_KEY);
    login(database.getAdminUserName(), "admin");
  }

  @AfterAll
  static void restoreHpcSettings() {
    if (previousEnabled == null || previousEnabled.isBlank()) {
      database.removeSetting(HPC_ENABLED_SETTING);
    } else {
      database.setSetting(HPC_ENABLED_SETTING, previousEnabled);
    }
    if (previousCredentialsKey == null || previousCredentialsKey.isBlank()) {
      database.removeSetting(HPC_CREDENTIALS_KEY_SETTING);
    } else {
      database.setSetting(HPC_CREDENTIALS_KEY_SETTING, previousCredentialsKey);
    }
  }

  @Test
  void workerCredentialRotationRevocationAndDisableTakeEffectWithoutRestart() {
    String registerBody =
        """
        {
          "worker_id": "%s",
          "hostname": "rotation.test.local",
          "capabilities": [
            {"processor":"rotation-test","profile":"any","max_concurrent_jobs":1}
          ]
        }
        """
            .formatted(WORKER_ID);

    Response issued =
        HpcTestkit.hpcRequest(sessionId)
            .body("{}")
            .when()
            .post("/api/hpc/workers/{id}/credentials/issue", WORKER_ID)
            .then()
            .statusCode(201)
            .extract()
            .response();
    String secretA = issued.jsonPath().getString("secret");
    String credentialIdA = issued.jsonPath().getString("id");

    signedHmacJsonRequest("POST", "/api/hpc/workers/register", registerBody, secretA, WORKER_ID)
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(200);

    Response rotated =
        HpcTestkit.hpcRequest(sessionId)
            .body("{}")
            .when()
            .post("/api/hpc/workers/{id}/credentials/rotate", WORKER_ID)
            .then()
            .statusCode(200)
            .extract()
            .response();
    String secretB = rotated.jsonPath().getString("secret");
    String credentialIdB = rotated.jsonPath().getString("id");

    signedHmacJsonRequest("POST", "/api/hpc/workers/register", registerBody, secretA, WORKER_ID)
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(401);

    signedHmacJsonRequest("POST", "/api/hpc/workers/register", registerBody, secretB, WORKER_ID)
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(200);

    HpcTestkit.hpcRequest(sessionId)
        .when()
        .post("/api/hpc/workers/{id}/credentials/{credentialId}/revoke", WORKER_ID, credentialIdB)
        .then()
        .statusCode(200);

    signedHmacJsonRequest("POST", "/api/hpc/workers/register", registerBody, secretB, WORKER_ID)
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(401);

    // Credential A was revoked by rotate, credential B by explicit revoke.
    HpcTestkit.hpcRequest(sessionId)
        .when()
        .post("/api/hpc/workers/{id}/credentials/{credentialId}/revoke", WORKER_ID, credentialIdA)
        .then()
        .statusCode(200);

    database.setSetting(HPC_ENABLED_SETTING, "false");

    signedHmacJsonRequest("POST", "/api/hpc/workers/register", registerBody, secretB, WORKER_ID)
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(503);
  }

  @Test
  void issueCredentialReturns503WhenCredentialsKeyIsMissing() {
    database.setSetting(HPC_ENABLED_SETTING, "true");
    database.removeSetting(HPC_CREDENTIALS_KEY_SETTING);
    try {
      HpcTestkit.hpcRequest(sessionId)
          .body("{}")
          .when()
          .post("/api/hpc/workers/{id}/credentials/issue", HpcTestkit.nextName("missing-key"))
          .then()
          .statusCode(503)
          .body("detail", containsString(HPC_CREDENTIALS_KEY_SETTING));
    } finally {
      database.setSetting(HPC_CREDENTIALS_KEY_SETTING, TEST_CREDENTIALS_KEY);
    }
  }

  private static RequestSpecification signedHmacJsonRequest(
      String method, String pathWithQuery, String body, String workerSecret, String workerId) {
    return given()
        .headers(HpcTestkit.hmacHeaders(method, pathWithQuery, body, workerSecret))
        .header("X-Worker-Id", workerId)
        .contentType("application/json")
        .body(body);
  }
}
