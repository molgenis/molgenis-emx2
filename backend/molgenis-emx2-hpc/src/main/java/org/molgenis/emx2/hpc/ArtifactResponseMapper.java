package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.hpc.HpcFields.*;
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
    response.put(ID, artifact.getString(ID));
    response.put(NAME, artifact.getString(NAME));
    response.put(TYPE, artifact.getString(TYPE));
    response.put(RESIDENCE, artifact.getString(RESIDENCE));
    response.put(STATUS, artifact.getString(STATUS));
    response.put(SHA256, artifact.getString(SHA256));
    response.put(SIZE_BYTES, artifact.getString(SIZE_BYTES));
    response.put(CONTENT_URL, artifact.getString(CONTENT_URL));
    response.put(CREATED_AT, artifact.getString(CREATED_AT));
    response.put(COMMITTED_AT, artifact.getString(COMMITTED_AT));

    JSONB metadataJson = artifact.getJsonb(METADATA);
    if (metadataJson != null) {
      response.put(METADATA, parseJsonb(metadataJson, METADATA));
    }

    ArtifactStatus status;
    try {
      status = ArtifactStatus.valueOf(artifact.getString(STATUS));
    } catch (Exception e) {
      status = ArtifactStatus.CREATED;
    }
    response.put(LINKS, LinkBuilder.forArtifact(artifact.getString(ID), status));

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
