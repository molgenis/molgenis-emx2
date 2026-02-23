package org.molgenis.emx2.hpc.model;

import java.time.LocalDateTime;

/** Domain object for an HPC job. Maps to the HpcJobs table. */
public record HpcJob(
    String id,
    String processor,
    String profile,
    String parameters,
    HpcJobStatus status,
    String workerId,
    String inputs,
    String slurmJobId,
    String submitUser,
    LocalDateTime createdAt,
    LocalDateTime claimedAt,
    LocalDateTime submittedAt,
    LocalDateTime startedAt,
    LocalDateTime completedAt) {}
