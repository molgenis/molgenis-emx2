package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.hpc.HpcFields.*;
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
    response.put(ID, job.getString(ID));
    response.put(PROCESSOR, job.getString(PROCESSOR));
    response.put(PROFILE, job.getString(PROFILE));
    response.put(STATUS, job.getString(STATUS));
    response.put(WORKER_ID, job.getString(WORKER_ID));
    response.put(SLURM_JOB_ID, job.getString(SLURM_JOB_ID));
    response.put(SUBMIT_USER, job.getString(SUBMIT_USER));

    JSONB parametersJson = job.getJsonb(PARAMETERS);
    if (parametersJson != null) {
      response.put(PARAMETERS, parseJsonb(parametersJson, PARAMETERS));
    }

    Object parsedInputs = parseJsonb(job.getJsonb(INPUTS), INPUTS);
    if (parsedInputs instanceof List<?> inputList) {
      response.put(INPUTS, inputList.stream().map(this::enrichInputRef).toList());
    } else if (parsedInputs != null) {
      response.put(INPUTS, parsedInputs);
    }

    // Enrich output artifact
    String outputArtifactId = job.getString(OUTPUT_ARTIFACT_ID);
    if (outputArtifactId != null) {
      response.put("output_artifact", enrichArtifactRef(outputArtifactId));
    }
    response.put(OUTPUT_ARTIFACT_ID, outputArtifactId);

    // Enrich log artifact
    String logArtifactId = job.getString(LOG_ARTIFACT_ID);
    if (logArtifactId != null) {
      response.put("log_artifact", enrichArtifactRef(logArtifactId));
    }
    response.put(LOG_ARTIFACT_ID, logArtifactId);
    response.put(TIMEOUT_SECONDS, job.getInteger(TIMEOUT_SECONDS));
    response.put(CREATED_AT, job.getString(CREATED_AT));
    response.put(CLAIMED_AT, job.getString(CLAIMED_AT));
    response.put(SUBMITTED_AT, job.getString(SUBMITTED_AT));
    response.put(STARTED_AT, job.getString(STARTED_AT));
    response.put(COMPLETED_AT, job.getString(COMPLETED_AT));
    response.put(PHASE, job.getString(PHASE));
    response.put(MESSAGE, job.getString(MESSAGE));
    response.put(PROGRESS, job.getDecimal(PROGRESS));

    HpcJobStatus status;
    try {
      status = HpcJobStatus.valueOf(job.getString(STATUS));
    } catch (Exception e) {
      status = HpcJobStatus.PENDING;
    }
    response.put(LINKS, LinkBuilder.forJob(job.getString(ID), status));

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
    ref.put(ID, artifactId);
    try {
      Row artifact = artifactService.getArtifact(artifactId);
      if (artifact != null) {
        ref.put(NAME, artifact.getString(NAME));
        ref.put(TYPE, artifact.getString(TYPE));
        ref.put(STATUS, artifact.getString(STATUS));
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
      Object artifactIdValue = map.get(ARTIFACT_ID);
      if (!(artifactIdValue instanceof String)) {
        artifactIdValue = map.get(ID);
      }
      if (artifactIdValue instanceof String artifactId) {
        Map<String, Object> enriched = new LinkedHashMap<>((Map<String, Object>) map);
        Map<String, Object> artifact = enrichArtifactRef(artifactId);
        enriched.putIfAbsent(ID, artifactId);
        enriched.putIfAbsent(ARTIFACT_ID, artifactId);
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
    if ("HMAC".equals(ctx.attribute(HpcAuth.HPC_AUTH_METHOD_ATTR))) {
      return;
    }

    Privileges privilege = ctx.attribute("hpcPrivilege");
    if (privilege != null && privilege.ordinal() >= Privileges.MANAGER.ordinal()) {
      return;
    }

    String authUser = ctx.attribute(HpcAuth.HPC_AUTH_USER_ATTR);
    String submitUser = job.getString(SUBMIT_USER);
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
