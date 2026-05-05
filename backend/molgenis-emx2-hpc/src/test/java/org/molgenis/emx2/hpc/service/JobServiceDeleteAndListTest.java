package org.molgenis.emx2.hpc.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.model.HpcJobStatus;

class JobServiceDeleteAndListTest extends HpcServiceIntegrationTestBase {

  @Test
  void deleteJob_removesTerminalJobAndTransitions() {
    registerWorker("w1", "proc", "p");
    String jobId = jobService.createJob("proc", "p", null, null, "user", null);
    jobService.claimJob(jobId, "w1");
    jobService.transitionJob(
        jobId,
        HpcJobStatus.SUBMITTED,
        new TransitionParams("w1", "submitted", "slurm-1", null, null, null, null, null));
    jobService.transitionJob(
        jobId,
        HpcJobStatus.STARTED,
        new TransitionParams("w1", "started", "slurm-1", null, null, null, null, null));
    jobService.transitionJob(jobId, HpcJobStatus.COMPLETED, TransitionParams.of("w1", "done"));

    Row deleted = jobService.deleteJob(jobId);
    assertNotNull(deleted);
    assertNull(jobService.getJob(jobId));
    assertTrue(jobService.getTransitions(jobId).isEmpty());
  }

  @Test
  void deleteJob_rejectsNonTerminalJob() {
    registerWorker("w2", "proc", "p");
    String jobId = jobService.createJob("proc", "p", null, null, "user", null);
    jobService.claimJob(jobId, "w2");

    assertThrows(MolgenisException.class, () -> jobService.deleteJob(jobId));
    assertNotNull(jobService.getJob(jobId), "Job should still exist");
  }

  @Test
  void deleteJob_returnsNullForNonExistent() {
    assertNull(jobService.deleteJob("nonexistent-job-id"));
  }

  @Test
  void getJob_returnsNullForMissingJob() {
    assertNull(jobService.getJob("nonexistent-id"));
  }

  @Test
  void listJobs_filtersByStatus() {
    registerWorker("w3", "proc", "p");
    String j1 = jobService.createJob("proc", "p", null, null, "user", null);
    String j2 = jobService.createJob("proc", "p", null, null, "user", null);
    jobService.claimJob(j1, "w3");

    List<Row> pending = jobService.listJobs("PENDING", null, null, 100, 0);
    List<Row> claimed = jobService.listJobs("CLAIMED", null, null, 100, 0);

    assertTrue(pending.stream().anyMatch(r -> j2.equals(r.getString("id"))));
    assertTrue(claimed.stream().anyMatch(r -> j1.equals(r.getString("id"))));
  }

  @Test
  void listJobs_filtersByProcessorAndProfile() {
    jobService.createJob("embed", "gpu", null, null, null, null);
    jobService.createJob("embed", "cpu", null, null, null, null);
    jobService.createJob("train", "gpu", null, null, null, null);

    List<Row> embedGpu = jobService.listJobs("PENDING", "embed", "gpu", 100, 0);
    assertEquals(1, embedGpu.size());

    List<Row> allEmbed = jobService.listJobs("PENDING", "embed", null, 100, 0);
    assertEquals(2, allEmbed.size());
  }

  @Test
  void listJobs_supportsPagination() {
    for (int i = 0; i < 5; i++) {
      jobService.createJob("paginate", "p", null, null, null, null);
    }

    List<Row> page1 = jobService.listJobs("PENDING", "paginate", null, 2, 0);
    List<Row> page2 = jobService.listJobs("PENDING", "paginate", null, 2, 2);
    assertEquals(2, page1.size());
    assertEquals(2, page2.size());
    assertNotEquals(page1.get(0).getString("id"), page2.get(0).getString("id"));
  }

  @Test
  void countJobs_matchesListSize() {
    jobService.createJob("count", "p", null, null, null, null);
    jobService.createJob("count", "p", null, null, null, null);

    int count = jobService.countJobs("PENDING", "count", null);
    assertEquals(2, count);
  }

  @Test
  void listPendingJobs_defaultsToStatusPending() {
    jobService.createJob("pending-test", "p", null, null, null, null);
    List<Row> pending = jobService.listPendingJobs("pending-test", null);
    assertFalse(pending.isEmpty());
  }

  @Test
  void transitionJob_returnsNullForNonExistentJob() {
    Row result =
        jobService.transitionJob(
            "nonexistent", HpcJobStatus.CLAIMED, TransitionParams.of("w", "detail"));
    assertNull(result);
  }

  @Test
  void claimJob_rejectsWhenWorkerAtCapacity() {
    workerService.registerOrHeartbeat(
        "cap-worker",
        "host",
        List.of(Map.of("processor", "cap-proc", "profile", "p", "max_concurrent_jobs", 1)));

    String j1 = jobService.createJob("cap-proc", "p", null, null, null, null);
    String j2 = jobService.createJob("cap-proc", "p", null, null, null, null);

    assertTrue(jobService.claimJob(j1, "cap-worker").isSuccess());
    ClaimResult r2 = jobService.claimJob(j2, "cap-worker");
    assertFalse(r2.isSuccess());
    assertEquals(ClaimResult.ClaimOutcome.CAPACITY_EXCEEDED, r2.outcome());
  }

  @Test
  void extractArtifactIds_handlesVariousFormats() {
    // Array of strings
    List<String> fromStrings = JobService.extractArtifactIds(List.of("id1", "id2"));
    assertEquals(List.of("id1", "id2"), fromStrings);

    // Array of objects
    List<String> fromObjects =
        JobService.extractArtifactIds(
            List.of(Map.of("artifact_id", "id3"), Map.of("artifact_id", "id4")));
    assertEquals(List.of("id3", "id4"), fromObjects);

    // Null
    assertTrue(JobService.extractArtifactIds(null).isEmpty());
  }

  private void registerWorker(String workerId, String processor, String profile) {
    workerService.registerOrHeartbeat(
        workerId,
        workerId + ".node",
        List.of(Map.of("processor", processor, "profile", profile, "max_concurrent_jobs", 10)));
  }
}
