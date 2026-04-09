package org.molgenis.emx2.web.hpc;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.molgenis.emx2.web.ApiTestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared test infrastructure for HPC API E2E tests. Manages the HPC lifecycle settings and provides
 * helper methods for job/worker/artifact operations.
 *
 * <p>All resources created via the helper methods are tracked and automatically cleaned up in
 * {@link #cleanUpTestResources()} after each test class. This prevents test pollution across
 * classes sharing the same {@code _SYSTEM_} schema.
 */
abstract class HpcApiTestBase extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(HpcApiTestBase.class);

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

  // --- Resource tracking for automatic cleanup ---

  /** Job IDs created during this test class. Insertion-ordered for deterministic cleanup. */
  private static final Set<String> trackedJobIds =
      Collections.synchronizedSet(new LinkedHashSet<>());

  /** Artifact IDs created during this test class. */
  private static final Set<String> trackedArtifactIds =
      Collections.synchronizedSet(new LinkedHashSet<>());

  /** Worker IDs registered during this test class. */
  private static final Set<String> trackedWorkerIds =
      Collections.synchronizedSet(new LinkedHashSet<>());

  // --- Request builders ---

  /** Builds a request with the required HPC protocol headers. */
  static RequestSpecification hpcRequest() {
    return HpcTestkit.hpcRequest(sessionId);
  }

  static RequestSpecification workerRequest(String workerId) {
    return hpcRequest().header("X-Worker-Id", workerId);
  }

  // --- Worker helpers ---

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
    trackedWorkerIds.add(workerId);
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

  // --- Job helpers ---

  /** Helper: create a job and return its ID. Automatically tracked for cleanup. */
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

    String jobId =
        hpcRequest()
            .body(body)
            .when()
            .post("/api/hpc/jobs")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");
    trackedJobIds.add(jobId);
    return jobId;
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

  // --- Artifact helpers ---

  /** Helper: create a managed artifact, commit it, return its ID. Automatically tracked. */
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
    trackedArtifactIds.add(artifactId);

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

  // --- Explicit resource tracking (for tests that create resources via raw requests) ---

  static void trackJob(String jobId) {
    trackedJobIds.add(jobId);
  }

  static void trackArtifact(String artifactId) {
    trackedArtifactIds.add(artifactId);
  }

  static void trackWorker(String workerId) {
    trackedWorkerIds.add(workerId);
  }

  // --- Lifecycle ---

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
  static void cleanUpTestResources() {
    // Order matters: jobs reference artifacts and workers, so delete jobs first.
    // Use best-effort: log failures but don't fail the teardown.

    for (String jobId : trackedJobIds) {
      try {
        // Cancel non-terminal jobs before deleting (DELETE requires terminal status)
        Response getResp = hpcRequest().when().get("/api/hpc/jobs/{id}", jobId);
        if (getResp.statusCode() == 200) {
          String status = getResp.jsonPath().getString("status");
          if (status != null
              && !status.equals("COMPLETED")
              && !status.equals("FAILED")
              && !status.equals("CANCELLED")) {
            hpcRequest()
                .body("{\"status\": \"CANCELLED\"}")
                .when()
                .post("/api/hpc/jobs/{id}/cancel", jobId);
          }
          hpcRequest().when().delete("/api/hpc/jobs/{id}", jobId);
        }
      } catch (Exception e) {
        logger.debug("Cleanup: could not delete job {}: {}", jobId, e.getMessage());
      }
    }
    trackedJobIds.clear();

    for (String artifactId : trackedArtifactIds) {
      try {
        hpcRequest().when().delete("/api/hpc/artifacts/{id}", artifactId);
      } catch (Exception e) {
        logger.debug("Cleanup: could not delete artifact {}: {}", artifactId, e.getMessage());
      }
    }
    trackedArtifactIds.clear();

    for (String workerId : trackedWorkerIds) {
      try {
        hpcRequest().when().delete("/api/hpc/workers/{id}", workerId);
      } catch (Exception e) {
        logger.debug("Cleanup: could not delete worker {}: {}", workerId, e.getMessage());
      }
    }
    trackedWorkerIds.clear();

    // Restore settings — use setSetting with "false"/"" rather than removeSetting,
    // because removeSetting calls setSettings(map) which overwrites ALL database settings
    // from this instance's potentially stale in-memory snapshot, which can wipe settings
    // (like MOLGENIS_JWT_SHARED_SECRET) that were set by a different SqlDatabase instance.
    if (previousHpcEnabled == null || previousHpcEnabled.isBlank()) {
      database.setSetting(HPC_ENABLED_SETTING, "false");
    } else {
      database.setSetting(HPC_ENABLED_SETTING, previousHpcEnabled);
    }
    if (previousCredentialsKey == null || previousCredentialsKey.isBlank()) {
      database.setSetting(HPC_CREDENTIALS_KEY_SETTING, "");
    } else {
      database.setSetting(HPC_CREDENTIALS_KEY_SETTING, previousCredentialsKey);
    }
  }
}
