package org.molgenis.emx2.hpc.model;

import java.time.LocalDateTime;

/** Immutable audit record of a job state transition. */
public record JobTransition(
    String id,
    String jobId,
    HpcJobStatus fromStatus,
    HpcJobStatus toStatus,
    LocalDateTime timestamp,
    String workerId,
    String detail) {}
