package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;

import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("slow")
class HpcApiAuthRotationE2ETest extends ApiTestBase {

  private static final String HPC_SECRET_SETTING = "MOLGENIS_HPC_SHARED_SECRET";
  private static final String SECRET_A = "0123456789abcdef0123456789abcdef";
  private static final String SECRET_B = "fedcba9876543210fedcba9876543210";
  private static final String WORKER_ID = "auth-rotation-worker";
  private static String previousSharedSecret;

  @BeforeAll
  static void configureSharedSecret() {
    previousSharedSecret = database.getSetting(HPC_SECRET_SETTING);
    database.setSetting(HPC_SECRET_SETTING, SECRET_A);
  }

  @AfterAll
  static void clearSharedSecret() {
    if (previousSharedSecret == null || previousSharedSecret.isBlank()) {
      database.removeSetting(HPC_SECRET_SETTING);
    } else {
      database.setSetting(HPC_SECRET_SETTING, previousSharedSecret);
    }
  }

  @Test
  void sharedSecretRotationAndDisableTakeEffectWithoutRestart() {
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

    signedHmacJsonRequest("POST", "/api/hpc/workers/register", registerBody, SECRET_A, WORKER_ID)
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(200);

    database.setSetting(HPC_SECRET_SETTING, SECRET_B);

    signedHmacJsonRequest("POST", "/api/hpc/workers/register", registerBody, SECRET_A, WORKER_ID)
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(401);

    signedHmacJsonRequest("POST", "/api/hpc/workers/register", registerBody, SECRET_B, WORKER_ID)
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(200);

    database.removeSetting(HPC_SECRET_SETTING);

    signedHmacJsonRequest("POST", "/api/hpc/workers/register", registerBody, SECRET_B, WORKER_ID)
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(503);
  }

  private static RequestSpecification signedHmacJsonRequest(
      String method, String pathWithQuery, String body, String sharedSecret, String workerId) {
    return given()
        .headers(HpcTestkit.hmacHeaders(method, pathWithQuery, body, sharedSecret))
        .header("X-Worker-Id", workerId)
        .contentType("application/json")
        .body(body);
  }
}
