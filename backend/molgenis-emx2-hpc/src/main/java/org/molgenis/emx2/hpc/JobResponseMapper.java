package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import io.javalin.http.Context;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jooq.JSONB;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.model.HpcJobStatus;
import org.molgenis.emx2.hpc.protocol.HpcHeaders;
import org.molgenis.emx2.hpc.protocol.LinkBuilder;
import org.molgenis.emx2.hpc.service.ArtifactService;

/**
 * Serialization helpers for job responses. Constructed with the {@link ArtifactService} dependency
 * needed to enrich artifact references.
 */
class JobResponseMapper {

  private static final int MAX_PROGRESS_PHASE_LENGTH = 100;
  private static final int MAX_PROGRESS_MESSAGE_LENGTH = 500;

  private final ArtifactService artifactService;

  JobResponseMapper(ArtifactService artifactService) {
    this.artifactService = artifactService;
  }

  /** Build a full JSON-ready response map for a job row, including enriched artifact refs. */
  Map<String, Object> jobToResponse(Row job) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", job.getString("id"));
    response.put("processor", job.getString("processor"));
    response.put("profile", job.getString("profile"));
    response.put("status", job.getString("status"));
    response.put("worker_id", job.getString("worker_id"));
    response.put("slurm_job_id", job.getString("slurm_job_id"));
    response.put("submit_user", job.getString("submit_user"));

    JSONB parametersJson = job.getJsonb("parameters");
    if (parametersJson != null) {
      response.put("parameters", parseJsonb(parametersJson, "parameters"));
    }

    Object parsedInputs = parseJsonb(job.getJsonb("inputs"), "inputs");
    if (parsedInputs instanceof List<?> inputList) {
      response.put("inputs", inputList.stream().map(this::enrichInputRef).toList());
    } else if (parsedInputs != null) {
      response.put("inputs", parsedInputs);
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
    response.put("phase", job.getString("phase"));
    response.put("message", job.getString("message"));
    response.put("progress", job.getDecimal("progress"));

    HpcJobStatus status;
    try {
      status = HpcJobStatus.valueOf(job.getString("status"));
    } catch (Exception e) {
      status = HpcJobStatus.PENDING;
    }
    response.put("_links", LinkBuilder.forJob(job.getString("id"), status));

    return response;
  }

  private static Object parseJsonb(JSONB jsonb, String fieldName) {
    if (jsonb == null) {
      return null;
    }
    try {
      return MAPPER.readValue(jsonb.toString(), Object.class);
    } catch (Exception e) {
      throw new MolgenisException("Invalid JSON in HPC field '" + fieldName + "'", e);
    }
  }

  /** Enrich an artifact reference with name, type, and status from the artifact service. */
  Map<String, Object> enrichArtifactRef(String artifactId) {
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

  @SuppressWarnings("unchecked")
  private Object enrichInputRef(Object inputRef) {
    if (inputRef instanceof String artifactId) {
      return enrichArtifactRef(artifactId);
    }
    if (inputRef instanceof Map<?, ?> map) {
      Object artifactIdValue = map.get("artifact_id");
      if (!(artifactIdValue instanceof String)) {
        artifactIdValue = map.get("id");
      }
      if (artifactIdValue instanceof String artifactId) {
        Map<String, Object> enriched = new LinkedHashMap<>((Map<String, Object>) map);
        Map<String, Object> artifact = enrichArtifactRef(artifactId);
        enriched.putIfAbsent("id", artifactId);
        enriched.putIfAbsent("artifact_id", artifactId);
        artifact.forEach(enriched::putIfAbsent);
        return enriched;
      }
    }
    return inputRef;
  }

  /** Parse an optional bounded string from a request body value. */
  static String parseOptionalBoundedString(Object value, String fieldName, int maxLength) {
    if (value == null) {
      return null;
    }
    if (!(value instanceof String s)) {
      throw new IllegalArgumentException(fieldName + " must be a string");
    }
    if (s.length() > maxLength) {
      throw new IllegalArgumentException(
          fieldName + " must be at most " + maxLength + " characters");
    }
    return s;
  }

  /** Parse an optional progress value (0.0–1.0). */
  static Double parseOptionalProgress(Object value) {
    if (value == null) {
      return null;
    }
    if (!(value instanceof Number n)) {
      throw new IllegalArgumentException("progress must be a number between 0.0 and 1.0");
    }
    double parsed = n.doubleValue();
    if (Double.isNaN(parsed) || Double.isInfinite(parsed) || parsed < 0.0 || parsed > 1.0) {
      throw new IllegalArgumentException("progress must be between 0.0 and 1.0");
    }
    return parsed;
  }

  /**
   * Resolve the worker ID: the X-Worker-Id header is authoritative; if a body value is also
   * supplied it must match.
   */
  static String resolveWorkerId(Context ctx, String bodyWorkerId) {
    String headerWorkerId = HpcHeaders.requireWorkerId(ctx);
    if (bodyWorkerId != null && !bodyWorkerId.isBlank() && !headerWorkerId.equals(bodyWorkerId)) {
      throw HpcException.badRequest(
          "worker_id in request body must match X-Worker-Id header", HpcApiUtils.requestId(ctx));
    }
    return headerWorkerId;
  }

  /** Verify that the caller is the job submitter or has MANAGER+ privilege. */
  static void requireSubmitterOrManager(Context ctx, Row job, String action) {
    if ("HMAC".equals(ctx.attribute("hpcAuthMethod"))) {
      return;
    }

    Privileges privilege = ctx.attribute("hpcPrivilege");
    if (privilege != null && privilege.ordinal() >= Privileges.MANAGER.ordinal()) {
      return;
    }

    String authUser = ctx.attribute("hpcAuthUser");
    String submitUser = job.getString("submit_user");
    if (authUser != null && authUser.equals(submitUser)) {
      return;
    }

    throw HpcException.forbidden(
        "Only the job submitter or a manager can " + action + " this job",
        HpcApiUtils.requestId(ctx));
  }

  static int maxProgressPhaseLength() {
    return MAX_PROGRESS_PHASE_LENGTH;
  }

  static int maxProgressMessageLength() {
    return MAX_PROGRESS_MESSAGE_LENGTH;
  }
}
