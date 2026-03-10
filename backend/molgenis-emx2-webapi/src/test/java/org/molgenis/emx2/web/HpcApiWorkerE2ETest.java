package org.molgenis.emx2.web;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsString;

import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;

/** E2E tests for HPC worker registration, heartbeat, and stale-worker expiry. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("slow")
class HpcApiWorkerE2ETest extends HpcApiTestBase {

  @Test
  @Order(50)
  void workerRegistrationAndHeartbeat() {
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
  void staleWorkersAreExpiredDuringPolling() {
    String staleWorkerId = HpcTestkit.nextName("stale-worker");
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
}
