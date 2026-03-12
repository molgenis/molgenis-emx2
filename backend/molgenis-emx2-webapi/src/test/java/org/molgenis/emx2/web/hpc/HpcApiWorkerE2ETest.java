package org.molgenis.emx2.web.hpc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsString;

import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;

/** E2E tests for HPC worker registration, heartbeat, and lifecycle. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("slow")
class HpcApiWorkerE2ETest extends HpcApiTestBase {

  @Test
  @Order(50)
  void workerRegistrationAndHeartbeat() {
    trackWorker("e2e-worker-reg");
    workerRequest("e2e-worker-reg")
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

    workerRequest("e2e-worker-reg")
        .when()
        .post("/api/hpc/workers/{id}/heartbeat", "e2e-worker-reg")
        .then()
        .statusCode(200)
        .body("worker_id", equalTo("e2e-worker-reg"))
        .body("status", equalTo("ok"));
  }

  @Test
  @Order(51)
  void staleHeartbeatDoesNotRemoveWorker() {
    String staleWorkerId = HpcTestkit.nextName("stale-worker");
    registerWorkerWithCapability(staleWorkerId, "stale-test", null);

    // Set heartbeat to 30 minutes ago
    Table workers = database.getSchema(Constants.SYSTEM_SCHEMA).getTable("HpcWorkers");
    Row staleWorker =
        workers.retrieveRows().stream()
            .filter(r -> staleWorkerId.equals(r.getString("worker_id")))
            .findFirst()
            .orElseThrow();
    staleWorker.set("last_heartbeat_at", LocalDateTime.now().minusMinutes(30));
    workers.update(staleWorker);

    // Poll jobs — worker MUST NOT be auto-removed (worker removal is explicit-only)
    hpcRequest().queryParam("status", "PENDING").when().get("/api/hpc/jobs").then().statusCode(200);

    // Worker must still exist — DELETE should succeed (204), not 404
    hpcRequest().when().delete("/api/hpc/workers/{id}", staleWorkerId).then().statusCode(204);
  }

  @Test
  @Order(52)
  void heartbeatForUnknownWorkerReturnsNotFound() {
    String missingWorkerId = HpcTestkit.nextName("missing-worker");

    workerRequest(missingWorkerId)
        .when()
        .post("/api/hpc/workers/{id}/heartbeat", missingWorkerId)
        .then()
        .statusCode(404)
        .body("detail", containsString("not found"));
  }

  @Test
  @Order(53)
  void deletingWorkerRemovesCredentialsSoWorkerIdCanBeReissued() {
    String workerId = HpcTestkit.nextName("delete-worker-credentials");
    trackWorker(workerId);

    hpcRequest()
        .body("{}")
        .when()
        .post("/api/hpc/workers/{id}/credentials/issue", workerId)
        .then()
        .statusCode(201);

    hpcRequest().when().delete("/api/hpc/workers/{id}", workerId).then().statusCode(204);

    hpcRequest()
        .body("{}")
        .when()
        .post("/api/hpc/workers/{id}/credentials/issue", workerId)
        .then()
        .statusCode(201);

    hpcRequest()
        .when()
        .get("/api/hpc/workers/{id}/credentials", workerId)
        .then()
        .statusCode(200)
        .body("count", equalTo(1));
  }
}
