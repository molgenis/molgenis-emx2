package org.molgenis.emx2.hpc.model;

import java.time.LocalDateTime;
import java.util.List;

/** Domain object representing a registered HPC worker (daemon). */
public record Worker(
    String workerId,
    String hostname,
    LocalDateTime registeredAt,
    LocalDateTime lastHeartbeatAt,
    List<WorkerCapability> capabilities) {}
