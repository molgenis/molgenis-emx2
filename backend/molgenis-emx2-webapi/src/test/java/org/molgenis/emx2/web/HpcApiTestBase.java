package org.molgenis.emx2.web;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Shared test infrastructure for HPC API E2E tests. Manages the HPC lifecycle settings and provides
 * helper methods for job/worker/artifact operations.
 */
abstract class HpcApiTestBase extends ApiTestBase {

  static final String HPC_ENABLED_SETTING = "MOLGENIS_HPC_ENABLED";
  static final String HPC_CREDENTIALS_KEY_SETTING = "MOLGENIS_HPC_CREDENTIALS_KEY";
  static final String TEST_CREDENTIALS_KEY =
      "hpc-api-e2e-credentials-key-0123456789abcdef0123456789";

  static final String WORKER_A = "e2e-worker-a";
  static final String WORKER_B = "e2e-worker-b";
  static final String DEFAULT_CAPABILITIES =
      """
      [
        {"processor":"lifecycle-test","profile":"gpu-medium","max_concurrent_jobs":4},
        {"processor":"conflict-test","profile":"any","max_concurrent_jobs":4},
        {"processor":"cancel-test","profile":"any","max_concurrent_jobs":4},
        {"processor":"delete-test","profile":"any","max_concurrent_jobs":4},
        {"processor":"audit-test","profile":"any","max_concurrent_jobs":4},
        {"processor":"log-artifact-test","profile":"any","max_concurrent_jobs":4},
        {"processor":"log-fail-test","profile":"any","max_concurrent_jobs":4}
      ]
      """;

  private static String previousHpcEnabled;
  private static String previousCredentialsKey;

  /** Builds a request with the required HPC protocol headers. */
  static RequestSpecification hpcRequest() {
    return HpcTestkit.hpcRequest(sessionId);
  }

  static RequestSpecification workerRequest(String workerId) {
    return hpcRequest().header("X-Worker-Id", workerId);
  }

  /** Helper: register a worker (idempotent). */
  static void registerWorker(String workerId) {
    registerWorker(workerId, DEFAULT_CAPABILITIES);
  }

  static void registerWorker(String workerId, String capabilitiesJson) {
    workerRequest(workerId)
        .body(
            """
            {"worker_id": "%s", "hostname": "test.local",
             "capabilities": %s}
            """
                .formatted(workerId, capabilitiesJson))
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(200);
  }

  static void registerWorkerWithCapability(String workerId, String processor, String profile) {
    String profileValue = profile == null || profile.isBlank() ? "any" : profile;
    registerWorker(
        workerId,
        """
        [{"processor":"%s","profile":"%s","max_concurrent_jobs":4}]
        """
            .formatted(processor, profileValue));
  }

  /** Helper: create a job and return its ID. */
  static String createJobHelper(String processor) {
    return createJobHelper(processor, null);
  }

  static String createJobHelper(String processor, String profile) {
    String body =
        profile != null
            ? """
            {"processor": "%s", "profile": "%s"}
            """
                .formatted(processor, profile)
            : """
            {"processor": "%s"}
            """
                .formatted(processor);

    return hpcRequest()
        .body(body)
        .when()
        .post("/api/hpc/jobs")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getString("id");
  }

  /** Helper: claim a job by a worker and return the response. */
  static Response claimJobHelper(String jobId, String workerId) {
    return workerRequest(workerId)
        .body(
            """
            {"worker_id": "%s"}
            """
                .formatted(workerId))
        .when()
        .post("/api/hpc/jobs/{id}/claim", jobId);
  }

  /** Helper: transition a job. */
  static Response transitionHelper(String jobId, String status, String workerId) {
    return transitionHelper(jobId, status, workerId, null);
  }

  static Response transitionHelper(String jobId, String status, String workerId, String detail) {
    return transitionHelper(jobId, status, workerId, detail, null, null);
  }

  static Response transitionHelper(
      String jobId,
      String status,
      String workerId,
      String detail,
      String outputArtifactId,
      String logArtifactId) {
    StringBuilder sb = new StringBuilder("{");
    sb.append("\"status\": \"%s\", \"worker_id\": \"%s\"".formatted(status, workerId));
    if (detail != null) sb.append(", \"detail\": \"%s\"".formatted(detail));
    if (outputArtifactId != null)
      sb.append(", \"output_artifact_id\": \"%s\"".formatted(outputArtifactId));
    if (logArtifactId != null) sb.append(", \"log_artifact_id\": \"%s\"".formatted(logArtifactId));
    sb.append("}");
    return workerRequest(workerId)
        .body(sb.toString())
        .when()
        .post("/api/hpc/jobs/{id}/transition", jobId);
  }

  /** Helper: create a managed artifact, commit it, return its ID. */
  static String createCommittedArtifact(String type, String name) {
    String artifactId =
        hpcRequest()
            .body(
                """
                {"type": "%s", "residence": "managed", "name": "%s"}
                """
                    .formatted(type, name))
            .when()
            .post("/api/hpc/artifacts")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");

    // Upload a file via PUT JSON metadata to transition CREATED -> UPLOADING
    hpcRequest()
        .body(
            """
            {"sha256": "abc123", "size_bytes": 12, "content_type": "text/plain"}
            """)
        .when()
        .put("/api/hpc/artifacts/{id}/files/{path}", artifactId, "test.txt")
        .then()
        .statusCode(201);

    // Commit: UPLOADING -> COMMITTED (omit sha256 to skip hash verification)
    hpcRequest()
        .body("{}")
        .when()
        .post("/api/hpc/artifacts/{id}/commit", artifactId)
        .then()
        .statusCode(200);

    return artifactId;
  }

  @BeforeAll
  static void setUpHpcSettings() {
    previousHpcEnabled = database.getSetting(HPC_ENABLED_SETTING);
    previousCredentialsKey = database.getSetting(HPC_CREDENTIALS_KEY_SETTING);
    database.setSetting(HPC_ENABLED_SETTING, "true");
    database.setSetting(HPC_CREDENTIALS_KEY_SETTING, TEST_CREDENTIALS_KEY);
    login(database.getAdminUserName(), "admin");
    registerWorker(WORKER_A);
    registerWorker(WORKER_B);
  }

  @AfterAll
  static void restoreHpcSettings() {
    if (previousHpcEnabled == null || previousHpcEnabled.isBlank()) {
      database.removeSetting(HPC_ENABLED_SETTING);
    } else {
      database.setSetting(HPC_ENABLED_SETTING, previousHpcEnabled);
    }
    if (previousCredentialsKey == null || previousCredentialsKey.isBlank()) {
      database.removeSetting(HPC_CREDENTIALS_KEY_SETTING);
    } else {
      database.setSetting(HPC_CREDENTIALS_KEY_SETTING, previousCredentialsKey);
    }
  }
}
