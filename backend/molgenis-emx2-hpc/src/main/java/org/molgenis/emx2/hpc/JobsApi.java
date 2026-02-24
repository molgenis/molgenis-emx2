package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.hpc.protocol.InputValidator.parseIntParam;
import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import io.javalin.http.Context;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.model.HpcJobStatus;
import org.molgenis.emx2.hpc.protocol.HpcHeaders;
import org.molgenis.emx2.hpc.protocol.InputValidator;
import org.molgenis.emx2.hpc.protocol.LinkBuilder;
import org.molgenis.emx2.hpc.protocol.ProblemDetail;
import org.molgenis.emx2.hpc.service.ArtifactService;
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

  public JobsApi(
      JobService jobService, ArtifactService artifactService, WorkerService workerService) {
    this.jobService = jobService;
    this.artifactService = artifactService;
    this.workerService = workerService;
  }

  /** POST /api/hpc/jobs — create a new job in PENDING status. */
  @SuppressWarnings("unchecked")
  public void createJob(Context ctx) {
    try {
      Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
      String processor = (String) body.get("processor");
      String profile = (String) body.get("profile");
      String parameters =
          body.get("parameters") != null ? MAPPER.writeValueAsString(body.get("parameters")) : null;
      String inputs =
          body.get("inputs") != null ? MAPPER.writeValueAsString(body.get("inputs")) : null;
      String submitUser = (String) body.get("submit_user");
      Integer timeoutSeconds =
          body.get("timeout_seconds") != null
              ? ((Number) body.get("timeout_seconds")).intValue()
              : null;

      try {
        InputValidator.requireString(processor, "processor");
        InputValidator.optionalString(profile, "profile");
        InputValidator.optionalString(submitUser, "submit_user");
      } catch (IllegalArgumentException e) {
        ProblemDetail.send(
            ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
        return;
      }

      String jobId =
          jobService.createJob(processor, profile, parameters, inputs, submitUser, timeoutSeconds);
      Map<String, Object> response = new LinkedHashMap<>();
      response.put("id", jobId);
      response.put("status", HpcJobStatus.PENDING.name());
      response.put("_links", LinkBuilder.forJob(jobId, HpcJobStatus.PENDING));

      ctx.status(201);
      ctx.json(response);
    } catch (Exception e) {
      ProblemDetail.send(
          ctx, 500, "Internal Server Error", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
    }
  }

  /** GET /api/hpc/jobs/{id} — get job details with HATEOAS links. */
  public void getJob(Context ctx) {
    String jobId = ctx.pathParam("id");
    try {
      InputValidator.requireUuid(jobId, "id");
    } catch (IllegalArgumentException e) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
    Row job = jobService.getJob(jobId);
    if (job == null) {
      ProblemDetail.send(
          ctx, 404, "Not Found", "Job " + jobId + " not found", ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
    ctx.json(jobToResponse(job));
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

    List<Map<String, Object>> items = jobs.stream().map(this::jobToResponse).toList();
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
   * POST /api/hpc/jobs/{id}/claim — atomic claim. Returns 200 on success, 409 if already claimed,
   * 404 if not found.
   */
  @SuppressWarnings("unchecked")
  public void claimJob(Context ctx) {
    String jobId = ctx.pathParam("id");
    try {
      InputValidator.requireUuid(jobId, "id");
    } catch (IllegalArgumentException e) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
    try {
      Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
      String workerId = (String) body.get("worker_id");

      try {
        InputValidator.requireString(workerId, "worker_id");
      } catch (IllegalArgumentException e) {
        ProblemDetail.send(
            ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
        return;
      }

      Row claimed = jobService.claimJob(jobId, workerId);
      if (claimed == null) {
        // Either not found or already claimed
        Row existing = jobService.getJob(jobId);
        if (existing == null) {
          ProblemDetail.send(
              ctx,
              404,
              "Not Found",
              "Job " + jobId + " not found",
              ctx.header(HpcHeaders.REQUEST_ID));
        } else {
          ProblemDetail.send(
              ctx,
              409,
              "Conflict",
              "Job "
                  + jobId
                  + " is not in PENDING status (current: "
                  + existing.getString("status")
                  + ")",
              ctx.header(HpcHeaders.REQUEST_ID));
        }
        return;
      }

      ctx.status(200);
      ctx.json(jobToResponse(claimed));
    } catch (Exception e) {
      ProblemDetail.send(
          ctx, 500, "Internal Server Error", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
    }
  }

  /**
   * POST /api/hpc/jobs/{id}/transition — report a status transition.
   *
   * <p>Request body: {"status": "SUBMITTED", "worker_id": "...", "detail": "sbatch id 12345",
   * "slurm_job_id": "12345"}
   */
  @SuppressWarnings("unchecked")
  public void transitionJob(Context ctx) {
    String jobId = ctx.pathParam("id");
    try {
      InputValidator.requireUuid(jobId, "id");
    } catch (IllegalArgumentException e) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
    try {
      Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
      String targetStatusStr = (String) body.get("status");
      String workerId = (String) body.get("worker_id");
      String detail = (String) body.get("detail");

      if (targetStatusStr == null) {
        ProblemDetail.send(
            ctx, 400, "Bad Request", "status is required", ctx.header(HpcHeaders.REQUEST_ID));
        return;
      }

      HpcJobStatus targetStatus;
      try {
        targetStatus = HpcJobStatus.valueOf(targetStatusStr);
      } catch (IllegalArgumentException e) {
        ProblemDetail.send(
            ctx,
            400,
            "Bad Request",
            "Invalid status: " + targetStatusStr,
            ctx.header(HpcHeaders.REQUEST_ID));
        return;
      }

      String slurmJobId = (String) body.get("slurm_job_id");
      String outputArtifactId = (String) body.get("output_artifact_id");
      String logArtifactId = (String) body.get("log_artifact_id");
      Row result =
          jobService.transitionJob(
              jobId, targetStatus, workerId, detail, slurmJobId, outputArtifactId, logArtifactId);
      if (result == null) {
        Row existing = jobService.getJob(jobId);
        if (existing == null) {
          ProblemDetail.send(
              ctx,
              404,
              "Not Found",
              "Job " + jobId + " not found",
              ctx.header(HpcHeaders.REQUEST_ID));
        } else {
          ProblemDetail.send(
              ctx,
              409,
              "Conflict",
              "Cannot transition job "
                  + jobId
                  + " from "
                  + existing.getString("status")
                  + " to "
                  + targetStatusStr,
              ctx.header(HpcHeaders.REQUEST_ID));
        }
        return;
      }

      ctx.status(200);
      ctx.json(jobToResponse(result));
    } catch (Exception e) {
      ProblemDetail.send(
          ctx, 500, "Internal Server Error", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
    }
  }

  /** POST /api/hpc/jobs/{id}/cancel — convenience endpoint for cancellation. */
  public void cancelJob(Context ctx) {
    String jobId = ctx.pathParam("id");
    try {
      InputValidator.requireUuid(jobId, "id");
    } catch (IllegalArgumentException e) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }

    // Cancel does not update worker_id — cancellation can come from any
    // authenticated caller (UI user, API client), not just the assigned worker.
    Row result =
        jobService.transitionJob(
            jobId, HpcJobStatus.CANCELLED, null, "Cancelled via API", null, null, null);
    if (result == null) {
      Row existing = jobService.getJob(jobId);
      if (existing == null) {
        ProblemDetail.send(
            ctx,
            404,
            "Not Found",
            "Job " + jobId + " not found",
            ctx.header(HpcHeaders.REQUEST_ID));
      } else {
        ProblemDetail.send(
            ctx,
            409,
            "Conflict",
            "Cannot cancel job " + jobId + " in status " + existing.getString("status"),
            ctx.header(HpcHeaders.REQUEST_ID));
      }
      return;
    }

    ctx.status(200);
    ctx.json(jobToResponse(result));
  }

  /**
   * DELETE /api/hpc/jobs/{id} — delete a job and its transitions. Non-terminal jobs are cancelled
   * first.
   */
  public void deleteJob(Context ctx) {
    String jobId = ctx.pathParam("id");
    try {
      InputValidator.requireUuid(jobId, "id");
    } catch (IllegalArgumentException e) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }

    Row deleted = jobService.deleteJob(jobId);
    if (deleted == null) {
      ProblemDetail.send(
          ctx, 404, "Not Found", "Job " + jobId + " not found", ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }

    ctx.status(204);
  }

  /** GET /api/hpc/jobs/{id}/transitions — audit trail. */
  public void getTransitions(Context ctx) {
    String jobId = ctx.pathParam("id");
    try {
      InputValidator.requireUuid(jobId, "id");
    } catch (IllegalArgumentException e) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
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
                  return m;
                })
            .toList();

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("items", items);
    response.put("count", items.size());
    ctx.json(response);
  }

  private Map<String, Object> enrichArtifactRef(String artifactId) {
    Map<String, Object> ref = new LinkedHashMap<>();
    ref.put("id", artifactId);
    try {
      Row artifact = artifactService.getArtifact(artifactId);
      if (artifact != null) {
        ref.put("name", artifact.getString("name"));
        ref.put("type", artifact.getString("type"));
        ref.put("status", artifact.getString("status"));
      }
    } catch (Exception e) {
      // If artifact lookup fails, return minimal ref
    }
    return ref;
  }

  private Map<String, Object> jobToResponse(Row job) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", job.getString("id"));
    response.put("processor", job.getString("processor"));
    response.put("profile", job.getString("profile"));
    response.put("status", job.getString("status"));
    response.put("worker_id", job.getString("worker_id"));
    response.put("slurm_job_id", job.getString("slurm_job_id"));
    response.put("submit_user", job.getString("submit_user"));

    // Include parameters as parsed JSON if present, raw string otherwise
    String parametersStr = job.getString("parameters");
    if (parametersStr != null) {
      try {
        response.put("parameters", MAPPER.readValue(parametersStr, Object.class));
      } catch (Exception e) {
        response.put("parameters", parametersStr);
      }
    }

    // Enrich inputs: resolve artifact IDs to objects with name, type, status
    String inputsStr = job.getString("inputs");
    if (inputsStr != null) {
      try {
        Object parsed = MAPPER.readValue(inputsStr, Object.class);
        if (parsed instanceof List<?> inputList) {
          List<Object> enriched =
              inputList.stream()
                  .map(
                      item -> {
                        if (item instanceof String artifactId) {
                          return enrichArtifactRef(artifactId);
                        }
                        return item;
                      })
                  .toList();
          response.put("inputs", enriched);
        } else {
          response.put("inputs", parsed);
        }
      } catch (Exception e) {
        response.put("inputs", inputsStr);
      }
    }

    // Enrich output artifact
    String outputArtifactId = job.getString("output_artifact_id");
    if (outputArtifactId != null) {
      response.put("output_artifact", enrichArtifactRef(outputArtifactId));
    }
    response.put("output_artifact_id", outputArtifactId);

    // Enrich log artifact
    String logArtifactId = job.getString("log_artifact_id");
    if (logArtifactId != null) {
      response.put("log_artifact", enrichArtifactRef(logArtifactId));
    }
    response.put("log_artifact_id", logArtifactId);
    response.put("timeout_seconds", job.getInteger("timeout_seconds"));
    response.put("created_at", job.getString("created_at"));
    response.put("claimed_at", job.getString("claimed_at"));
    response.put("submitted_at", job.getString("submitted_at"));
    response.put("started_at", job.getString("started_at"));
    response.put("completed_at", job.getString("completed_at"));

    HpcJobStatus status;
    try {
      status = HpcJobStatus.valueOf(job.getString("status"));
    } catch (Exception e) {
      status = HpcJobStatus.PENDING;
    }
    response.put("_links", LinkBuilder.forJob(job.getString("id"), status));

    return response;
  }
}
