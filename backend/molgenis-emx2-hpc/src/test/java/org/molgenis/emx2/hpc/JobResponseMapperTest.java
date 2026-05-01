package org.molgenis.emx2.hpc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.protocol.HpcHeaders;
import org.molgenis.emx2.hpc.service.ArtifactService;

class JobResponseMapperTest {

  @Test
  void jobToResponse_mapsBasicFields() {
    ArtifactService artifactService = mock(ArtifactService.class);
    JobResponseMapper mapper = new JobResponseMapper(artifactService);

    Row job = new Row();
    job.set("id", "job-1");
    job.set("processor", "text-embedding");
    job.set("profile", "gpu-medium");
    job.set("status", "PENDING");
    job.set("worker_id", "w1");
    job.set("slurm_job_id", "slurm-99");
    job.set("submit_user", "admin");
    job.set("created_at", "2026-01-01T00:00:00");
    job.set("timeout_seconds", 3600);

    Map<String, Object> response = mapper.jobToResponse(job);
    assertEquals("job-1", response.get("id"));
    assertEquals("text-embedding", response.get("processor"));
    assertEquals("gpu-medium", response.get("profile"));
    assertEquals("PENDING", response.get("status"));
    assertEquals("w1", response.get("worker_id"));
    assertEquals("slurm-99", response.get("slurm_job_id"));
    assertEquals("admin", response.get("submit_user"));
    assertNotNull(response.get("_links"));
  }

  @Test
  void jobToResponse_parsesParametersJson() {
    ArtifactService artifactService = mock(ArtifactService.class);
    JobResponseMapper mapper = new JobResponseMapper(artifactService);

    Row job = new Row();
    job.set("id", "job-2");
    job.set("status", "PENDING");

    // parameters is null when not set — just verify the mapper doesn't fail
    Map<String, Object> response = mapper.jobToResponse(job);
    assertFalse(response.containsKey("parameters"));
  }

  @Test
  void jobToResponse_enrichesOutputArtifact() {
    ArtifactService artifactService = mock(ArtifactService.class);
    Row artifactRow = new Row();
    artifactRow.set("name", "output.tar.gz");
    artifactRow.set("type", "archive");
    artifactRow.set("status", "COMMITTED");
    when(artifactService.getArtifact("art-1")).thenReturn(artifactRow);

    JobResponseMapper mapper = new JobResponseMapper(artifactService);

    Row job = new Row();
    job.set("id", "job-3");
    job.set("status", "COMPLETED");
    job.set("output_artifact_id", "art-1");

    Map<String, Object> response = mapper.jobToResponse(job);
    assertEquals("art-1", response.get("output_artifact_id"));
    @SuppressWarnings("unchecked")
    Map<String, Object> enriched = (Map<String, Object>) response.get("output_artifact");
    assertNotNull(enriched);
    assertEquals("output.tar.gz", enriched.get("name"));
    assertEquals("COMMITTED", enriched.get("status"));
  }

  @Test
  void jobToResponse_handlesUnparseableStatus() {
    ArtifactService artifactService = mock(ArtifactService.class);
    JobResponseMapper mapper = new JobResponseMapper(artifactService);

    Row job = new Row();
    job.set("id", "job-4");
    job.set("status", "INVALID_STATUS");

    Map<String, Object> response = mapper.jobToResponse(job);
    // Should fall back to PENDING links without throwing
    assertNotNull(response.get("_links"));
  }

  @Test
  void jobToResponse_includesProgressFields() {
    ArtifactService artifactService = mock(ArtifactService.class);
    JobResponseMapper mapper = new JobResponseMapper(artifactService);

    Row job = new Row();
    job.set("id", "job-5");
    job.set("status", "STARTED");
    job.set("phase", "training");
    job.set("message", "step 3 of 10");
    job.set("progress", 0.3);

    Map<String, Object> response = mapper.jobToResponse(job);
    assertEquals("training", response.get("phase"));
    assertEquals("step 3 of 10", response.get("message"));
  }

  @Test
  void enrichArtifactRef_returnsMinimalRefOnFailure() {
    ArtifactService artifactService = mock(ArtifactService.class);
    when(artifactService.getArtifact("missing")).thenThrow(new RuntimeException("db error"));

    JobResponseMapper mapper = new JobResponseMapper(artifactService);
    Map<String, Object> ref = mapper.enrichArtifactRef("missing");
    assertEquals("missing", ref.get("id"));
    assertFalse(ref.containsKey("name"));
  }

  @Test
  void parseOptionalBoundedString_acceptsValidInput() {
    assertEquals("hello", JobResponseMapper.parseOptionalBoundedString("hello", "field", 100));
    assertNull(JobResponseMapper.parseOptionalBoundedString(null, "field", 100));
  }

  @Test
  void parseOptionalBoundedString_rejectsTooLong() {
    assertThrows(
        IllegalArgumentException.class,
        () -> JobResponseMapper.parseOptionalBoundedString("toolong", "field", 3));
  }

  @Test
  void parseOptionalBoundedString_rejectsNonString() {
    assertThrows(
        IllegalArgumentException.class,
        () -> JobResponseMapper.parseOptionalBoundedString(42, "field", 100));
  }

  @Test
  void parseOptionalProgress_acceptsValidRange() {
    assertNull(JobResponseMapper.parseOptionalProgress(null));
    assertEquals(0.0, JobResponseMapper.parseOptionalProgress(0.0));
    assertEquals(0.5, JobResponseMapper.parseOptionalProgress(0.5));
    assertEquals(1.0, JobResponseMapper.parseOptionalProgress(1.0));
  }

  @Test
  void parseOptionalProgress_rejectsOutOfRange() {
    assertThrows(
        IllegalArgumentException.class, () -> JobResponseMapper.parseOptionalProgress(1.5));
    assertThrows(
        IllegalArgumentException.class, () -> JobResponseMapper.parseOptionalProgress(-0.1));
    assertThrows(
        IllegalArgumentException.class, () -> JobResponseMapper.parseOptionalProgress(Double.NaN));
  }

  @Test
  void parseOptionalProgress_rejectsNonNumber() {
    assertThrows(
        IllegalArgumentException.class, () -> JobResponseMapper.parseOptionalProgress("0.5"));
  }

  @Test
  void resolveWorkerId_matchesHeaderAndBody() {
    Context ctx = mock(Context.class);
    when(ctx.header(HpcHeaders.WORKER_ID)).thenReturn("w1");
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn("req-1");
    assertEquals("w1", JobResponseMapper.resolveWorkerId(ctx, "w1"));
    assertEquals("w1", JobResponseMapper.resolveWorkerId(ctx, null));
    assertEquals("w1", JobResponseMapper.resolveWorkerId(ctx, ""));
  }

  @Test
  void resolveWorkerId_throwsOnMismatch() {
    Context ctx = mock(Context.class);
    when(ctx.header(HpcHeaders.WORKER_ID)).thenReturn("w1");
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn("req-1");
    assertThrows(HpcException.class, () -> JobResponseMapper.resolveWorkerId(ctx, "w2"));
  }

  @Test
  void requireSubmitterOrManager_allowsHmacAuth() {
    Context ctx = mock(Context.class);
    when(ctx.attribute(HpcAuth.HPC_AUTH_METHOD_ATTR)).thenReturn("HMAC");
    Row job = new Row();
    assertDoesNotThrow(() -> JobResponseMapper.requireSubmitterOrManager(ctx, job, "cancel"));
  }

  @Test
  void requireSubmitterOrManager_allowsManagerPrivilege() {
    Context ctx = mock(Context.class);
    when(ctx.attribute(HpcAuth.HPC_AUTH_METHOD_ATTR)).thenReturn("USER");
    when(ctx.attribute("hpcPrivilege")).thenReturn(Privileges.MANAGER);
    Row job = new Row();
    job.set("submit_user", "other-user");
    assertDoesNotThrow(() -> JobResponseMapper.requireSubmitterOrManager(ctx, job, "cancel"));
  }

  @Test
  void requireSubmitterOrManager_allowsOwnerPrivilege() {
    Context ctx = mock(Context.class);
    when(ctx.attribute(HpcAuth.HPC_AUTH_METHOD_ATTR)).thenReturn("USER");
    when(ctx.attribute("hpcPrivilege")).thenReturn(Privileges.OWNER);
    Row job = new Row();
    job.set("submit_user", "other-user");
    assertDoesNotThrow(() -> JobResponseMapper.requireSubmitterOrManager(ctx, job, "cancel"));
  }

  @Test
  void requireSubmitterOrManager_allowsSubmitter() {
    Context ctx = mock(Context.class);
    when(ctx.attribute(HpcAuth.HPC_AUTH_METHOD_ATTR)).thenReturn("USER");
    when(ctx.attribute("hpcPrivilege")).thenReturn(Privileges.EDITOR);
    when(ctx.attribute(HpcAuth.HPC_AUTH_USER_ATTR)).thenReturn("submitter1");
    Row job = new Row();
    job.set("submit_user", "submitter1");
    assertDoesNotThrow(() -> JobResponseMapper.requireSubmitterOrManager(ctx, job, "cancel"));
  }

  @Test
  void requireSubmitterOrManager_rejectsNonSubmitterNonManager() {
    Context ctx = mock(Context.class);
    when(ctx.attribute(HpcAuth.HPC_AUTH_METHOD_ATTR)).thenReturn("USER");
    when(ctx.attribute("hpcPrivilege")).thenReturn(Privileges.EDITOR);
    when(ctx.attribute(HpcAuth.HPC_AUTH_USER_ATTR)).thenReturn("someone-else");
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn("req-1");
    Row job = new Row();
    job.set("submit_user", "submitter1");
    HpcException ex =
        assertThrows(
            HpcException.class,
            () -> JobResponseMapper.requireSubmitterOrManager(ctx, job, "cancel"));
    assertEquals(403, ex.getStatus());
    assertTrue(ex.getMessage().contains("submitter or a manager"));
  }

  @Test
  void requireSubmitterOrManager_rejectsNullPrivilege() {
    Context ctx = mock(Context.class);
    when(ctx.attribute(HpcAuth.HPC_AUTH_METHOD_ATTR)).thenReturn("USER");
    when(ctx.attribute("hpcPrivilege")).thenReturn(null);
    when(ctx.attribute(HpcAuth.HPC_AUTH_USER_ATTR)).thenReturn("someone");
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn("req-1");
    Row job = new Row();
    job.set("submit_user", "other");
    HpcException ex =
        assertThrows(
            HpcException.class,
            () -> JobResponseMapper.requireSubmitterOrManager(ctx, job, "delete"));
    assertEquals(403, ex.getStatus());
  }

  @Test
  void jobToResponse_withParametersJsonb() {
    ArtifactService artifactService = mock(ArtifactService.class);
    JobResponseMapper mapper = new JobResponseMapper(artifactService);

    Row job = new Row();
    job.set("id", "job-p1");
    job.set("status", "PENDING");
    job.set("parameters", "{\"model\":\"gpt-4\",\"temperature\":0.7}");

    Map<String, Object> response = mapper.jobToResponse(job);
    assertNotNull(response.get("parameters"));
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) response.get("parameters");
    assertEquals("gpt-4", params.get("model"));
  }

  @Test
  void jobToResponse_enrichesLogArtifact() {
    ArtifactService artifactService = mock(ArtifactService.class);
    Row logArtifactRow = new Row();
    logArtifactRow.set("name", "logs.tar.gz");
    logArtifactRow.set("type", "log");
    logArtifactRow.set("status", "COMMITTED");
    when(artifactService.getArtifact("log-1")).thenReturn(logArtifactRow);

    JobResponseMapper mapper = new JobResponseMapper(artifactService);

    Row job = new Row();
    job.set("id", "job-l1");
    job.set("status", "COMPLETED");
    job.set("log_artifact_id", "log-1");

    Map<String, Object> response = mapper.jobToResponse(job);
    assertEquals("log-1", response.get("log_artifact_id"));
    @SuppressWarnings("unchecked")
    Map<String, Object> enriched = (Map<String, Object>) response.get("log_artifact");
    assertNotNull(enriched);
    assertEquals("logs.tar.gz", enriched.get("name"));
    assertEquals("COMMITTED", enriched.get("status"));
  }

  @Test
  void jobToResponse_enrichesInputsAsList() {
    ArtifactService artifactService = mock(ArtifactService.class);
    Row inputArtifact = new Row();
    inputArtifact.set("name", "input.csv");
    inputArtifact.set("type", "data");
    inputArtifact.set("status", "COMMITTED");
    when(artifactService.getArtifact("in-1")).thenReturn(inputArtifact);

    JobResponseMapper mapper = new JobResponseMapper(artifactService);

    Row job = new Row();
    job.set("id", "job-i1");
    job.set("status", "PENDING");
    job.set("inputs", "[\"in-1\"]");

    Map<String, Object> response = mapper.jobToResponse(job);
    assertNotNull(response.get("inputs"));
    @SuppressWarnings("unchecked")
    List<Object> inputs = (List<Object>) response.get("inputs");
    assertEquals(1, inputs.size());
    @SuppressWarnings("unchecked")
    Map<String, Object> enrichedInput = (Map<String, Object>) inputs.get(0);
    assertEquals("in-1", enrichedInput.get("id"));
    assertEquals("input.csv", enrichedInput.get("name"));
  }

  @Test
  void jobToResponse_enrichesInputsAsMapWithArtifactId() {
    ArtifactService artifactService = mock(ArtifactService.class);
    Row inputArtifact = new Row();
    inputArtifact.set("name", "input.csv");
    inputArtifact.set("type", "data");
    inputArtifact.set("status", "COMMITTED");
    when(artifactService.getArtifact("in-2")).thenReturn(inputArtifact);

    JobResponseMapper mapper = new JobResponseMapper(artifactService);

    Row job = new Row();
    job.set("id", "job-i2");
    job.set("status", "PENDING");
    job.set("inputs", "[{\"artifact_id\":\"in-2\",\"role\":\"primary\"}]");

    Map<String, Object> response = mapper.jobToResponse(job);
    @SuppressWarnings("unchecked")
    List<Object> inputs = (List<Object>) response.get("inputs");
    assertEquals(1, inputs.size());
    @SuppressWarnings("unchecked")
    Map<String, Object> enrichedInput = (Map<String, Object>) inputs.get(0);
    assertEquals("in-2", enrichedInput.get("artifact_id"));
    assertEquals("primary", enrichedInput.get("role"));
    assertEquals("input.csv", enrichedInput.get("name"));
  }

  @Test
  void jobToResponse_enrichesInputsAsMapWithIdFallback() {
    ArtifactService artifactService = mock(ArtifactService.class);
    Row inputArtifact = new Row();
    inputArtifact.set("name", "input2.csv");
    inputArtifact.set("type", "data");
    inputArtifact.set("status", "COMMITTED");
    when(artifactService.getArtifact("in-3")).thenReturn(inputArtifact);

    JobResponseMapper mapper = new JobResponseMapper(artifactService);

    Row job = new Row();
    job.set("id", "job-i3");
    job.set("status", "PENDING");
    job.set("inputs", "[{\"id\":\"in-3\"}]");

    Map<String, Object> response = mapper.jobToResponse(job);
    @SuppressWarnings("unchecked")
    List<Object> inputs = (List<Object>) response.get("inputs");
    @SuppressWarnings("unchecked")
    Map<String, Object> enrichedInput = (Map<String, Object>) inputs.get(0);
    assertEquals("in-3", enrichedInput.get("id"));
    assertEquals("input2.csv", enrichedInput.get("name"));
  }

  @Test
  void jobToResponse_inputMapWithNoArtifactId_passesThrough() {
    ArtifactService artifactService = mock(ArtifactService.class);
    JobResponseMapper mapper = new JobResponseMapper(artifactService);

    Row job = new Row();
    job.set("id", "job-i4");
    job.set("status", "PENDING");
    job.set("inputs", "[{\"url\":\"https://example.com/data\"}]");

    Map<String, Object> response = mapper.jobToResponse(job);
    @SuppressWarnings("unchecked")
    List<Object> inputs = (List<Object>) response.get("inputs");
    assertEquals(1, inputs.size());
    @SuppressWarnings("unchecked")
    Map<String, Object> input = (Map<String, Object>) inputs.get(0);
    assertEquals("https://example.com/data", input.get("url"));
    assertNull(input.get("name")); // no enrichment
  }

  @Test
  void jobToResponse_parseJsonb_throwsOnInvalidJson() {
    ArtifactService artifactService = mock(ArtifactService.class);
    JobResponseMapper mapper = new JobResponseMapper(artifactService);

    Row job = new Row();
    job.set("id", "job-bad");
    job.set("status", "PENDING");
    // Invalid JSON triggers an exception either at Row.getJsonb or during parseJsonb
    job.set("parameters", "{invalid json");

    assertThrows(Exception.class, () -> mapper.jobToResponse(job));
  }

  @Test
  void jobToResponse_inputsNonListJson_setsDirectly() {
    ArtifactService artifactService = mock(ArtifactService.class);
    JobResponseMapper mapper = new JobResponseMapper(artifactService);

    Row job = new Row();
    job.set("id", "job-obj");
    job.set("status", "PENDING");
    // A JSON object (not an array) — hits the "parsedInputs != null but not List" branch
    job.set("inputs", "{\"artifact_id\":\"in-1\"}");

    Map<String, Object> response = mapper.jobToResponse(job);
    // Should set inputs directly as a map, not as enriched list
    assertNotNull(response.get("inputs"));
    assertTrue(response.get("inputs") instanceof Map);
  }
}
