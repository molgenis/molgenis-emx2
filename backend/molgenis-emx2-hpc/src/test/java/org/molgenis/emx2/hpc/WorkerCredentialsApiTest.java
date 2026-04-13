package org.molgenis.emx2.hpc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.http.Context;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.service.WorkerCredentialService;

class WorkerCredentialsApiTest {

  private WorkerCredentialService credentialService;
  private WorkerCredentialsApi api;
  private Context ctx;

  private static final String WORKER_ID = "worker-1";
  private static final String CRED_ID = "cred-1";

  @BeforeEach
  void setUp() {
    credentialService = mock(WorkerCredentialService.class);
    api = new WorkerCredentialsApi(credentialService);
    ctx = mock(Context.class);
    when(ctx.pathParam("id")).thenReturn(WORKER_ID);
    when(ctx.header("X-Request-Id")).thenReturn("req-1");
    when(ctx.attribute("hpcAuthMethod")).thenReturn("USER");
    when(ctx.attribute("hpcAuthUser")).thenReturn("admin");
  }

  // ── issueCredential ────────────────────────────────────────────────────────

  @Test
  void issueCredential_success() throws JsonProcessingException {
    when(ctx.body()).thenReturn("{\"label\":\"my-cred\"}");
    LocalDateTime now = LocalDateTime.of(2025, 1, 1, 0, 0);
    WorkerCredentialService.IssuedCredential issued =
        new WorkerCredentialService.IssuedCredential(
            CRED_ID, WORKER_ID, "secret123", "ACTIVE", "my-cred", now, null);
    when(credentialService.issueCredential(eq(WORKER_ID), eq("my-cred"), isNull(), eq("admin")))
        .thenReturn(issued);

    api.issueCredential(ctx);

    verify(ctx).status(201);
    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return CRED_ID.equals(map.get("id"))
                      && "secret123".equals(map.get("secret"))
                      && WORKER_ID.equals(map.get("worker_id"));
                }));
  }

  @Test
  void issueCredential_emptyBody() throws JsonProcessingException {
    when(ctx.body()).thenReturn("");
    LocalDateTime now = LocalDateTime.of(2025, 1, 1, 0, 0);
    WorkerCredentialService.IssuedCredential issued =
        new WorkerCredentialService.IssuedCredential(
            CRED_ID, WORKER_ID, "secret123", "ACTIVE", null, now, null);
    when(credentialService.issueCredential(eq(WORKER_ID), isNull(), isNull(), eq("admin")))
        .thenReturn(issued);

    api.issueCredential(ctx);

    verify(ctx).status(201);
  }

  @Test
  void issueCredential_conflict_alreadyActive() {
    when(ctx.body()).thenReturn("{}");
    when(credentialService.issueCredential(eq(WORKER_ID), isNull(), isNull(), eq("admin")))
        .thenThrow(new IllegalStateException("Worker worker-1 already has an active credential"));

    HpcException ex = assertThrows(HpcException.class, () -> api.issueCredential(ctx));
    assertEquals(409, ex.getStatus());
    assertTrue(ex.getMessage().contains("already has an active credential"));
  }

  @Test
  void issueCredential_credentialsKeyMissing() {
    when(ctx.body()).thenReturn("{}");
    when(credentialService.issueCredential(eq(WORKER_ID), isNull(), isNull(), eq("admin")))
        .thenThrow(
            new IllegalStateException(
                WorkerCredentialService.CREDENTIALS_KEY_SETTING
                    + " must be set to use worker credentials"));

    HpcException ex = assertThrows(HpcException.class, () -> api.issueCredential(ctx));
    assertEquals(503, ex.getStatus());
  }

  @Test
  void issueCredential_hmacAuth_nullCreatedBy() throws JsonProcessingException {
    when(ctx.attribute("hpcAuthMethod")).thenReturn("HMAC");
    when(ctx.body()).thenReturn("{}");
    LocalDateTime now = LocalDateTime.of(2025, 1, 1, 0, 0);
    WorkerCredentialService.IssuedCredential issued =
        new WorkerCredentialService.IssuedCredential(
            CRED_ID, WORKER_ID, "secret123", "ACTIVE", null, now, null);
    when(credentialService.issueCredential(eq(WORKER_ID), isNull(), isNull(), isNull()))
        .thenReturn(issued);

    api.issueCredential(ctx);

    verify(credentialService).issueCredential(eq(WORKER_ID), isNull(), isNull(), isNull());
  }

  // ── rotateCredential ───────────────────────────────────────────────────────

  @Test
  void rotateCredential_success() throws JsonProcessingException {
    when(ctx.body()).thenReturn("{\"label\":\"rotated\"}");
    LocalDateTime now = LocalDateTime.of(2025, 1, 1, 0, 0);
    WorkerCredentialService.IssuedCredential issued =
        new WorkerCredentialService.IssuedCredential(
            "cred-2", WORKER_ID, "newSecret", "ACTIVE", "rotated", now, null);
    when(credentialService.rotateCredential(eq(WORKER_ID), eq("rotated"), isNull(), eq("admin")))
        .thenReturn(issued);

    api.rotateCredential(ctx);

    verify(ctx).status(200);
    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return "cred-2".equals(map.get("id")) && "newSecret".equals(map.get("secret"));
                }));
  }

  @Test
  void rotateCredential_credentialsKeyMissing() {
    when(ctx.body()).thenReturn("{}");
    when(credentialService.rotateCredential(eq(WORKER_ID), isNull(), isNull(), eq("admin")))
        .thenThrow(
            new IllegalStateException(
                WorkerCredentialService.CREDENTIALS_KEY_SETTING
                    + " must be set to use worker credentials"));

    HpcException ex = assertThrows(HpcException.class, () -> api.rotateCredential(ctx));
    assertEquals(503, ex.getStatus());
  }

  // ── revokeCredential ───────────────────────────────────────────────────────

  @Test
  void revokeCredential_success() {
    when(ctx.pathParam("credentialId")).thenReturn(CRED_ID);
    Row revokedRow = buildCredentialRow(CRED_ID, WORKER_ID, "REVOKED");
    when(credentialService.revokeCredential(WORKER_ID, CRED_ID)).thenReturn(revokedRow);

    api.revokeCredential(ctx);

    verify(ctx).status(200);
    verify(ctx).json(any());
  }

  @Test
  void revokeCredential_notFound() {
    when(ctx.pathParam("credentialId")).thenReturn(CRED_ID);
    when(credentialService.revokeCredential(WORKER_ID, CRED_ID)).thenReturn(null);

    HpcException ex = assertThrows(HpcException.class, () -> api.revokeCredential(ctx));
    assertEquals(404, ex.getStatus());
  }

  // ── listCredentials ────────────────────────────────────────────────────────

  @Test
  void listCredentials_success() {
    Row cred = buildCredentialRow(CRED_ID, WORKER_ID, "ACTIVE");
    when(credentialService.listCredentials(WORKER_ID)).thenReturn(List.of(cred));

    api.listCredentials(ctx);

    verify(ctx).status(200);
    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return Integer.valueOf(1).equals(map.get("count"));
                }));
  }

  @Test
  void listCredentials_empty() {
    when(credentialService.listCredentials(WORKER_ID)).thenReturn(List.of());

    api.listCredentials(ctx);

    verify(ctx).status(200);
    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return Integer.valueOf(0).equals(map.get("count"));
                }));
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private static Row buildCredentialRow(String id, String workerId, String status) {
    Row row = new Row();
    row.set("id", id);
    row.set("worker_id", workerId);
    row.set("status", status);
    row.set("label", null);
    row.set("created_at", "2025-01-01T00:00:00");
    row.set("created_by", "admin");
    row.set("last_used_at", null);
    row.set("revoked_at", null);
    row.set("expires_at", null);
    return row;
  }
}
