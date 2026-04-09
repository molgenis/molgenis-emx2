package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.hpc.HpcApiUtils.requestId;
import static org.molgenis.emx2.hpc.HpcFields.*;
import static org.molgenis.emx2.hpc.JobResponseMapper.parseOptionalBoundedString;
import static org.molgenis.emx2.hpc.JobResponseMapper.parseOptionalProgress;
import static org.molgenis.emx2.hpc.JobResponseMapper.requireSubmitterOrManager;
import static org.molgenis.emx2.hpc.JobResponseMapper.resolveWorkerId;
import static org.molgenis.emx2.hpc.protocol.InputValidator.parseIntParam;
import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.molgenis.emx2.hpc.service.TransitionParams;

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
  private final JobResponseMapper mapper;

  public JobsApi(JobService jobService, ArtifactService artifactService) {
    this.jobService = jobService;
    this.mapper = new JobResponseMapper(artifactService);
  }

  /** POST /api/hpc/jobs — create a new job in PENDING status. */
  @SuppressWarnings("unchecked")
  public void createJob(Context ctx) throws JsonProcessingException {
    Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
    String processor = (String) body.get(PROCESSOR);
    String profile = (String) body.get(PROFILE);
    Object parameters =
        body.get(PARAMETERS) != null ? MAPPER.valueToTree(body.get(PARAMETERS)) : null;
    Object inputs = body.get(INPUTS) != null ? MAPPER.valueToTree(body.get(INPUTS)) : null;
    String submitUser =
        "USER".equals(ctx.attribute("hpcAuthMethod")) ? ctx.attribute("hpcAuthUser") : null;
    Integer timeoutSeconds =
        body.get(TIMEOUT_SECONDS) != null ? ((Number) body.get(TIMEOUT_SECONDS)).intValue() : null;

    InputValidator.requireString(processor, PROCESSOR);
    InputValidator.optionalString(profile, PROFILE);

    String jobId =
        jobService.createJob(processor, profile, parameters, inputs, submitUser, timeoutSeconds);
    Map<String, Object> response = new LinkedHashMap<>();
    response.put(ID, jobId);
    response.put(STATUS, HpcJobStatus.PENDING.name());
    response.put(LINKS, LinkBuilder.forJob(jobId, HpcJobStatus.PENDING));

    ctx.status(201);
    ctx.json(response);
  }

  /** GET /api/hpc/jobs/{id} — get job details with HATEOAS links. */
  public void getJob(Context ctx) {
    String jobId = ctx.pathParam(ID);
    InputValidator.requireUuid(jobId, ID);
    Row job = jobService.getJob(jobId);
    if (job == null) {
      throw HpcException.notFound("Job " + jobId + " not found", requestId(ctx));
    }
    ctx.json(mapper.jobToResponse(job));
  }

  /** GET /api/hpc/jobs — list jobs with optional filtering and pagination. */
  public void listJobs(Context ctx) {
    String status = ctx.queryParam(STATUS);
    String processor = ctx.queryParam(PROCESSOR);
    String profile = ctx.queryParam(PROFILE);
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
    response.put(LINKS, Map.of("self", Map.of("href", "/api/hpc/jobs", "method", "GET")));

    ctx.header("X-Total-Count", String.valueOf(totalCount));
    ctx.json(response);
  }

  /**
   * POST /api/hpc/jobs/{id}/claim — atomic claim with capability verification. Returns 200 on
   * success, 409 if already claimed or capability mismatch, 404 if not found.
   */
  @SuppressWarnings("unchecked")
  public void claimJob(Context ctx) throws JsonProcessingException {
    String jobId = ctx.pathParam(ID);
    InputValidator.requireUuid(jobId, ID);

    Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
    String workerId = resolveWorkerId(ctx, (String) body.get(WORKER_ID));

    ClaimResult result = jobService.claimJob(jobId, workerId);
    if (result.isSuccess()) {
      ctx.status(200);
      ctx.json(mapper.jobToResponse(result.job()));
      return;
    }

    if (result.outcome() == ClaimResult.ClaimOutcome.CAPABILITY_MISMATCH) {
      Row existing = jobService.getJob(jobId);
      String processor = existing != null ? existing.getString(PROCESSOR) : "?";
      String profile = existing != null ? existing.getString(PROFILE) : "?";
      throw HpcException.conflict(
          "Worker "
              + workerId
              + " does not have a registered capability for processor="
              + processor
              + " profile="
              + profile,
          requestId(ctx));
    }

    if (result.outcome() == ClaimResult.ClaimOutcome.CAPACITY_EXCEEDED) {
      throw HpcException.conflict(
          "Worker " + workerId + " has reached its max_concurrent_jobs limit", requestId(ctx));
    }

    // NOT_PENDING: either not found or already claimed
    Row existing = jobService.getJob(jobId);
    if (existing == null) {
      throw HpcException.notFound("Job " + jobId + " not found", requestId(ctx));
    }
    throw HpcException.conflict(
        "Job " + jobId + " is not in PENDING status (current: " + existing.getString(STATUS) + ")",
        requestId(ctx));
  }

  /**
   * POST /api/hpc/jobs/{id}/transition — report a status transition.
   *
   * <p>Request body: {"status": "SUBMITTED", "worker_id": "...", "detail": "sbatch id 12345",
   * "slurm_job_id": "12345", "phase": "staging", "message": "step 2/5", "progress": 0.4}
   */
  @SuppressWarnings("unchecked")
  public void transitionJob(Context ctx) throws JsonProcessingException {
    String jobId = ctx.pathParam(ID);
    InputValidator.requireUuid(jobId, ID);

    Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
    String targetStatusStr = (String) body.get(STATUS);
    if (targetStatusStr == null) {
      throw HpcException.badRequest("status is required", requestId(ctx));
    }

    HpcJobStatus targetStatus;
    try {
      targetStatus = HpcJobStatus.valueOf(targetStatusStr);
    } catch (IllegalArgumentException e) {
      throw HpcException.badRequest("Invalid status: " + targetStatusStr, requestId(ctx));
    }

    TransitionParams params = parseTransitionParams(ctx, body);
    Row result = jobService.transitionJob(jobId, targetStatus, params);
    if (result == null) {
      Row existing = jobService.getJob(jobId);
      if (existing == null) {
        throw HpcException.notFound("Job " + jobId + " not found", requestId(ctx));
      }
      throw HpcException.conflict(
          "Cannot transition job "
              + jobId
              + " from "
              + existing.getString(STATUS)
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
  public void completeJob(Context ctx) throws JsonProcessingException {
    String jobId = ctx.pathParam(ID);
    InputValidator.requireUuid(jobId, ID);

    Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
    String targetStatusStr = (String) body.get(STATUS);
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

    TransitionParams params = parseTransitionParams(ctx, body);
    Row result = jobService.transitionJob(jobId, targetStatus, params);
    if (result == null) {
      Row existing = jobService.getJob(jobId);
      if (existing == null) {
        throw HpcException.notFound("Job " + jobId + " not found", requestId(ctx));
      }
      throw HpcException.conflict(
          "Cannot complete job "
              + jobId
              + " from "
              + existing.getString(STATUS)
              + " to "
              + targetStatusStr,
          requestId(ctx));
    }

    ctx.status(200);
    ctx.json(mapper.jobToResponse(result));
  }

  /** POST /api/hpc/jobs/{id}/cancel — convenience endpoint for cancellation. */
  public void cancelJob(Context ctx) {
    String jobId = ctx.pathParam(ID);
    InputValidator.requireUuid(jobId, ID);
    Row existing = jobService.getJob(jobId);
    if (existing == null) {
      throw HpcException.notFound("Job " + jobId + " not found", requestId(ctx));
    }
    requireSubmitterOrManager(ctx, existing, "cancel");

    // Cancel does not update worker_id — cancellation can come from any
    // authenticated caller (UI user, API client), not just the assigned worker.
    Row result =
        jobService.transitionJob(
            jobId, HpcJobStatus.CANCELLED, TransitionParams.of(null, "Cancelled via API"));
    if (result == null) {
      throw HpcException.conflict(
          "Cannot cancel job " + jobId + " in status " + existing.getString(STATUS),
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
    String jobId = ctx.pathParam(ID);
    InputValidator.requireUuid(jobId, ID);
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
    String jobId = ctx.pathParam(ID);
    InputValidator.requireUuid(jobId, ID);

    List<Row> transitions = jobService.getTransitions(jobId);

    List<Map<String, Object>> items =
        transitions.stream()
            .map(
                t -> {
                  Map<String, Object> m = new LinkedHashMap<>();
                  m.put(ID, t.getString(ID));
                  m.put(JOB_ID, t.getString(JOB_ID));
                  m.put(FROM_STATUS, t.getString(FROM_STATUS));
                  m.put(TO_STATUS, t.getString(TO_STATUS));
                  m.put(TIMESTAMP, t.getString(TIMESTAMP));
                  m.put(WORKER_ID, t.getString(WORKER_ID));
                  m.put(DETAIL, t.getString(DETAIL));
                  m.put(PHASE, t.getString(PHASE));
                  m.put(MESSAGE, t.getString(MESSAGE));
                  m.put(PROGRESS, t.getDecimal(PROGRESS));
                  return m;
                })
            .toList();

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("items", items);
    response.put("count", items.size());
    ctx.json(response);
  }

  private static TransitionParams parseTransitionParams(Context ctx, Map<String, Object> body) {
    return new TransitionParams(
        resolveWorkerId(ctx, (String) body.get(WORKER_ID)),
        (String) body.get(DETAIL),
        (String) body.get(SLURM_JOB_ID),
        (String) body.get(OUTPUT_ARTIFACT_ID),
        (String) body.get(LOG_ARTIFACT_ID),
        parseOptionalBoundedString(
            body.get(PHASE), PHASE, JobResponseMapper.maxProgressPhaseLength()),
        parseOptionalBoundedString(
            body.get(MESSAGE), MESSAGE, JobResponseMapper.maxProgressMessageLength()),
        parseOptionalProgress(body.get(PROGRESS)));
  }
}
