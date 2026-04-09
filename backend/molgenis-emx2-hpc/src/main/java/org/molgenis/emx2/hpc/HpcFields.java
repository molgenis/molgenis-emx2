package org.molgenis.emx2.hpc;

/**
 * Shared string constants for HPC database column names and JSON response keys used across multiple
 * HPC classes.
 */
public final class HpcFields {

  // --- Common identifiers ---
  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String TYPE = "type";
  public static final String STATUS = "status";
  public static final String LABEL = "label";

  // --- Worker fields ---
  public static final String WORKER_ID = "worker_id";
  public static final String HOSTNAME = "hostname";
  public static final String REGISTERED_AT = "registered_at";
  public static final String LAST_HEARTBEAT_AT = "last_heartbeat_at";
  public static final String MAX_CONCURRENT_JOBS = "max_concurrent_jobs";
  public static final String SECRET_ENCRYPTED = "secret_encrypted";

  // --- Credential lifecycle ---
  public static final String CREATED_AT = "created_at";
  public static final String CREATED_BY = "created_by";
  public static final String EXPIRES_AT = "expires_at";
  public static final String REVOKED_AT = "revoked_at";
  public static final String LAST_USED_AT = "last_used_at";

  // --- Job fields ---
  public static final String PROCESSOR = "processor";
  public static final String PROFILE = "profile";
  public static final String PARAMETERS = "parameters";
  public static final String INPUTS = "inputs";
  public static final String SUBMIT_USER = "submit_user";
  public static final String SLURM_JOB_ID = "slurm_job_id";
  public static final String TIMEOUT_SECONDS = "timeout_seconds";
  public static final String OUTPUT_ARTIFACT_ID = "output_artifact_id";
  public static final String LOG_ARTIFACT_ID = "log_artifact_id";
  public static final String CLAIMED_AT = "claimed_at";
  public static final String SUBMITTED_AT = "submitted_at";
  public static final String STARTED_AT = "started_at";
  public static final String COMPLETED_AT = "completed_at";

  // --- Job progress ---
  public static final String PHASE = "phase";
  public static final String MESSAGE = "message";
  public static final String PROGRESS = "progress";

  // --- Transition fields ---
  public static final String JOB_ID = "job_id";
  public static final String FROM_STATUS = "from_status";
  public static final String TO_STATUS = "to_status";
  public static final String TIMESTAMP = "timestamp";
  public static final String DETAIL = "detail";

  // --- Artifact fields ---
  public static final String ARTIFACT_ID = "artifact_id";
  public static final String RESIDENCE = "residence";
  public static final String SHA256 = "sha256";
  public static final String SIZE_BYTES = "size_bytes";
  public static final String CONTENT_URL = "content_url";
  public static final String CONTENT_TYPE = "content_type";
  public static final String METADATA = "metadata";
  public static final String PATH = "path";
  public static final String COMMITTED_AT = "committed_at";

  // --- JSON response keys ---
  public static final String LINKS = "_links";

  // --- Common message fragments ---
  public static final String NOT_FOUND_SUFFIX = " not found";

  // --- HATEOAS link building ---
  public static final String METHOD = "method";
  public static final String WORKERS_PATH = "/api/hpc/workers/";
  public static final String CREDENTIALS_PATH = "/credentials";

  private HpcFields() {}
}
