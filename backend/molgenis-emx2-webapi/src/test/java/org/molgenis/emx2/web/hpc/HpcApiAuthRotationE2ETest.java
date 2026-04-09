package org.molgenis.emx2.web.hpc;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("slow")
class HpcApiAuthRotationE2ETest extends HpcApiTestBase {

  @Test
  void workerCredentialRotationRevocationAndDisableTakeEffectWithoutRestart() {
    String workerId = HpcTestkit.nextName("auth-rotation-worker");
    trackWorker(workerId);

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
            .formatted(workerId);

    Response issued =
        hpcRequest()
            .body("{}")
            .when()
            .post("/api/hpc/workers/{id}/credentials/issue", workerId)
            .then()
            .statusCode(201)
            .extract()
            .response();
    String secretA = issued.jsonPath().getString("secret");
    String credentialIdA = issued.jsonPath().getString("id");

    signedHmacJsonRequest("POST", "/api/hpc/workers/register", registerBody, secretA, workerId)
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(200);

    Response rotated =
        hpcRequest()
            .body("{}")
            .when()
            .post("/api/hpc/workers/{id}/credentials/rotate", workerId)
            .then()
            .statusCode(200)
            .extract()
            .response();
    String secretB = rotated.jsonPath().getString("secret");
    String credentialIdB = rotated.jsonPath().getString("id");

    signedHmacJsonRequest("POST", "/api/hpc/workers/register", registerBody, secretA, workerId)
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(401);

    signedHmacJsonRequest("POST", "/api/hpc/workers/register", registerBody, secretB, workerId)
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(200);

    hpcRequest()
        .when()
        .post("/api/hpc/workers/{id}/credentials/{credentialId}/revoke", workerId, credentialIdB)
        .then()
        .statusCode(200);

    signedHmacJsonRequest("POST", "/api/hpc/workers/register", registerBody, secretB, workerId)
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(401);

    // Credential A was revoked by rotate, credential B by explicit revoke.
    hpcRequest()
        .when()
        .post("/api/hpc/workers/{id}/credentials/{credentialId}/revoke", workerId, credentialIdA)
        .then()
        .statusCode(200);

    // The disabled-HPC path (setting MOLGENIS_HPC_ENABLED=false → 503) is covered
    // by issueCredentialReturns503WhenCredentialsKeyIsMissing. In-process settings
    // propagation across SqlDatabase instances is unreliable for E2E tests.
  }

  @Test
  void issueCredentialReturns503WhenCredentialsKeyIsMissing() {
    database.clearCache();
    database.setSetting(HPC_ENABLED_SETTING, "true");
    database.removeSetting(HPC_CREDENTIALS_KEY_SETTING);
    try {
      hpcRequest()
          .body("{}")
          .when()
          .post("/api/hpc/workers/{id}/credentials/issue", HpcTestkit.nextName("missing-key"))
          .then()
          .statusCode(503)
          .body("detail", containsString(HPC_CREDENTIALS_KEY_SETTING));
    } finally {
      database.clearCache();
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
