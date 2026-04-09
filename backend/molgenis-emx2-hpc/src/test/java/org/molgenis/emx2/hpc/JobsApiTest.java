package org.molgenis.emx2.hpc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.model.HpcJobStatus;
import org.molgenis.emx2.hpc.service.ArtifactService;
import org.molgenis.emx2.hpc.service.ClaimResult;
import org.molgenis.emx2.hpc.service.JobService;
import org.molgenis.emx2.hpc.service.TransitionParams;

class JobsApiTest {

  private JobService jobService;
  private ArtifactService artifactService;
  private JobsApi jobsApi;
  private Context ctx;

  private static final String JOB_ID = UUID.randomUUID().toString();

  @BeforeEach
  void setUp() {
    jobService = mock(JobService.class);
    artifactService = mock(ArtifactService.class);
    jobsApi = new JobsApi(jobService, artifactService);
    ctx = mock(Context.class);
    // Default: HMAC auth (worker context) — bypasses submitter checks
    when(ctx.attribute("hpcAuthMethod")).thenReturn("HMAC");
    when(ctx.header("X-Request-Id")).thenReturn("req-1");
    when(ctx.header("X-Worker-Id")).thenReturn("w1");
  }

  // ── createJob ──────────────────────────────────────────────────────────────

  @Test
  void createJob_success() throws Exception {
    when(ctx.body()).thenReturn("{\"processor\":\"text\",\"profile\":\"gpu\"}");
    when(ctx.attribute("hpcAuthMethod")).thenReturn("USER");
    when(ctx.attribute("hpcAuthUser")).thenReturn("admin");
    when(jobService.createJob(eq("text"), eq("gpu"), isNull(), isNull(), eq("admin"), isNull()))
        .thenReturn(JOB_ID);

    jobsApi.createJob(ctx);

    verify(ctx).status(201);
    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return JOB_ID.equals(map.get("id")) && "PENDING".equals(map.get("status"));
                }));
  }

  @Test
  void createJob_missingProcessor_throws() {
    when(ctx.body()).thenReturn("{\"profile\":\"gpu\"}");
    assertThrows(IllegalArgumentException.class, () -> jobsApi.createJob(ctx));
  }

  @Test
  void createJob_hmacAuth_nullSubmitUser() throws Exception {
    when(ctx.body()).thenReturn("{\"processor\":\"text\"}");
    when(ctx.attribute("hpcAuthMethod")).thenReturn("HMAC");
    when(jobService.createJob(eq("text"), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(JOB_ID);

    jobsApi.createJob(ctx);

    verify(jobService).createJob(eq("text"), isNull(), isNull(), isNull(), isNull(), isNull());
    verify(ctx).status(201);
  }

  @Test
  void createJob_withTimeoutSeconds() throws Exception {
    when(ctx.body()).thenReturn("{\"processor\":\"text\",\"timeout_seconds\":3600}");
    when(jobService.createJob(eq("text"), isNull(), isNull(), isNull(), isNull(), eq(3600)))
        .thenReturn(JOB_ID);

    jobsApi.createJob(ctx);

    verify(jobService).createJob(eq("text"), isNull(), isNull(), isNull(), isNull(), eq(3600));
    verify(ctx).status(201);
  }

  // ── getJob ─────────────────────────────────────────────────────────────────

  @Test
  void getJob_found() {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    Row jobRow = buildJobRow(JOB_ID, "PENDING", "text", "gpu");
    when(jobService.getJob(JOB_ID)).thenReturn(jobRow);

    jobsApi.getJob(ctx);

    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return JOB_ID.equals(map.get("id"));
                }));
  }

  @Test
  void getJob_notFound_throws() {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(jobService.getJob(JOB_ID)).thenReturn(null);

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.getJob(ctx));
    assertEquals(404, ex.getStatus());
  }

  // ── listJobs ───────────────────────────────────────────────────────────────

  @Test
  void listJobs_returnsPagedResponse() {
    when(ctx.queryParam("status")).thenReturn("PENDING");
    when(ctx.queryParam("processor")).thenReturn(null);
    when(ctx.queryParam("profile")).thenReturn(null);
    when(ctx.queryParam("limit")).thenReturn("10");
    when(ctx.queryParam("offset")).thenReturn("0");

    Row job = buildJobRow(JOB_ID, "PENDING", "text", "gpu");
    when(jobService.listJobs("PENDING", null, null, 10, 0)).thenReturn(List.of(job));
    when(jobService.countJobs("PENDING", null, null)).thenReturn(1);

    jobsApi.listJobs(ctx);

    verify(ctx).header("X-Total-Count", "1");
    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return Integer.valueOf(1).equals(map.get("count"))
                      && Integer.valueOf(1).equals(map.get("total_count"));
                }));
  }

  @Test
  void listJobs_defaultLimitAndOffset() {
    when(ctx.queryParam("status")).thenReturn(null);
    when(ctx.queryParam("processor")).thenReturn(null);
    when(ctx.queryParam("profile")).thenReturn(null);
    when(ctx.queryParam("limit")).thenReturn(null);
    when(ctx.queryParam("offset")).thenReturn(null);
    when(jobService.listJobs(null, null, null, 100, 0)).thenReturn(List.of());
    when(jobService.countJobs(null, null, null)).thenReturn(0);

    jobsApi.listJobs(ctx);

    verify(jobService).listJobs(null, null, null, 100, 0);
  }

  // ── claimJob ───────────────────────────────────────────────────────────────

  @Test
  void claimJob_success() throws Exception {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(ctx.body()).thenReturn("{\"worker_id\":\"w1\"}");
    Row claimedRow = buildJobRow(JOB_ID, "CLAIMED", "text", "gpu");
    when(jobService.claimJob(JOB_ID, "w1")).thenReturn(ClaimResult.success(claimedRow));

    jobsApi.claimJob(ctx);

    verify(ctx).status(200);
    verify(ctx).json(any());
  }

  @Test
  void claimJob_notPending_existingJob_throws409() throws Exception {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(ctx.body()).thenReturn("{\"worker_id\":\"w1\"}");
    when(jobService.claimJob(JOB_ID, "w1")).thenReturn(ClaimResult.notPending());
    Row existing = buildJobRow(JOB_ID, "CLAIMED", "text", "gpu");
    when(jobService.getJob(JOB_ID)).thenReturn(existing);

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.claimJob(ctx));
    assertEquals(409, ex.getStatus());
    assertTrue(ex.getMessage().contains("not in PENDING status"));
  }

  @Test
  void claimJob_notPending_noJob_throws404() throws Exception {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(ctx.body()).thenReturn("{\"worker_id\":\"w1\"}");
    when(jobService.claimJob(JOB_ID, "w1")).thenReturn(ClaimResult.notPending());
    when(jobService.getJob(JOB_ID)).thenReturn(null);

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.claimJob(ctx));
    assertEquals(404, ex.getStatus());
  }

  @Test
  void claimJob_capabilityMismatch_throws409() throws Exception {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(ctx.body()).thenReturn("{\"worker_id\":\"w1\"}");
    when(jobService.claimJob(JOB_ID, "w1")).thenReturn(ClaimResult.capabilityMismatch());
    Row existing = buildJobRow(JOB_ID, "PENDING", "text", "gpu");
    when(jobService.getJob(JOB_ID)).thenReturn(existing);

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.claimJob(ctx));
    assertEquals(409, ex.getStatus());
    assertTrue(ex.getMessage().contains("capability"));
  }

  @Test
  void claimJob_capacityExceeded_throws409() throws Exception {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(ctx.body()).thenReturn("{\"worker_id\":\"w1\"}");
    when(jobService.claimJob(JOB_ID, "w1")).thenReturn(ClaimResult.capacityExceeded());

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.claimJob(ctx));
    assertEquals(409, ex.getStatus());
    assertTrue(ex.getMessage().contains("max_concurrent_jobs"));
  }

  // ── transitionJob ──────────────────────────────────────────────────────────

  @Test
  void transitionJob_success() throws Exception {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(ctx.body())
        .thenReturn("{\"status\":\"SUBMITTED\",\"worker_id\":\"w1\",\"detail\":\"sbatch 123\"}");
    Row resultRow = buildJobRow(JOB_ID, "SUBMITTED", "text", "gpu");
    when(jobService.transitionJob(
            eq(JOB_ID), eq(HpcJobStatus.SUBMITTED), any(TransitionParams.class)))
        .thenReturn(resultRow);

    jobsApi.transitionJob(ctx);

    verify(ctx).status(200);
    verify(ctx).json(any());
  }

  @Test
  void transitionJob_missingStatus_throws400() {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(ctx.body()).thenReturn("{\"worker_id\":\"w1\"}");

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.transitionJob(ctx));
    assertEquals(400, ex.getStatus());
    assertTrue(ex.getMessage().contains("status is required"));
  }

  @Test
  void transitionJob_invalidStatus_throws400() {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(ctx.body()).thenReturn("{\"status\":\"BOGUS\",\"worker_id\":\"w1\"}");

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.transitionJob(ctx));
    assertEquals(400, ex.getStatus());
    assertTrue(ex.getMessage().contains("Invalid status"));
  }

  @Test
  void transitionJob_notAllowed_existingJob_throws409() throws Exception {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(ctx.body()).thenReturn("{\"status\":\"COMPLETED\",\"worker_id\":\"w1\"}");
    when(jobService.transitionJob(
            eq(JOB_ID), eq(HpcJobStatus.COMPLETED), any(TransitionParams.class)))
        .thenReturn(null);
    Row existing = buildJobRow(JOB_ID, "PENDING", "text", "gpu");
    when(jobService.getJob(JOB_ID)).thenReturn(existing);

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.transitionJob(ctx));
    assertEquals(409, ex.getStatus());
    assertTrue(ex.getMessage().contains("Cannot transition"));
  }

  @Test
  void transitionJob_notAllowed_noJob_throws404() throws Exception {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(ctx.body()).thenReturn("{\"status\":\"COMPLETED\",\"worker_id\":\"w1\"}");
    when(jobService.transitionJob(
            eq(JOB_ID), eq(HpcJobStatus.COMPLETED), any(TransitionParams.class)))
        .thenReturn(null);
    when(jobService.getJob(JOB_ID)).thenReturn(null);

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.transitionJob(ctx));
    assertEquals(404, ex.getStatus());
  }

  // ── completeJob ────────────────────────────────────────────────────────────

  @Test
  void completeJob_success() throws Exception {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(ctx.body()).thenReturn("{\"status\":\"COMPLETED\",\"worker_id\":\"w1\"}");
    Row resultRow = buildJobRow(JOB_ID, "COMPLETED", "text", "gpu");
    when(jobService.transitionJob(
            eq(JOB_ID), eq(HpcJobStatus.COMPLETED), any(TransitionParams.class)))
        .thenReturn(resultRow);

    jobsApi.completeJob(ctx);

    verify(ctx).status(200);
  }

  @Test
  void completeJob_nonTerminalStatus_throws400() {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(ctx.body()).thenReturn("{\"status\":\"SUBMITTED\",\"worker_id\":\"w1\"}");

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.completeJob(ctx));
    assertEquals(400, ex.getStatus());
    assertTrue(ex.getMessage().contains("terminal"));
  }

  @Test
  void completeJob_missingStatus_throws400() {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(ctx.body()).thenReturn("{\"worker_id\":\"w1\"}");

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.completeJob(ctx));
    assertEquals(400, ex.getStatus());
  }

  // ── cancelJob ──────────────────────────────────────────────────────────────

  @Test
  void cancelJob_success() {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    Row existing = buildJobRow(JOB_ID, "PENDING", "text", "gpu");
    when(jobService.getJob(JOB_ID)).thenReturn(existing);
    Row cancelledRow = buildJobRow(JOB_ID, "CANCELLED", "text", "gpu");
    when(jobService.transitionJob(
            eq(JOB_ID), eq(HpcJobStatus.CANCELLED), any(TransitionParams.class)))
        .thenReturn(cancelledRow);

    jobsApi.cancelJob(ctx);

    verify(ctx).status(200);
    verify(ctx).json(any());
  }

  @Test
  void cancelJob_notFound_throws404() {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(jobService.getJob(JOB_ID)).thenReturn(null);

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.cancelJob(ctx));
    assertEquals(404, ex.getStatus());
  }

  @Test
  void cancelJob_transitionFails_throws409() {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    Row existing = buildJobRow(JOB_ID, "COMPLETED", "text", "gpu");
    when(jobService.getJob(JOB_ID)).thenReturn(existing);
    when(jobService.transitionJob(
            eq(JOB_ID), eq(HpcJobStatus.CANCELLED), any(TransitionParams.class)))
        .thenReturn(null);

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.cancelJob(ctx));
    assertEquals(409, ex.getStatus());
  }

  // ── deleteJob ──────────────────────────────────────────────────────────────

  @Test
  void deleteJob_success() {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    Row existing = buildJobRow(JOB_ID, "COMPLETED", "text", "gpu");
    when(jobService.getJob(JOB_ID)).thenReturn(existing);
    when(jobService.deleteJob(JOB_ID)).thenReturn(existing);

    jobsApi.deleteJob(ctx);

    verify(ctx).status(204);
  }

  @Test
  void deleteJob_notFound_throws404() {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(jobService.getJob(JOB_ID)).thenReturn(null);

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.deleteJob(ctx));
    assertEquals(404, ex.getStatus());
  }

  @Test
  void deleteJob_serviceReturnsNull_throws404() {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    Row existing = buildJobRow(JOB_ID, "COMPLETED", "text", "gpu");
    when(jobService.getJob(JOB_ID)).thenReturn(existing);
    when(jobService.deleteJob(JOB_ID)).thenReturn(null);

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.deleteJob(ctx));
    assertEquals(404, ex.getStatus());
  }

  @Test
  void deleteJob_molgenisException_throws409() {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    Row existing = buildJobRow(JOB_ID, "RUNNING", "text", "gpu");
    when(jobService.getJob(JOB_ID)).thenReturn(existing);
    when(jobService.deleteJob(JOB_ID))
        .thenThrow(new org.molgenis.emx2.MolgenisException("Job is not terminal"));

    HpcException ex = assertThrows(HpcException.class, () -> jobsApi.deleteJob(ctx));
    assertEquals(409, ex.getStatus());
  }

  // ── getTransitions ─────────────────────────────────────────────────────────

  @Test
  void getTransitions_returnsList() {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    Row transition = new Row();
    transition.set("id", "t-1");
    transition.set("job_id", JOB_ID);
    transition.set("from_status", "PENDING");
    transition.set("to_status", "CLAIMED");
    transition.set("timestamp", "2025-01-01T00:00:00");
    transition.set("worker_id", "w1");
    transition.set("detail", "claimed");
    transition.set("phase", null);
    transition.set("message", null);
    transition.setDecimal("progress", null);
    when(jobService.getTransitions(JOB_ID)).thenReturn(List.of(transition));

    jobsApi.getTransitions(ctx);

    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return Integer.valueOf(1).equals(map.get("count"));
                }));
  }

  @Test
  void getTransitions_emptyList() {
    when(ctx.pathParam("id")).thenReturn(JOB_ID);
    when(jobService.getTransitions(JOB_ID)).thenReturn(List.of());

    jobsApi.getTransitions(ctx);

    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return Integer.valueOf(0).equals(map.get("count"));
                }));
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private static Row buildJobRow(String id, String status, String processor, String profile) {
    Row row = new Row();
    row.set("id", id);
    row.set("status", status);
    row.set("processor", processor);
    row.set("profile", profile);
    row.set("worker_id", null);
    row.set("slurm_job_id", null);
    row.set("submit_user", null);
    row.set("output_artifact_id", null);
    row.set("log_artifact_id", null);
    row.set("timeout_seconds", null);
    row.set("created_at", "2025-01-01T00:00:00");
    row.set("claimed_at", null);
    row.set("submitted_at", null);
    row.set("started_at", null);
    row.set("completed_at", null);
    row.set("phase", null);
    row.set("message", null);
    row.setDecimal("progress", null);
    return row;
  }
}
