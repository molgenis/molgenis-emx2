package org.molgenis.emx2.hpc.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.model.HpcJobStatus;
import org.molgenis.emx2.hpc.service.ClaimResult.ClaimOutcome;

class JobServiceIntegrationTest extends HpcServiceIntegrationTestBase {

  @Test
  void claimIsAtomicUnderConcurrentRaces() throws Exception {
    String processor = "svc-race";
    registerWorkerCapability("worker-a", processor, "gpu-a");
    registerWorkerCapability("worker-b", processor, "gpu-a");

    String jobId = jobService.createJob(processor, "gpu-a", "{}", null, "integration-test", null);

    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      Future<ClaimResult> first =
          pool.submit(
              () -> {
                start.await();
                return jobService.claimJob(jobId, "worker-a");
              });
      Future<ClaimResult> second =
          pool.submit(
              () -> {
                start.await();
                return jobService.claimJob(jobId, "worker-b");
              });

      start.countDown();

      ClaimResult r1 = first.get(5, TimeUnit.SECONDS);
      ClaimResult r2 = second.get(5, TimeUnit.SECONDS);

      int successCount = (r1.isSuccess() ? 1 : 0) + (r2.isSuccess() ? 1 : 0);
      assertEquals(1, successCount, "Exactly one claimant must succeed");

      Set<ClaimOutcome> outcomes = Set.of(r1.outcome(), r2.outcome());
      assertTrue(outcomes.contains(ClaimOutcome.SUCCESS));
      assertTrue(outcomes.contains(ClaimOutcome.NOT_PENDING));

      Row claimed = jobService.getJob(jobId);
      assertEquals(HpcJobStatus.CLAIMED.name(), claimed.getString("status"));
      assertTrue(Set.of("worker-a", "worker-b").contains(claimed.getString("worker_id")));

      long claimedTransitions =
          jobService.getTransitions(jobId).stream()
              .filter(t -> HpcJobStatus.CLAIMED.name().equals(t.getString("to_status")))
              .count();
      assertEquals(1, claimedTransitions, "Only one CLAIMED transition should be recorded");
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void transitionValidationRejectsInvalidStateAndWrongWorker() {
    String processor = "svc-transition";
    registerWorkerCapability("worker-owner", processor, "cpu");
    registerWorkerCapability("worker-other", processor, "cpu");

    String jobId = jobService.createJob(processor, "cpu", null, null, "integration-test", null);
    assertTrue(jobService.claimJob(jobId, "worker-owner").isSuccess());

    Row wrongWorker =
        jobService.transitionJob(
            jobId,
            HpcJobStatus.SUBMITTED,
            new TransitionParams(
                "worker-other", "should fail", "slurm-1", null, null, null, null, null));
    assertNull(wrongWorker, "Non-owner worker must not transition claimed job");

    Row invalidState =
        jobService.transitionJob(
            jobId,
            HpcJobStatus.COMPLETED,
            TransitionParams.of("worker-owner", "cannot skip states"));
    assertNull(invalidState, "CLAIMED -> COMPLETED must be rejected");

    Row stillClaimed = jobService.getJob(jobId);
    assertEquals(HpcJobStatus.CLAIMED.name(), stillClaimed.getString("status"));

    List<String> toStatuses =
        jobService.getTransitions(jobId).stream().map(t -> t.getString("to_status")).toList();
    assertIterableEquals(List.of("PENDING", "CLAIMED"), toStatuses);
  }

  @Test
  void transitionsRemainOrderedAndDuplicateRetriesAreIdempotent() throws Exception {
    String processor = "svc-order";
    registerWorkerCapability("worker-order", processor, "any");

    String jobId = jobService.createJob(processor, "any", null, null, "integration-test", null);
    assertTrue(jobService.claimJob(jobId, "worker-order").isSuccess());

    assertNotNull(
        jobService.transitionJob(
            jobId,
            HpcJobStatus.SUBMITTED,
            new TransitionParams(
                "worker-order", "submitted", "slurm-123", null, null, null, null, null)));
    Thread.sleep(5);
    assertNotNull(
        jobService.transitionJob(
            jobId,
            HpcJobStatus.STARTED,
            new TransitionParams(
                "worker-order", "started", "slurm-123", null, null, null, null, null)));
    Thread.sleep(5);
    assertNotNull(
        jobService.transitionJob(
            jobId,
            HpcJobStatus.COMPLETED,
            new TransitionParams(
                "worker-order", "done", "slurm-123", null, null, null, null, null)));

    int beforeDuplicate = jobService.getTransitions(jobId).size();
    assertNotNull(
        jobService.transitionJob(
            jobId,
            HpcJobStatus.COMPLETED,
            new TransitionParams(
                "worker-order", "done", "slurm-123", null, null, null, null, null)));
    int afterDuplicate = jobService.getTransitions(jobId).size();
    assertEquals(
        beforeDuplicate, afterDuplicate, "Duplicate transition retry should be idempotent");

    List<Row> transitions = jobService.getTransitions(jobId);
    List<String> statuses = transitions.stream().map(t -> t.getString("to_status")).toList();
    assertIterableEquals(
        List.of("PENDING", "CLAIMED", "SUBMITTED", "STARTED", "COMPLETED"), statuses);

    LocalDateTime previous = null;
    for (Row t : transitions) {
      LocalDateTime timestamp = parseTimestamp(t.getString("timestamp"));
      assertNotNull(timestamp, "Transition timestamp must be parseable");
      if (previous != null) {
        assertFalse(
            timestamp.isBefore(previous),
            "Transitions must be returned in ascending timestamp order");
      }
      previous = timestamp;
    }
  }

  @Test
  void startedProgressUpdatesPersistSnapshotAndAudit() {
    String processor = "svc-progress";
    registerWorkerCapability("worker-progress", processor, "any");

    String jobId = jobService.createJob(processor, "any", null, null, "integration-test", null);
    assertTrue(jobService.claimJob(jobId, "worker-progress").isSuccess());
    assertNotNull(
        jobService.transitionJob(
            jobId,
            HpcJobStatus.SUBMITTED,
            new TransitionParams(
                "worker-progress", "submitted", "slurm-progress", null, null, null, null, null)));
    assertNotNull(
        jobService.transitionJob(
            jobId,
            HpcJobStatus.STARTED,
            new TransitionParams(
                "worker-progress", "started", "slurm-progress", null, null, null, null, null)));

    int beforeProgress = jobService.getTransitions(jobId).size();

    assertNotNull(
        jobService.transitionJob(
            jobId,
            HpcJobStatus.STARTED,
            new TransitionParams(
                "worker-progress",
                "progress update",
                "slurm-progress",
                null,
                null,
                "sorting",
                "step 3 of 10",
                0.3)));

    Row updated = jobService.getJob(jobId);
    assertEquals("sorting", updated.getString("phase"));
    assertEquals("step 3 of 10", updated.getString("message"));
    assertEquals(0.3, updated.getDecimal("progress"));

    List<Row> transitions = jobService.getTransitions(jobId);
    assertEquals(
        beforeProgress + 1, transitions.size(), "Progress update should add one transition");
    Row progressTransition = transitions.get(transitions.size() - 1);
    assertEquals(HpcJobStatus.STARTED.name(), progressTransition.getString("to_status"));
    assertEquals("sorting", progressTransition.getString("phase"));
    assertEquals("step 3 of 10", progressTransition.getString("message"));
    assertEquals(0.3, progressTransition.getDecimal("progress"));

    // Idempotent retry should not duplicate this exact progress transition.
    assertNotNull(
        jobService.transitionJob(
            jobId,
            HpcJobStatus.STARTED,
            new TransitionParams(
                "worker-progress",
                "progress update",
                "slurm-progress",
                null,
                null,
                "sorting",
                "step 3 of 10",
                0.3)));
    assertEquals(beforeProgress + 1, jobService.getTransitions(jobId).size());
  }

  private void registerWorkerCapability(String workerId, String processor, String profile) {
    workerService.registerOrHeartbeat(
        workerId,
        workerId + ".node",
        List.of(
            Map.of(
                "processor", processor,
                "profile", profile,
                "max_concurrent_jobs", 1)));
  }

  private static LocalDateTime parseTimestamp(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return LocalDateTime.parse(value.replace(' ', 'T'));
  }
}
