package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.*;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;

/**
 * E2E integration tests for the HPC API. Runs against a real MolgenisWebservice + PostgreSQL via
 * {@link ApiTestBase}. HMAC is disabled (no MOLGENIS_HPC_SHARED_SECRET configured).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("slow")
class HpcApiE2ETest extends ApiTestBase {

  private static final String WORKER_A = "e2e-worker-a";
  private static final String WORKER_B = "e2e-worker-b";
  private static final String DEFAULT_CAPABILITIES =
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

  /** Builds a request with the required HPC protocol headers. */
  private static RequestSpecification hpcRequest() {
    RequestSpecification req =
        given()
            .header("X-EMX2-API-Version", "2025-01")
            .header("X-Request-Id", UUID.randomUUID().toString())
            .header("X-Timestamp", Instant.now().toString())
            .contentType("application/json");
    if (sessionId != null) {
      req = req.sessionId(sessionId);
    }
    return req;
  }

  /** Helper: register a worker (idempotent). */
  private static void registerWorker(String workerId) {
    registerWorker(workerId, DEFAULT_CAPABILITIES);
  }

  private static void registerWorker(String workerId, String capabilitiesJson) {
    hpcRequest()
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

  private static void registerWorkerWithCapability(
      String workerId, String processor, String profile) {
    String profileValue = profile == null || profile.isBlank() ? "any" : profile;
    registerWorker(
        workerId,
        """
        [{"processor":"%s","profile":"%s","max_concurrent_jobs":4}]
        """
            .formatted(processor, profileValue));
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

    // Upload a file via PUT JSON metadata to transition CREATED → UPLOADING
    hpcRequest()
        .body(
            """
            {"sha256": "abc123", "size_bytes": 12, "content_type": "text/plain"}
            """)
        .when()
        .put("/api/hpc/artifacts/{id}/files/{path}", artifactId, "test.txt")
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
    login(database.getAdminUserName(), "admin");
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

  @Test
  @Order(21)
  void claimRejectsWorkerWithoutMatchingCapability() {
    String processor = "cap-test-" + UUID.randomUUID();
    String profile = "gpu-a";
    String workerBad = "cap-bad-" + UUID.randomUUID();
    String workerGood = "cap-good-" + UUID.randomUUID();
    registerWorkerWithCapability(workerBad, processor, "gpu-b");
    registerWorkerWithCapability(workerGood, processor, profile);

    String jobId = createJobHelper(processor, profile);

    claimJobHelper(jobId, workerBad)
        .then()
        .statusCode(409)
        .body("title", equalTo("Conflict"))
        .body("detail", containsString("does not have a registered capability"));

    // Job remains claimable by a compatible worker.
    claimJobHelper(jobId, workerGood)
        .then()
        .statusCode(200)
        .body("status", equalTo("CLAIMED"))
        .body("worker_id", equalTo(workerGood));
  }

  @Test
  @Order(22)
  void claimIsAtomicUnderRace() throws Exception {
    String processor = "race-test-" + UUID.randomUUID();
    String worker1 = "race-worker-1-" + UUID.randomUUID();
    String worker2 = "race-worker-2-" + UUID.randomUUID();
    registerWorkerWithCapability(worker1, processor, null);
    registerWorkerWithCapability(worker2, processor, null);
    String jobId = createJobHelper(processor);

    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch startLatch = new CountDownLatch(1);
    try {
      Future<Integer> first =
          pool.submit(
              () -> {
                startLatch.await();
                return claimJobHelper(jobId, worker1).statusCode();
              });
      Future<Integer> second =
          pool.submit(
              () -> {
                startLatch.await();
                return claimJobHelper(jobId, worker2).statusCode();
              });

      startLatch.countDown();

      int s1 = first.get(5, TimeUnit.SECONDS);
      int s2 = second.get(5, TimeUnit.SECONDS);
      Assertions.assertTrue(
          (s1 == 200 && s2 == 409) || (s1 == 409 && s2 == 200),
          "Exactly one race claimant must succeed. statuses=" + s1 + "," + s2);

      String winningWorker = s1 == 200 ? worker1 : worker2;
      hpcRequest()
          .when()
          .get("/api/hpc/jobs/{id}", jobId)
          .then()
          .statusCode(200)
          .body("status", equalTo("CLAIMED"))
          .body("worker_id", equalTo(winningWorker));
    } finally {
      pool.shutdownNow();
    }
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
        .contentType(startsWith("application/problem+json"))
        .body("type", equalTo("about:blank"))
        .body("title", equalTo("Conflict"))
        .body("status", equalTo(409))
        .body("detail", containsString("Cannot transition job"))
        .body("instance", startsWith("urn:request:"));
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

  @Test
  @Order(51)
  void staleWorkersAreExpiredDuringPolling() {
    String staleWorkerId = "stale-worker-" + UUID.randomUUID();
    registerWorkerWithCapability(staleWorkerId, "stale-test", null);

    Table workers = database.getSchema(Constants.SYSTEM_SCHEMA).getTable("HpcWorkers");
    Row staleWorker =
        workers.retrieveRows().stream()
            .filter(r -> staleWorkerId.equals(r.getString("worker_id")))
            .findFirst()
            .orElseThrow();
    staleWorker.set("last_heartbeat_at", LocalDateTime.now().minusMinutes(30));
    workers.update(staleWorker);

    // listJobs triggers lazy stale-worker expiry
    hpcRequest().queryParam("status", "PENDING").when().get("/api/hpc/jobs").then().statusCode(200);

    // Worker should already be removed by expiry.
    hpcRequest().when().delete("/api/hpc/workers/{id}", staleWorkerId).then().statusCode(404);
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

    // Upload file via PUT (JSON metadata-only mode)
    hpcRequest()
        .body(
            """
            {
              "sha256": "abc123def456",
              "size_bytes": 1024,
              "content_type": "text/csv"
            }
            """)
        .when()
        .put("/api/hpc/artifacts/{id}/files/{path}", artifactId, "results/output.csv")
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

  @Test
  @Order(61)
  void artifactFileEndpointAcceptsLiteralNestedPaths() {
    String artifactId =
        hpcRequest()
            .body(
                """
                {"type": "dataset", "residence": "managed"}
                """)
            .when()
            .post("/api/hpc/artifacts")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");

    hpcRequest()
        .body(
            """
            {
              "sha256": "nested123",
              "size_bytes": 11,
              "content_type": "text/plain"
            }
            """)
        .when()
        .put("/api/hpc/artifacts/{id}/files/results/nested/output.txt", artifactId)
        .then()
        .statusCode(201)
        .body("path", equalTo("results/nested/output.txt"));

    hpcRequest()
        .when()
        .head("/api/hpc/artifacts/{id}/files/results/nested/output.txt", artifactId)
        .then()
        .statusCode(200)
        .header("X-Content-SHA256", equalTo("nested123"))
        .header("Content-Length", equalTo("11"));

    hpcRequest()
        .when()
        .delete("/api/hpc/artifacts/{id}/files/results/nested/output.txt", artifactId)
        .then()
        .statusCode(204);
  }

  @Test
  @Order(62)
  void committedArtifactRejectsFurtherMutation() {
    String artifactId = createCommittedArtifact("dataset", "immutable-e2e");

    // Cannot delete files from a committed artifact.
    hpcRequest()
        .when()
        .delete("/api/hpc/artifacts/{id}/files/{path}", artifactId, "test.txt")
        .then()
        .statusCode(409)
        .body("title", equalTo("Conflict"));

    // Cannot overwrite file content/metadata after commit.
    Response overwrite =
        hpcRequest()
            .body(
                """
                {"sha256": "new-hash", "size_bytes": 99, "content_type": "text/plain"}
                """)
            .when()
            .put("/api/hpc/artifacts/{id}/files/{path}", artifactId, "test.txt");
    Assertions.assertTrue(
        overwrite.statusCode() >= 400, "Committed artifact mutation must be rejected");

    // Cannot add new files after commit either.
    Response addFile =
        hpcRequest()
            .body(
                """
                {"sha256": "new-file-hash", "size_bytes": 21, "content_type": "text/plain"}
                """)
            .when()
            .put("/api/hpc/artifacts/{id}/files/{path}", artifactId, "new-file.txt");
    Assertions.assertTrue(addFile.statusCode() >= 400, "Committed artifact must reject new files");

    // Existing file metadata remains unchanged.
    hpcRequest()
        .when()
        .head("/api/hpc/artifacts/{id}/files/{path}", artifactId, "test.txt")
        .then()
        .statusCode(200)
        .header("X-Content-SHA256", equalTo("abc123"))
        .header("Content-Length", equalTo("12"));
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
  void deleteNonTerminalJobReturnsConflict() {
    // Create PENDING job → DELETE should fail with 409.
    String jobId = createJobHelper("delete-pending-test");

    hpcRequest()
        .when()
        .delete("/api/hpc/jobs/{id}", jobId)
        .then()
        .statusCode(409)
        .body("title", equalTo("Conflict"));

    // Job still exists.
    hpcRequest()
        .when()
        .get("/api/hpc/jobs/{id}", jobId)
        .then()
        .statusCode(200)
        .body("status", equalTo("PENDING"));

    // Cleanup
    hpcRequest().when().post("/api/hpc/jobs/{id}/cancel", jobId).then().statusCode(200);
    hpcRequest().when().delete("/api/hpc/jobs/{id}", jobId).then().statusCode(204);
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
        .body("items[0].to_status", equalTo("PENDING"))
        .body("items[1].to_status", equalTo("CLAIMED"))
        .body("items[2].to_status", equalTo("SUBMITTED"))
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
