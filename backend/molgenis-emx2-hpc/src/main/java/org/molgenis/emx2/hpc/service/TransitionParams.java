package org.molgenis.emx2.hpc.service;

/** Groups parameters for job state transitions to reduce method parameter counts. */
public record TransitionParams(
    String workerId,
    String detail,
    String slurmJobId,
    String outputArtifactId,
    String logArtifactId,
    String phase,
    String message,
    Double progress) {

  /** Convenience factory for transitions that only need worker and detail. */
  public static TransitionParams of(String workerId, String detail) {
    return new TransitionParams(workerId, detail, null, null, null, null, null, null);
  }
}
