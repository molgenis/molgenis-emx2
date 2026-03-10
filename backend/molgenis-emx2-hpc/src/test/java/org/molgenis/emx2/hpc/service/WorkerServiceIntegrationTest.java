package org.molgenis.emx2.hpc.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.hpc.model.HpcJobStatus;

class WorkerServiceIntegrationTest extends HpcServiceIntegrationTestBase {

  @Test
  void registerReplacesCapabilitySetAndRefreshesHeartbeat() {
    String workerId = "worker-refresh";

    Row first =
        workerService.registerOrHeartbeat(
            workerId,
            "node-a",
            List.of(
                Map.of(
                    "processor", "proc-a",
                    "profile", "p1",
                    "max_concurrent_jobs", 1),
                Map.of(
                    "processor", "proc-b",
                    "profile", "p2",
                    "max_concurrent_jobs", 2)));

    Table caps = database.getSchema(schemaName).getTable("HpcWorkerCapabilities");
    assertEquals(2, caps.where(f("worker_id", EQUALS, workerId)).retrieveRows().size());

    Row second =
        workerService.registerOrHeartbeat(
            workerId,
            "node-b",
            List.of(
                Map.of(
                    "processor", "proc-c",
                    "profile", "p3",
                    "max_concurrent_jobs", 4)));

    assertEquals("node-b", second.getString("hostname"));
    assertNotNull(first.getString("last_heartbeat_at"));
    assertNotNull(second.getString("last_heartbeat_at"));

    List<Row> remainingCaps = caps.where(f("worker_id", EQUALS, workerId)).retrieveRows();
    assertEquals(1, remainingCaps.size());
    assertEquals("proc-c", remainingCaps.getFirst().getString("processor"));
    assertEquals("p3", remainingCaps.getFirst().getString("profile"));
  }

  @Test
  void expiringStaleWorkersNullifiesJobOwnership() {
    String workerId = "stale-worker";
    workerService.registerOrHeartbeat(
        workerId,
        "stale-host",
        List.of(
            Map.of(
                "processor", "expire-proc",
                "profile", "any",
                "max_concurrent_jobs", 1)));

    String jobId = jobService.createJob("expire-proc", "any", null, null, "integration-test", null);
    assertTrue(jobService.claimJob(jobId, workerId).isSuccess());

    Table workers = database.getSchema(schemaName).getTable("HpcWorkers");
    Row worker = workers.where(f("worker_id", EQUALS, workerId)).retrieveRows().getFirst();
    worker.set("last_heartbeat_at", LocalDateTime.now().minusMinutes(30));
    workers.update(worker);

    workerService.expireStaleWorkers();

    assertTrue(workers.where(f("worker_id", EQUALS, workerId)).retrieveRows().isEmpty());

    Table caps = database.getSchema(schemaName).getTable("HpcWorkerCapabilities");
    assertTrue(caps.where(f("worker_id", EQUALS, workerId)).retrieveRows().isEmpty());

    Row job = jobService.getJob(jobId);
    assertEquals(HpcJobStatus.CLAIMED.name(), job.getString("status"));
    assertNull(job.getString("worker_id"));
  }

  @Test
  void heartbeatIndicatesWhetherWorkerExists() {
    String workerId = "heartbeat-worker";
    workerService.registerOrHeartbeat(
        workerId,
        "node-heartbeat",
        List.of(Map.of("processor", "heartbeat-proc", "profile", "any", "max_concurrent_jobs", 1)));

    assertTrue(workerService.heartbeat(workerId));
    assertFalse(workerService.heartbeat("missing-worker"));
  }
}
