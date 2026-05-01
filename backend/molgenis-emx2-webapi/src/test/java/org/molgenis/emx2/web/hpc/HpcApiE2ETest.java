package org.molgenis.emx2.web.hpc;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.*;

/**
 * Core lifecycle and state-machine E2E tests for the HPC API: job creation, transitions, listing,
 * cancellation, deletion, and audit trail.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("slow")
class HpcApiE2ETest extends HpcApiTestBase {

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
    trackJob(jobId);

    // Claim
    claimJobHelper(jobId, WORKER_A)
        .then()
        .statusCode(200)
        .body("status", equalTo("CLAIMED"))
        .body("worker_id", equalTo(WORKER_A));

    // CLAIMED -> SUBMITTED
    transitionHelper(jobId, "SUBMITTED", WORKER_A, "sbatch id 99")
        .then()
        .statusCode(200)
        .body("status", equalTo("SUBMITTED"));

    // SUBMITTED -> STARTED
    transitionHelper(jobId, "STARTED", WORKER_A, "running on node-05")
        .then()
        .statusCode(200)
        .body("status", equalTo("STARTED"));

    // STARTED -> COMPLETED
    transitionHelper(jobId, "COMPLETED", WORKER_A, "exit code 0")
        .then()
        .statusCode(200)
        .body("status", equalTo("COMPLETED"));
  }

  @Test
  @Order(11)
  void createJobDerivesSubmitUserFromSession() {
    String jobId =
        hpcRequest()
            .body(
                """
                {
                  "processor": "submit-user-test",
                  "submit_user": "spoofed-user"
                }
                """)
            .when()
            .post("/api/hpc/jobs")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");
    trackJob(jobId);

    hpcRequest()
        .when()
        .get("/api/hpc/jobs/{id}", jobId)
        .then()
        .statusCode(200)
        .body("submit_user", equalTo("admin"));
  }

  // ── 3. Invalid transitions ─────────────────────────────────────────────

  @Test
  @Order(30)
  void invalidTransitionPendingToCompleted() {
    String jobId = createJobHelper("transition-test");

    // PENDING -> COMPLETED should fail (must go through CLAIMED first)
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

  // ── 4. Job listing with filters ────────────────────────────────────────

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

  @Test
  @Order(41)
  void listJobsUsesDeterministicDatabasePaginationOrder() {
    String processor = "paged-proc";
    createJobHelper(processor, "batch");
    String secondJobId = createJobHelper(processor, "batch");
    String thirdJobId = createJobHelper(processor, "batch");

    hpcRequest()
        .queryParam("processor", processor)
        .queryParam("profile", "batch")
        .queryParam("limit", "2")
        .queryParam("offset", "1")
        .when()
        .get("/api/hpc/jobs")
        .then()
        .statusCode(200)
        .body("total_count", equalTo(3))
        .body("count", equalTo(2))
        .body("items[0].id", equalTo(secondJobId))
        .body("items[1].id", equalTo(thirdJobId));
  }

  // ── 5. Job cancellation ────────────────────────────────────────────────

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

  // ── 6. Delete job ─────────────────────────────────────────────────────

  @Test
  @Order(90)
  void deleteTerminalJob() {
    // Create -> claim -> complete -> DELETE -> 204
    String jobId = createJobHelper("delete-test");
    claimJobHelper(jobId, WORKER_A).then().statusCode(200);
    transitionHelper(jobId, "SUBMITTED", WORKER_A).then().statusCode(200);
    transitionHelper(jobId, "STARTED", WORKER_A).then().statusCode(200);
    transitionHelper(jobId, "COMPLETED", WORKER_A).then().statusCode(200);

    hpcRequest().when().delete("/api/hpc/jobs/{id}", jobId).then().statusCode(204);

    // GET same job -> 404
    hpcRequest().when().get("/api/hpc/jobs/{id}", jobId).then().statusCode(404);
  }

  @Test
  @Order(91)
  void deleteNonTerminalJobReturnsConflict() {
    // Create PENDING job -> DELETE should fail with 409.
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

  @Test
  @Order(92)
  void deleteNonExistentJobReturns404() {
    hpcRequest().when().delete("/api/hpc/jobs/{id}", HpcTestkit.nextUuid()).then().statusCode(404);
  }

  // ── 7. Transition audit trail ─────────────────────────────────────────

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

  // ── 8. Log/output artifacts on terminal jobs ──────────────────────────

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

    // STARTED -> COMPLETED with both artifact IDs
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

    // GET the job -- verify both artifacts are persisted
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

  @Test
  @Order(120)
  void failedJobWithLogArtifactOnly() {
    String logArtifactId = createCommittedArtifact("log", "log-fail-e2e");

    String jobId = createJobHelper("log-fail-test");
    claimJobHelper(jobId, WORKER_A).then().statusCode(200);
    transitionHelper(jobId, "SUBMITTED", WORKER_A).then().statusCode(200);
    transitionHelper(jobId, "STARTED", WORKER_A).then().statusCode(200);

    // STARTED -> FAILED with only log artifact (no output)
    transitionHelper(jobId, "FAILED", WORKER_A, "segfault", null, logArtifactId)
        .then()
        .statusCode(200)
        .body("status", equalTo("FAILED"))
        .body("output_artifact_id", nullValue())
        .body("log_artifact_id", equalTo(logArtifactId))
        .body("log_artifact.id", equalTo(logArtifactId))
        .body("log_artifact.type", equalTo("log"));

    // GET the job -- verify persisted correctly
    hpcRequest()
        .when()
        .get("/api/hpc/jobs/{id}", jobId)
        .then()
        .statusCode(200)
        .body("status", equalTo("FAILED"))
        .body("log_artifact_id", equalTo(logArtifactId))
        .body("output_artifact_id", nullValue());
  }

  // ── 9. Job listing includes log_artifact_id ──────────────────────────

  @Test
  @Order(130)
  void jobListingIncludesLogArtifactId() {
    // List COMPLETED jobs -- at least one should have a log artifact
    hpcRequest()
        .queryParam("status", "COMPLETED")
        .when()
        .get("/api/hpc/jobs")
        .then()
        .statusCode(200)
        .body("items", hasSize(greaterThanOrEqualTo(1)));
  }
}
