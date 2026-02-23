package org.molgenis.emx2.hpc.model;

import java.time.LocalDateTime;

/** Typed, content-addressed artifact. Can be input to or output from a job. */
public record Artifact(
    String id,
    String type,
    String format,
    String residence,
    ArtifactStatus status,
    String sha256,
    Long sizeBytes,
    String contentUrl,
    String metadata,
    String schemaInfo,
    LocalDateTime createdAt,
    LocalDateTime committedAt) {}
