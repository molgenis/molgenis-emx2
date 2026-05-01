package org.molgenis.emx2.hpc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;

class ArtifactResponseMapperTest {

  private final ArtifactResponseMapper mapper = new ArtifactResponseMapper();

  @Test
  void artifactToResponse_mapsAllFields() {
    Row artifact = new Row();
    artifact.set("id", "art-1");
    artifact.set("name", "output.tar.gz");
    artifact.set("type", "archive");
    artifact.set("residence", "managed");
    artifact.set("status", "COMMITTED");
    artifact.set("sha256", "abc123");
    artifact.set("size_bytes", "1024");
    artifact.set("content_url", null);
    artifact.set("created_at", "2026-01-01T00:00:00");
    artifact.set("committed_at", "2026-01-01T01:00:00");

    Map<String, Object> response = mapper.artifactToResponse(artifact);
    assertEquals("art-1", response.get("id"));
    assertEquals("output.tar.gz", response.get("name"));
    assertEquals("archive", response.get("type"));
    assertEquals("managed", response.get("residence"));
    assertEquals("COMMITTED", response.get("status"));
    assertEquals("abc123", response.get("sha256"));
    assertEquals("1024", response.get("size_bytes"));
    assertNotNull(response.get("_links"));
  }

  @Test
  void artifactToResponse_parsesMetadata() {
    Row artifact = new Row();
    artifact.set("id", "art-2");
    artifact.set("status", "CREATED");

    // metadata is null when not set — verify mapper doesn't fail
    Map<String, Object> response = mapper.artifactToResponse(artifact);
    assertFalse(response.containsKey("metadata"));
  }

  @Test
  void artifactToResponse_handlesUnparseableStatus() {
    Row artifact = new Row();
    artifact.set("id", "art-3");
    artifact.set("status", "UNKNOWN");

    Map<String, Object> response = mapper.artifactToResponse(artifact);
    assertNotNull(response.get("_links"));
  }

  @Test
  void artifactToResponse_linksVaryByStatus() {
    Row created = new Row();
    created.set("id", "art-c");
    created.set("status", "CREATED");
    Map<String, Object> createdResp = mapper.artifactToResponse(created);
    @SuppressWarnings("unchecked")
    Map<String, Object> createdLinks = (Map<String, Object>) createdResp.get("_links");
    assertTrue(createdLinks.containsKey("upload"));
    assertFalse(createdLinks.containsKey("download"));

    Row committed = new Row();
    committed.set("id", "art-d");
    committed.set("status", "COMMITTED");
    Map<String, Object> committedResp = mapper.artifactToResponse(committed);
    @SuppressWarnings("unchecked")
    Map<String, Object> committedLinks = (Map<String, Object>) committedResp.get("_links");
    assertTrue(committedLinks.containsKey("download"));
    assertFalse(committedLinks.containsKey("upload"));
  }
}
