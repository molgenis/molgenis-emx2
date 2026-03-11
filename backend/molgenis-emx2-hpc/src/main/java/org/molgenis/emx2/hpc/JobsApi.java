package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.hpc.HpcApiUtils.requestId;
import static org.molgenis.emx2.hpc.JobResponseMapper.parseOptionalBoundedString;
import static org.molgenis.emx2.hpc.JobResponseMapper.parseOptionalProgress;
import static org.molgenis.emx2.hpc.JobResponseMapper.requireSubmitterOrManager;
import static org.molgenis.emx2.hpc.JobResponseMapper.resolveWorkerId;
import static org.molgenis.emx2.hpc.protocol.InputValidator.parseIntParam;
import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import io.javalin.http.Context;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.model.HpcJobStatus;
import org.molgenis.emx2.hpc.protocol.InputValidator;
import org.molgenis.emx2.hpc.protocol.LinkBuilder;
import org.molgenis.emx2.hpc.service.ArtifactService;
import org.molgenis.emx2.hpc.service.ClaimResult;
import org.molgenis.emx2.hpc.service.JobService;
import org.molgenis.emx2.hpc.service.WorkerService;

/**
 * Job lifecycle endpoints:
 *
 * <ul>
 *   <li>POST /api/hpc/jobs — create a new job
 *   <li>GET /api/hpc/jobs/{id} — get job details
 *   <li>GET /api/hpc/jobs — list jobs (filter by status, processor, profile)
 *   <li>POST /api/hpc/jobs/{id}/claim — atomic claim by a worker
 *   <li>POST /api/hpc/jobs/{id}/transition — report status transition
 *   <li>POST /api/hpc/jobs/{id}/cancel — cancel a job
 *   <li>GET /api/hpc/jobs/{id}/transitions — audit trail
 * </ul>
 */
public class JobsApi {

  private final JobService jobService;
  private final ArtifactService artifactService;
  private final WorkerService workerService;
  private final JobResponseMapper mapper;

  public JobsApi(
      JobService jobService, ArtifactService artifactService, WorkerService workerService) {
    this.jobService = jobService;
    this.artifactService = artifactService;
    this.workerService = workerService;
    this.mapper = new JobResponseMapper(artifactService);
  }

  /** POST /api/hpc/jobs — create a new job in PENDING status. */
  @SuppressWarnings("unchecked")
  public void createJob(Context ctx) throws Exception {
    Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
    String processor = (String) body.get("processor");
    String profile = (String) body.get("profile");
    Object parameters =
        body.get("parameters") != null ? MAPPER.valueToTree(body.get("parameters")) : null;
    Object inputs = body.get("inputs") != null ? MAPPER.valueToTree(body.get("inputs")) : null;
    String submitUser =
        "USER".equals(ctx.attribute("hpcAuthMethod")) ? ctx.attribute("hpcAuthUser") : null;
    Integer timeoutSeconds =
        body.get("timeout_seconds") != null
            ? ((Number) body.get("timeout_seconds")).intValue()
            : null;

    InputValidator.requireString(processor, "processor");
    InputValidator.optionalString(profile, "profile");

    String jobId =
        jobService.createJob(processor, profile, parameters, inputs, submitUser, timeoutSeconds);
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", jobId);
    response.put("status", HpcJobStatus.PENDING.name());
    response.put("_links", LinkBuilder.forJob(jobId, HpcJobStatus.PENDING));

    ctx.status(201);
    ctx.json(response);
  }

  /** GET /api/hpc/jobs/{id} — get job details with HATEOAS links. */
  public void getJob(Context ctx) {
    String jobId = ctx.pathParam("id");
    InputValidator.requireUuid(jobId, "id");
    Row job = jobService.getJob(jobId);
    if (job == null) {
      throw HpcException.notFound("Job " + jobId + " not found", requestId(ctx));
    }
    ctx.json(mapper.jobToResponse(job));
  }

  /** GET /api/hpc/jobs — list jobs with optional filtering and pagination. */
  public void listJobs(Context ctx) {
    // Expire stale jobs, artifacts, and workers before listing
    jobService.expireStaleJobs();
    artifactService.expireStaleArtifacts();
    workerService.expireStaleWorkers();

    String status = ctx.queryParam("status");
    String processor = ctx.queryParam("processor");
    String profile = ctx.queryParam("profile");
    int limit = parseIntParam(ctx.queryParam("limit"), 100);
    int offset = parseIntParam(ctx.queryParam("offset"), 0);

    List<Row> jobs = jobService.listJobs(status, processor, profile, limit, offset);
    int totalCount = jobService.countJobs(status, processor, profile);

    List<Map<String, Object>> items = jobs.stream().map(mapper::jobToResponse).toList();
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("items", items);
    response.put("count", items.size());
    response.put("total_count", totalCount);
    response.put("limit", limit);
    response.put("offset", offset);
    response.put("_links", Map.of("self", Map.of("href", "/api/hpc/jobs", "method", "GET")));

    ctx.header("X-Total-Count", String.valueOf(totalCount));
    ctx.json(response);
  }

  /**
   * POST /api/hpc/jobs/{id}/claim — atomic claim with capability verification. Returns 200 on
   * success, 409 if already claimed or capability mismatch, 404 if not found.
   */
  @SuppressWarnings("unchecked")
  public void claimJob(Context ctx) throws Exception {
    String jobId = ctx.pathParam("id");
    InputValidator.requireUuid(jobId, "id");

    Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
    String workerId = resolveWorkerId(ctx, (String) body.get("worker_id"));

    ClaimResult result = jobService.claimJob(jobId, workerId);
    if (result.isSuccess()) {
      ctx.status(200);
      ctx.json(mapper.jobToResponse(result.job()));
      return;
    }

    if (result.outcome() == ClaimResult.ClaimOutcome.CAPABILITY_MISMATCH) {
      Row existing = jobService.getJob(jobId);
      String processor = existing != null ? existing.getString("processor") : "?";
      String profile = existing != null ? existing.getString("profile") : "?";
      throw HpcException.conflict(
          "Worker "
              + workerId
              + " does not have a registered capability for processor="
              + processor
              + " profile="
              + profile,
          requestId(ctx));
    }

    // NOT_PENDING: either not found or already claimed
    Row existing = jobService.getJob(jobId);
    if (existing == null) {
      throw HpcException.notFound("Job " + jobId + " not found", requestId(ctx));
    }
    throw HpcException.conflict(
        "Job "
            + jobId
            + " is not in PENDING status (current: "
            + existing.getString("status")
            + ")",
        requestId(ctx));
  }

  /**
   * POST /api/hpc/jobs/{id}/transition — report a status transition.
   *
   * <p>Request body: {"status": "SUBMITTED", "worker_id": "...", "detail": "sbatch id 12345",
   * "slurm_job_id": "12345", "phase": "staging", "message": "step 2/5", "progress": 0.4}
   */
  @SuppressWarnings("unchecked")
  public void transitionJob(Context ctx) throws Exception {
    String jobId = ctx.pathParam("id");
    InputValidator.requireUuid(jobId, "id");

    Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
    String targetStatusStr = (String) body.get("status");
    String workerId = resolveWorkerId(ctx, (String) body.get("worker_id"));
    String detail = (String) body.get("detail");
    String phase =
        parseOptionalBoundedString(
            body.get("phase"), "phase", JobResponseMapper.maxProgressPhaseLength());
    String message =
        parseOptionalBoundedString(
            body.get("message"), "message", JobResponseMapper.maxProgressMessageLength());
    Double progress = parseOptionalProgress(body.get("progress"));

    if (targetStatusStr == null) {
      throw HpcException.badRequest("status is required", requestId(ctx));
    }

    HpcJobStatus targetStatus;
    try {
      targetStatus = HpcJobStatus.valueOf(targetStatusStr);
    } catch (IllegalArgumentException e) {
      throw HpcException.badRequest("Invalid status: " + targetStatusStr, requestId(ctx));
    }

    String slurmJobId = (String) body.get("slurm_job_id");
    String outputArtifactId = (String) body.get("output_artifact_id");
    String logArtifactId = (String) body.get("log_artifact_id");
    Row result =
        jobService.transitionJob(
            jobId,
            targetStatus,
            workerId,
            detail,
            slurmJobId,
            outputArtifactId,
            logArtifactId,
            phase,
            message,
            progress);
    if (result == null) {
      Row existing = jobService.getJob(jobId);
      if (existing == null) {
        throw HpcException.notFound("Job " + jobId + " not found", requestId(ctx));
      }
      throw HpcException.conflict(
          "Cannot transition job "
              + jobId
              + " from "
              + existing.getString("status")
              + " to "
              + targetStatusStr,
          requestId(ctx));
    }

    ctx.status(200);
    ctx.json(mapper.jobToResponse(result));
  }

  /**
   * POST /api/hpc/jobs/{id}/complete — atomically complete a job (terminal transition).
   *
   * <p>Restricts target status to terminal states (COMPLETED, FAILED, CANCELLED). Idempotent: if
   * the job is already in the target terminal status, returns 200. This endpoint exists for the
   * daemon's crash-recovery flow: artifacts are uploaded in phases, and this single call finalizes
   * the job with all artifact references.
   */
  @SuppressWarnings("unchecked")
  public void completeJob(Context ctx) throws Exception {
    String jobId = ctx.pathParam("id");
    InputValidator.requireUuid(jobId, "id");

    Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
    String targetStatusStr = (String) body.get("status");
    String workerId = resolveWorkerId(ctx, (String) body.get("worker_id"));
    String detail = (String) body.get("detail");
    String phase =
        parseOptionalBoundedString(
            body.get("phase"), "phase", JobResponseMapper.maxProgressPhaseLength());
    String message =
        parseOptionalBoundedString(
            body.get("message"), "message", JobResponseMapper.maxProgressMessageLength());
    Double progress = parseOptionalProgress(body.get("progress"));
    String slurmJobId = (String) body.get("slurm_job_id");
    String outputArtifactId = (String) body.get("output_artifact_id");
    String logArtifactId = (String) body.get("log_artifact_id");

    if (targetStatusStr == null) {
      throw HpcException.badRequest("status is required", requestId(ctx));
    }

    HpcJobStatus targetStatus;
    try {
      targetStatus = HpcJobStatus.valueOf(targetStatusStr);
    } catch (IllegalArgumentException e) {
      throw HpcException.badRequest("Invalid status: " + targetStatusStr, requestId(ctx));
    }

    if (!targetStatus.isTerminal()) {
      throw HpcException.badRequest(
          "Complete endpoint only accepts terminal statuses (COMPLETED, FAILED, CANCELLED), got: "
              + targetStatusStr,
          requestId(ctx));
    }

    Row result =
        jobService.transitionJob(
            jobId,
            targetStatus,
            workerId,
            detail,
            slurmJobId,
            outputArtifactId,
            logArtifactId,
            phase,
            message,
            progress);
    if (result == null) {
      Row existing = jobService.getJob(jobId);
      if (existing == null) {
        throw HpcException.notFound("Job " + jobId + " not found", requestId(ctx));
      }
      throw HpcException.conflict(
          "Cannot complete job "
              + jobId
              + " from "
              + existing.getString("status")
              + " to "
              + targetStatusStr,
          requestId(ctx));
    }

    ctx.status(200);
    ctx.json(mapper.jobToResponse(result));
  }

  /** POST /api/hpc/jobs/{id}/cancel — convenience endpoint for cancellation. */
  public void cancelJob(Context ctx) {
    String jobId = ctx.pathParam("id");
    InputValidator.requireUuid(jobId, "id");
    Row existing = jobService.getJob(jobId);
    if (existing == null) {
      throw HpcException.notFound("Job " + jobId + " not found", requestId(ctx));
    }
    requireSubmitterOrManager(ctx, existing, "cancel");

    // Cancel does not update worker_id — cancellation can come from any
    // authenticated caller (UI user, API client), not just the assigned worker.
    Row result =
        jobService.transitionJob(
            jobId,
            HpcJobStatus.CANCELLED,
            null,
            "Cancelled via API",
            null,
            null,
            null,
            null,
            null,
            null);
    if (result == null) {
      throw HpcException.conflict(
          "Cannot cancel job " + jobId + " in status " + existing.getString("status"),
          requestId(ctx));
    }

    ctx.status(200);
    ctx.json(mapper.jobToResponse(result));
  }

  /**
   * DELETE /api/hpc/jobs/{id} — delete a job and its transitions. The job MUST be in a terminal
   * state; returns 409 if not.
   */
  public void deleteJob(Context ctx) {
    String jobId = ctx.pathParam("id");
    InputValidator.requireUuid(jobId, "id");
    Row existing = jobService.getJob(jobId);
    if (existing == null) {
      throw HpcException.notFound("Job " + jobId + " not found", requestId(ctx));
    }
    requireSubmitterOrManager(ctx, existing, "delete");

    try {
      Row deleted = jobService.deleteJob(jobId);
      if (deleted == null) {
        throw HpcException.notFound("Job " + jobId + " not found", requestId(ctx));
      }
    } catch (MolgenisException e) {
      throw HpcException.conflict(e.getMessage(), requestId(ctx));
    }

    ctx.status(204);
  }

  /** GET /api/hpc/jobs/{id}/transitions — audit trail. */
  public void getTransitions(Context ctx) {
    String jobId = ctx.pathParam("id");
    InputValidator.requireUuid(jobId, "id");

    List<Row> transitions = jobService.getTransitions(jobId);

    List<Map<String, Object>> items =
        transitions.stream()
            .map(
                t -> {
                  Map<String, Object> m = new LinkedHashMap<>();
                  m.put("id", t.getString("id"));
                  m.put("job_id", t.getString("job_id"));
                  m.put("from_status", t.getString("from_status"));
                  m.put("to_status", t.getString("to_status"));
                  m.put("timestamp", t.getString("timestamp"));
                  m.put("worker_id", t.getString("worker_id"));
                  m.put("detail", t.getString("detail"));
                  m.put("phase", t.getString("phase"));
                  m.put("message", t.getString("message"));
                  m.put("progress", t.getDecimal("progress"));
                  return m;
                })
            .toList();

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("items", items);
    response.put("count", items.size());
    ctx.json(response);
  }
}
