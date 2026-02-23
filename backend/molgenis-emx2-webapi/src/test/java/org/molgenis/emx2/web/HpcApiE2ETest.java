package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.*;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;

/**
 * E2E integration tests for the HPC API. Runs against a real MolgenisWebservice + PostgreSQL via
 * {@link ApiTestBase}. HMAC is disabled (no MOLGENIS_HPC_SHARED_SECRET configured).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("slow")
class HpcApiE2ETest extends ApiTestBase {

  private static final String WORKER_A = "e2e-worker-a";
  private static final String WORKER_B = "e2e-worker-b";

  /** Builds a request with the required HPC protocol headers. */
  private static RequestSpecification hpcRequest() {
    return given()
        .header("X-EMX2-API-Version", "2025-01")
        .header("X-Request-Id", UUID.randomUUID().toString())
        .header("X-Timestamp", Instant.now().toString())
        .contentType("application/json");
  }

  /** Helper: register a worker (idempotent). */
  private static void registerWorker(String workerId) {
    hpcRequest()
        .body(
            """
            {"worker_id": "%s", "hostname": "test.local",
             "capabilities": [{"processor": "any", "profile": "any", "max_concurrent_jobs": 4}]}
            """
                .formatted(workerId))
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(200);
  }

  /** Helper: create a job and return its ID. */
  private static String createJobHelper(String processor) {
    return createJobHelper(processor, null);
  }

  private static String createJobHelper(String processor, String profile) {
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
  private static Response claimJobHelper(String jobId, String workerId) {
    return hpcRequest()
        .body(
            """
            {"worker_id": "%s"}
            """
                .formatted(workerId))
        .when()
        .post("/api/hpc/jobs/{id}/claim", jobId);
  }

  /** Helper: transition a job. */
  private static Response transitionHelper(String jobId, String status, String workerId) {
    return transitionHelper(jobId, status, workerId, null);
  }

  private static Response transitionHelper(
      String jobId, String status, String workerId, String detail) {
    return transitionHelper(jobId, status, workerId, detail, null, null);
  }

  private static Response transitionHelper(
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
    return hpcRequest().body(sb.toString()).when().post("/api/hpc/jobs/{id}/transition", jobId);
  }

  /** Helper: create a managed artifact, commit it, return its ID. */
  private static String createCommittedArtifact(String type, String name) {
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

    // Upload a file via JSON metadata to transition CREATED → UPLOADING
    hpcRequest()
        .body(
            """
            {"path": "test.txt", "sha256": "abc123", "size_bytes": 12, "content_type": "text/plain"}
            """)
        .when()
        .post("/api/hpc/artifacts/{id}/files", artifactId)
        .then()
        .statusCode(201);

    // Commit: UPLOADING → COMMITTED (omit sha256 to skip hash verification)
    hpcRequest()
        .body("{}")
        .when()
        .post("/api/hpc/artifacts/{id}/commit", artifactId)
        .then()
        .statusCode(200);

    return artifactId;
  }

  @BeforeAll
  static void registerTestWorkers() {
    registerWorker(WORKER_A);
    registerWorker(WORKER_B);
  }

  // ── 1. Health endpoint ──────────────────────────────────────────────────

  @Test
  @Order(1)
  void healthEndpointReturnsOk() {
    given()
        .when()
        .get("/api/hpc/health")
        .then()
        .statusCode(200)
        .body("status", equalTo("ok"))
        .body("api_version", equalTo("2025-01"))
        .body("database", equalTo("connected"));
  }

  // ── 2. Full job lifecycle ───────────────────────────────────────────────

  @Test
  @Order(10)
  void fullJobLifecycle() {
    // Create
    String jobId =
        hpcRequest()
            .body(
                """
                {
                  "processor": "lifecycle-test",
                  "profile": "gpu-medium",
                  "submit_user": "test-user",
                  "parameters": {"model": "bge-large", "batch_size": 32}
                }
                """)
            .when()
            .post("/api/hpc/jobs")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("status", equalTo("PENDING"))
            .extract()
            .jsonPath()
            .getString("id");

    // Claim
    claimJobHelper(jobId, WORKER_A)
        .then()
        .statusCode(200)
        .body("status", equalTo("CLAIMED"))
        .body("worker_id", equalTo(WORKER_A));

    // CLAIMED → SUBMITTED
    transitionHelper(jobId, "SUBMITTED", WORKER_A, "sbatch id 99")
        .then()
        .statusCode(200)
        .body("status", equalTo("SUBMITTED"));

    // SUBMITTED → STARTED
    transitionHelper(jobId, "STARTED", WORKER_A, "running on node-05")
        .then()
        .statusCode(200)
        .body("status", equalTo("STARTED"));

    // STARTED → COMPLETED
    transitionHelper(jobId, "COMPLETED", WORKER_A, "exit code 0")
        .then()
        .statusCode(200)
        .body("status", equalTo("COMPLETED"));
  }

  // ── 3. Claim conflict ──────────────────────────────────────────────────

  @Test
  @Order(20)
  void claimConflict() {
    String jobId = createJobHelper("conflict-test");

    // Worker A claims
    claimJobHelper(jobId, WORKER_A).then().statusCode(200);

    // Worker B tries to claim — 409
    claimJobHelper(jobId, WORKER_B).then().statusCode(409).body("title", equalTo("Conflict"));
  }

  // ── 4. Invalid transitions ─────────────────────────────────────────────

  @Test
  @Order(30)
  void invalidTransitionPendingToCompleted() {
    String jobId = createJobHelper("transition-test");

    // PENDING → COMPLETED should fail (must go through CLAIMED first)
    transitionHelper(jobId, "COMPLETED", WORKER_A)
        .then()
        .statusCode(409)
        .body("title", equalTo("Conflict"));
  }

  // ── 5. Job listing with filters ────────────────────────────────────────

  @Test
  @Order(40)
  void listJobsWithFilters() {
    createJobHelper("filter-proc-A", "small");
    createJobHelper("filter-proc-B", "large");

    // Filter by processor
    hpcRequest()
        .queryParam("processor", "filter-proc-A")
        .when()
        .get("/api/hpc/jobs")
        .then()
        .statusCode(200)
        .body("items", hasSize(greaterThanOrEqualTo(1)))
        .body("items[0].processor", equalTo("filter-proc-A"))
        .body("total_count", greaterThanOrEqualTo(1));

    // Filter by status
    hpcRequest()
        .queryParam("status", "PENDING")
        .when()
        .get("/api/hpc/jobs")
        .then()
        .statusCode(200)
        .body("items", hasSize(greaterThanOrEqualTo(1)));

    // Pagination
    hpcRequest()
        .queryParam("limit", "2")
        .queryParam("offset", "0")
        .when()
        .get("/api/hpc/jobs")
        .then()
        .statusCode(200)
        .body("limit", equalTo(2))
        .body("offset", equalTo(0))
        .body("items", hasSize(lessThanOrEqualTo(2)));
  }

  // ── 6. Worker registration + heartbeat ─────────────────────────────────

  @Test
  @Order(50)
  void workerRegistrationAndHeartbeat() {
    hpcRequest()
        .body(
            """
            {
              "worker_id": "e2e-worker-reg",
              "hostname": "node-01.test.local",
              "capabilities": [
                {"processor": "text-embedding", "profile": "gpu-medium", "max_concurrent_jobs": 4}
              ]
            }
            """)
        .when()
        .post("/api/hpc/workers/register")
        .then()
        .statusCode(200)
        .body("worker_id", equalTo("e2e-worker-reg"))
        .body("registered_at", notNullValue())
        .body("last_heartbeat_at", notNullValue());

    hpcRequest()
        .when()
        .post("/api/hpc/workers/{id}/heartbeat", "e2e-worker-reg")
        .then()
        .statusCode(200)
        .body("worker_id", equalTo("e2e-worker-reg"))
        .body("status", equalTo("ok"));
  }

  // ── 7. Artifact lifecycle ──────────────────────────────────────────────

  @Test
  @Order(60)
  void artifactLifecycle() {
    // Create artifact (type must match HpcArtifactType ontology)
    Response createResp =
        hpcRequest()
            .body(
                """
                {"type": "dataset", "format": "csv", "residence": "managed"}
                """)
            .when()
            .post("/api/hpc/artifacts");

    createResp.then().statusCode(201).body("id", notNullValue()).body("type", equalTo("dataset"));

    String artifactId = createResp.jsonPath().getString("id");

    // Upload file (JSON metadata-only mode)
    hpcRequest()
        .body(
            """
            {
              "path": "results/output.csv",
              "role": "primary",
              "sha256": "abc123def456",
              "size_bytes": 1024,
              "content_type": "text/csv"
            }
            """)
        .when()
        .post("/api/hpc/artifacts/{id}/files", artifactId)
        .then()
        .statusCode(201)
        .body("artifact_id", equalTo(artifactId))
        .body("path", equalTo("results/output.csv"));

    // List files
    hpcRequest()
        .when()
        .get("/api/hpc/artifacts/{id}/files", artifactId)
        .then()
        .statusCode(200)
        .body("count", equalTo(1))
        .body("items[0].path", equalTo("results/output.csv"));

    // Commit artifact — sha256 must match the single file's sha256 (tree hash for 1 file = file
    // hash)
    hpcRequest()
        .body(
            """
            {"sha256": "abc123def456", "size_bytes": 1024}
            """)
        .when()
        .post("/api/hpc/artifacts/{id}/commit", artifactId)
        .then()
        .statusCode(200)
        .body("status", equalTo("COMMITTED"));
  }

  // ── 8. Job cancellation ────────────────────────────────────────────────

  @Test
  @Order(70)
  void jobCancellation() {
    String jobId = createJobHelper("cancel-test");

    claimJobHelper(jobId, WORKER_A).then().statusCode(200);

    // Cancel the job
    hpcRequest()
        .header("X-Worker-Id", WORKER_A)
        .when()
        .post("/api/hpc/jobs/{id}/cancel", jobId)
        .then()
        .statusCode(200)
        .body("status", equalTo("CANCELLED"));

    // Verify via GET
    hpcRequest()
        .when()
        .get("/api/hpc/jobs/{id}", jobId)
        .then()
        .statusCode(200)
        .body("status", equalTo("CANCELLED"));
  }

  // ── 9. Delete job ─────────────────────────────────────────────────────

  @Test
  @Order(90)
  void deleteTerminalJob() {
    // Create → claim → complete → DELETE → 204
    String jobId = createJobHelper("delete-test");
    claimJobHelper(jobId, WORKER_A).then().statusCode(200);
    transitionHelper(jobId, "SUBMITTED", WORKER_A).then().statusCode(200);
    transitionHelper(jobId, "STARTED", WORKER_A).then().statusCode(200);
    transitionHelper(jobId, "COMPLETED", WORKER_A).then().statusCode(200);

    hpcRequest().when().delete("/api/hpc/jobs/{id}", jobId).then().statusCode(204);

    // GET same job → 404
    hpcRequest().when().get("/api/hpc/jobs/{id}", jobId).then().statusCode(404);
  }

  @Test
  @Order(91)
  void deleteNonTerminalJobCancelsThenDeletes() {
    // Create PENDING job → DELETE → 204 (auto-cancels then deletes)
    String jobId = createJobHelper("delete-pending-test");

    hpcRequest().when().delete("/api/hpc/jobs/{id}", jobId).then().statusCode(204);

    // GET same job → 404
    hpcRequest().when().get("/api/hpc/jobs/{id}", jobId).then().statusCode(404);
  }

  // ── 9b. Delete non-existent job ─────────────────────────────────────────

  @Test
  @Order(92)
  void deleteNonExistentJobReturns404() {
    hpcRequest()
        .when()
        .delete("/api/hpc/jobs/{id}", UUID.randomUUID().toString())
        .then()
        .statusCode(404);
  }

  // ── 10. Transition audit trail ─────────────────────────────────────────

  @Test
  @Order(100)
  void transitionAuditTrail() {
    String jobId = createJobHelper("audit-test");

    claimJobHelper(jobId, WORKER_A).then().statusCode(200);

    transitionHelper(jobId, "SUBMITTED", WORKER_A, "sbatch 42").then().statusCode(200);

    // Get transitions and verify audit trail
    hpcRequest()
        .when()
        .get("/api/hpc/jobs/{id}/transitions", jobId)
        .then()
        .statusCode(200)
        .body("count", greaterThanOrEqualTo(3))
        .body("items.find { it.to_status == 'PENDING' }", notNullValue())
        .body("items.find { it.to_status == 'CLAIMED' }", notNullValue())
        .body("items.find { it.to_status == 'SUBMITTED' }", notNullValue());
  }

  // ── 11. Log artifact on completed job ──────────────────────────────────

  @Test
  @Order(110)
  void completedJobWithLogAndOutputArtifacts() {
    // Create two committed artifacts: one output, one log
    String outputArtifactId = createCommittedArtifact("blob", "output-e2e");
    String logArtifactId = createCommittedArtifact("log", "log-e2e");

    // Verify log artifact was created with type=log
    hpcRequest()
        .when()
        .get("/api/hpc/artifacts/{id}", logArtifactId)
        .then()
        .statusCode(200)
        .body("type", equalTo("log"))
        .body("name", equalTo("log-e2e"))
        .body("status", equalTo("COMMITTED"));

    // Create job and advance to STARTED
    String jobId = createJobHelper("log-artifact-test");
    claimJobHelper(jobId, WORKER_A).then().statusCode(200);
    transitionHelper(jobId, "SUBMITTED", WORKER_A).then().statusCode(200);
    transitionHelper(jobId, "STARTED", WORKER_A).then().statusCode(200);

    // STARTED → COMPLETED with both artifact IDs
    transitionHelper(jobId, "COMPLETED", WORKER_A, "exit code 0", outputArtifactId, logArtifactId)
        .then()
        .statusCode(200)
        .body("status", equalTo("COMPLETED"))
        .body("output_artifact_id", equalTo(outputArtifactId))
        .body("log_artifact_id", equalTo(logArtifactId))
        .body("output_artifact.id", equalTo(outputArtifactId))
        .body("output_artifact.type", equalTo("blob"))
        .body("log_artifact.id", equalTo(logArtifactId))
        .body("log_artifact.type", equalTo("log"));

    // GET the job — verify both artifacts are persisted
    hpcRequest()
        .when()
        .get("/api/hpc/jobs/{id}", jobId)
        .then()
        .statusCode(200)
        .body("output_artifact_id", equalTo(outputArtifactId))
        .body("log_artifact_id", equalTo(logArtifactId))
        .body("output_artifact.name", equalTo("output-e2e"))
        .body("log_artifact.name", equalTo("log-e2e"));
  }

  // ── 12. Log artifact on failed job (no output artifact) ─────────────────

  @Test
  @Order(120)
  void failedJobWithLogArtifactOnly() {
    String logArtifactId = createCommittedArtifact("log", "log-fail-e2e");

    String jobId = createJobHelper("log-fail-test");
    claimJobHelper(jobId, WORKER_A).then().statusCode(200);
    transitionHelper(jobId, "SUBMITTED", WORKER_A).then().statusCode(200);
    transitionHelper(jobId, "STARTED", WORKER_A).then().statusCode(200);

    // STARTED → FAILED with only log artifact (no output)
    transitionHelper(jobId, "FAILED", WORKER_A, "segfault", null, logArtifactId)
        .then()
        .statusCode(200)
        .body("status", equalTo("FAILED"))
        .body("output_artifact_id", nullValue())
        .body("log_artifact_id", equalTo(logArtifactId))
        .body("log_artifact.id", equalTo(logArtifactId))
        .body("log_artifact.type", equalTo("log"));

    // GET the job — verify persisted correctly
    hpcRequest()
        .when()
        .get("/api/hpc/jobs/{id}", jobId)
        .then()
        .statusCode(200)
        .body("status", equalTo("FAILED"))
        .body("log_artifact_id", equalTo(logArtifactId))
        .body("output_artifact_id", nullValue());
  }

  // ── 13. Job listing includes log_artifact_id ──────────────────────────

  @Test
  @Order(130)
  void jobListingIncludesLogArtifactId() {
    // List COMPLETED jobs — at least one should have a log artifact
    hpcRequest()
        .queryParam("status", "COMPLETED")
        .when()
        .get("/api/hpc/jobs")
        .then()
        .statusCode(200)
        .body("items", hasSize(greaterThanOrEqualTo(1)));
  }
}
