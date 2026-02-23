package org.molgenis.emx2.hpc.model;

/** A single file within a multi-file artifact. Content stored via EMX2 FILE column. */
public record ArtifactFile(
    String id,
    String artifactId,
    String path,
    String role,
    String sha256,
    Long sizeBytes,
    String contentType) {}
