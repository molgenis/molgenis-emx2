package org.molgenis.emx2.hpc;

/** Shared string constants for HPC table names in the _SYSTEM_ schema. */
public final class HpcTables {

  public static final String JOBS = "HpcJobs";
  public static final String JOB_TRANSITIONS = "HpcJobTransitions";
  public static final String JOB_STATUS = "HpcJobStatus";
  public static final String WORKERS = "HpcWorkers";
  public static final String WORKER_CAPABILITIES = "HpcWorkerCapabilities";
  public static final String WORKER_CREDENTIALS = "HpcWorkerCredentials";
  public static final String WORKER_CREDENTIAL_STATUS = "HpcWorkerCredentialStatus";
  public static final String ARTIFACTS = "HpcArtifacts";
  public static final String ARTIFACT_FILES = "HpcArtifactFiles";
  public static final String ARTIFACT_STATUS = "HpcArtifactStatus";
  public static final String ARTIFACT_RESIDENCE = "HpcArtifactResidence";

  private HpcTables() {}
}
