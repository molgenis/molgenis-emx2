package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jooq.JSONB;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.model.ArtifactStatus;
import org.molgenis.emx2.hpc.protocol.LinkBuilder;

/** Serialization helpers for artifact responses. */
class ArtifactResponseMapper {

  ArtifactResponseMapper() {}

  /** Build a full JSON-ready response map for an artifact row. */
  @SuppressWarnings("unchecked")
  Map<String, Object> artifactToResponse(Row artifact) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", artifact.getString("id"));
    response.put("name", artifact.getString("name"));
    response.put("type", artifact.getString("type"));
    response.put("residence", artifact.getString("residence"));
    response.put("status", artifact.getString("status"));
    response.put("sha256", artifact.getString("sha256"));
    response.put("size_bytes", artifact.getString("size_bytes"));
    response.put("content_url", artifact.getString("content_url"));
    response.put("created_at", artifact.getString("created_at"));
    response.put("committed_at", artifact.getString("committed_at"));

    JSONB metadataJson = artifact.getJsonb("metadata");
    if (metadataJson != null) {
      response.put("metadata", parseJsonb(metadataJson, "metadata"));
    }

    ArtifactStatus status;
    try {
      status = ArtifactStatus.valueOf(artifact.getString("status"));
    } catch (Exception e) {
      status = ArtifactStatus.CREATED;
    }
    response.put("_links", LinkBuilder.forArtifact(artifact.getString("id"), status));

    return response;
  }

  private static Object parseJsonb(JSONB jsonb, String fieldName) {
    try {
      return MAPPER.readValue(jsonb.toString(), Object.class);
    } catch (Exception e) {
      throw new MolgenisException("Invalid JSON in HPC field '" + fieldName + "'", e);
    }
  }
}
