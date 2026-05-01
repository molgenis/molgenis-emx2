package org.molgenis.emx2.hpc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.sql.SqlDatabase;

class HpcAuthTest {

  private Context ctx;
  private SqlDatabase database;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    database = mock(SqlDatabase.class);
    when(ctx.header("X-Request-Id")).thenReturn("req-1");
  }

  // ── hasBody ───────────────────────────────────────────────────────────────

  @ParameterizedTest
  @EnumSource(
      value = HandlerType.class,
      names = {"GET", "HEAD", "DELETE"})
  void hasBody_bodylessMethodsReturnFalse(HandlerType method) {
    when(ctx.method()).thenReturn(method);
    assertFalse(HpcAuth.hasBody(ctx));
  }

  @Test
  void hasBody_postWithContentLengthGreaterThanZero_returnsTrue() {
    when(ctx.method()).thenReturn(HandlerType.POST);
    when(ctx.header("Transfer-Encoding")).thenReturn(null);
    when(ctx.header("Content-Length")).thenReturn("42");
    assertTrue(HpcAuth.hasBody(ctx));
  }

  @Test
  void hasBody_postWithContentLengthZero_returnsFalse() {
    when(ctx.method()).thenReturn(HandlerType.POST);
    when(ctx.header("Transfer-Encoding")).thenReturn(null);
    when(ctx.header("Content-Length")).thenReturn("0");
    assertFalse(HpcAuth.hasBody(ctx));
  }

  @Test
  void hasBody_postWithNoHeaders_returnsFalse() {
    when(ctx.method()).thenReturn(HandlerType.POST);
    when(ctx.header("Transfer-Encoding")).thenReturn(null);
    when(ctx.header("Content-Length")).thenReturn(null);
    assertFalse(HpcAuth.hasBody(ctx));
  }

  @Test
  void hasBody_postWithBlankContentLength_returnsFalse() {
    when(ctx.method()).thenReturn(HandlerType.POST);
    when(ctx.header("Transfer-Encoding")).thenReturn(null);
    when(ctx.header("Content-Length")).thenReturn("  ");
    assertFalse(HpcAuth.hasBody(ctx));
  }

  @Test
  void hasBody_postWithTransferEncoding_returnsTrue() {
    when(ctx.method()).thenReturn(HandlerType.POST);
    when(ctx.header("Transfer-Encoding")).thenReturn("chunked");
    assertTrue(HpcAuth.hasBody(ctx));
  }

  @Test
  void hasBody_postWithUnparseableContentLength_returnsTrue() {
    when(ctx.method()).thenReturn(HandlerType.POST);
    when(ctx.header("Transfer-Encoding")).thenReturn(null);
    when(ctx.header("Content-Length")).thenReturn("not-a-number");
    assertTrue(HpcAuth.hasBody(ctx));
  }

  @Test
  void hasBody_putWithContentLength_returnsTrue() {
    when(ctx.method()).thenReturn(HandlerType.PUT);
    when(ctx.header("Transfer-Encoding")).thenReturn(null);
    when(ctx.header("Content-Length")).thenReturn("100");
    assertTrue(HpcAuth.hasBody(ctx));
  }

  // ── requireHpcPrivilege ───────────────────────────────────────────────────

  @Test
  void requireHpcPrivilege_hmacAuth_alwaysPasses() {
    when(ctx.attribute("hpcAuthMethod")).thenReturn("HMAC");

    // Should not throw — HMAC bypasses privilege check
    assertDoesNotThrow(() -> HpcAuth.requireHpcPrivilege(ctx, Privileges.OWNER));
  }

  @Test
  void requireHpcPrivilege_userWithManager_passesForManager() {
    when(ctx.attribute("hpcAuthMethod")).thenReturn("USER");
    when(ctx.attribute("hpcPrivilege")).thenReturn(Privileges.MANAGER);

    assertDoesNotThrow(() -> HpcAuth.requireHpcPrivilege(ctx, Privileges.MANAGER));
  }

  @Test
  void requireHpcPrivilege_userWithOwner_passesForManager() {
    when(ctx.attribute("hpcAuthMethod")).thenReturn("USER");
    when(ctx.attribute("hpcPrivilege")).thenReturn(Privileges.OWNER);

    assertDoesNotThrow(() -> HpcAuth.requireHpcPrivilege(ctx, Privileges.MANAGER));
  }

  @Test
  void requireHpcPrivilege_userWithViewer_failsForManager() {
    when(ctx.attribute("hpcAuthMethod")).thenReturn("USER");
    when(ctx.attribute("hpcPrivilege")).thenReturn(Privileges.VIEWER);

    HpcException ex =
        assertThrows(
            HpcException.class, () -> HpcAuth.requireHpcPrivilege(ctx, Privileges.MANAGER));
    assertEquals(403, ex.getStatus());
    assertTrue(ex.getMessage().contains("Manager"));
  }

  @Test
  void requireHpcPrivilege_userWithEditor_passesForViewer() {
    when(ctx.attribute("hpcAuthMethod")).thenReturn("USER");
    when(ctx.attribute("hpcPrivilege")).thenReturn(Privileges.EDITOR);

    assertDoesNotThrow(() -> HpcAuth.requireHpcPrivilege(ctx, Privileges.VIEWER));
  }

  @Test
  void requireHpcPrivilege_userWithEditor_failsForManager() {
    when(ctx.attribute("hpcAuthMethod")).thenReturn("USER");
    when(ctx.attribute("hpcPrivilege")).thenReturn(Privileges.EDITOR);

    HpcException ex =
        assertThrows(
            HpcException.class, () -> HpcAuth.requireHpcPrivilege(ctx, Privileges.MANAGER));
    assertEquals(403, ex.getStatus());
  }

  @Test
  void requireHpcPrivilege_userWithNullPrivilege_throws403() {
    when(ctx.attribute("hpcAuthMethod")).thenReturn("USER");
    when(ctx.attribute("hpcPrivilege")).thenReturn(null);

    HpcException ex =
        assertThrows(HpcException.class, () -> HpcAuth.requireHpcPrivilege(ctx, Privileges.VIEWER));
    assertEquals(403, ex.getStatus());
  }

  // ── requireUserHpcPrivilege ───────────────────────────────────────────────

  @Test
  void requireUserHpcPrivilege_hmacAuth_throws403() {
    when(ctx.attribute("hpcAuthMethod")).thenReturn("HMAC");

    HpcException ex =
        assertThrows(
            HpcException.class, () -> HpcAuth.requireUserHpcPrivilege(ctx, Privileges.MANAGER));
    assertEquals(403, ex.getStatus());
    assertTrue(ex.getMessage().contains("Worker principal"));
  }

  @Test
  void requireUserHpcPrivilege_userWithSufficientPrivilege_passes() {
    when(ctx.attribute("hpcAuthMethod")).thenReturn("USER");
    when(ctx.attribute("hpcPrivilege")).thenReturn(Privileges.MANAGER);

    assertDoesNotThrow(() -> HpcAuth.requireUserHpcPrivilege(ctx, Privileges.MANAGER));
  }

  @Test
  void requireUserHpcPrivilege_userWithInsufficientPrivilege_throws403() {
    when(ctx.attribute("hpcAuthMethod")).thenReturn("USER");
    when(ctx.attribute("hpcPrivilege")).thenReturn(Privileges.VIEWER);

    HpcException ex =
        assertThrows(
            HpcException.class, () -> HpcAuth.requireUserHpcPrivilege(ctx, Privileges.MANAGER));
    assertEquals(403, ex.getStatus());
  }

  // ── healthCheck ───────────────────────────────────────────────────────────

  @SuppressWarnings("unchecked")
  @Test
  void healthCheck_databaseError_returnsDegraded() {
    doThrow(new RuntimeException("db down")).when(database).tx(any());

    HpcAuth.healthCheck(ctx, database, true, true);

    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(ctx).json(captor.capture());

    Map<String, Object> health = captor.getValue();
    assertEquals("degraded", health.get("status"));
    assertEquals("error", health.get("database"));
    assertEquals("db down", health.get("database_error"));
    assertEquals(true, health.get("hpc_enabled"));
    assertEquals(true, health.get("hpc_initialized"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void healthCheck_hpcDisabled() {
    // readSetting uses database.tx() internally, so allow it to succeed (returns null)
    doAnswer(invocation -> null).when(database).tx(any());

    HpcAuth.healthCheck(ctx, database, false, false);

    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(ctx).json(captor.capture());

    Map<String, Object> health = captor.getValue();
    assertEquals(false, health.get("hpc_enabled"));
    assertEquals(false, health.get("hpc_initialized"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void healthCheck_responseStructure() {
    // Simulate successful DB check
    doAnswer(
            invocation -> {
              // The tx callback doesn't need to do anything for this test
              return null;
            })
        .when(database)
        .tx(any());

    HpcAuth.healthCheck(ctx, database, true, true);

    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(ctx).json(captor.capture());

    Map<String, Object> health = captor.getValue();
    assertEquals("ok", health.get("status"));
    assertEquals("2025-01", health.get("api_version"));
    assertEquals("per-worker-hmac", health.get("worker_auth_mode"));
    assertTrue(health.containsKey("credentials_key_configured"));
  }

  // ── resolveEffectivePrivilege ─────────────────────────────────────────────

  @Test
  void resolveEffectivePrivilege_nullUsername_returnsNull() {
    Privileges result = HpcAuth.resolveEffectivePrivilege(database, null);
    assertNull(result);
    verify(database, never()).tx(any());
  }

  // ── hpcHandler ────────────────────────────────────────────────────────────

  @Test
  void hpcHandler_hpcExceptionPassesThrough() {
    io.javalin.http.Handler inner =
        c -> {
          throw HpcException.notFound("not found", "req-1");
        };

    io.javalin.http.Handler wrapped = HpcAuth.hpcHandler(inner);

    HpcException ex = assertThrows(HpcException.class, () -> wrapped.handle(ctx));
    assertEquals(404, ex.getStatus());
  }

  @Test
  void hpcHandler_illegalArgumentBecomesHpcBadRequest() {
    io.javalin.http.Handler inner =
        c -> {
          throw new IllegalArgumentException("bad param");
        };

    io.javalin.http.Handler wrapped = HpcAuth.hpcHandler(inner);

    HpcException ex = assertThrows(HpcException.class, () -> wrapped.handle(ctx));
    assertEquals(400, ex.getStatus());
    assertTrue(ex.getMessage().contains("bad param"));
  }

  @Test
  void hpcHandler_illegalArgumentNoMessage_usesClassName() {
    io.javalin.http.Handler inner =
        c -> {
          throw new IllegalArgumentException();
        };

    io.javalin.http.Handler wrapped = HpcAuth.hpcHandler(inner);

    HpcException ex = assertThrows(HpcException.class, () -> wrapped.handle(ctx));
    assertEquals(400, ex.getStatus());
    assertTrue(ex.getMessage().contains("IllegalArgumentException"));
  }

  @Test
  void hpcHandler_molgenisExceptionNotFound_becomes404() {
    when(ctx.method()).thenReturn(HandlerType.GET);
    when(ctx.path()).thenReturn("/api/hpc/test");

    io.javalin.http.Handler inner =
        c -> {
          throw new org.molgenis.emx2.MolgenisException("Job not found");
        };

    io.javalin.http.Handler wrapped = HpcAuth.hpcHandler(inner);

    HpcException ex = assertThrows(HpcException.class, () -> wrapped.handle(ctx));
    assertEquals(404, ex.getStatus());
  }

  @Test
  void hpcHandler_molgenisExceptionGeneric_becomes500() {
    when(ctx.method()).thenReturn(HandlerType.GET);
    when(ctx.path()).thenReturn("/api/hpc/test");

    io.javalin.http.Handler inner =
        c -> {
          throw new org.molgenis.emx2.MolgenisException("DB constraint violated");
        };

    io.javalin.http.Handler wrapped = HpcAuth.hpcHandler(inner);

    HpcException ex = assertThrows(HpcException.class, () -> wrapped.handle(ctx));
    assertEquals(500, ex.getStatus());
  }

  @Test
  void hpcHandler_unexpectedException_becomes500() {
    when(ctx.method()).thenReturn(HandlerType.GET);
    when(ctx.path()).thenReturn("/api/hpc/test");

    io.javalin.http.Handler inner =
        c -> {
          throw new RuntimeException("unexpected");
        };

    io.javalin.http.Handler wrapped = HpcAuth.hpcHandler(inner);

    HpcException ex = assertThrows(HpcException.class, () -> wrapped.handle(ctx));
    assertEquals(500, ex.getStatus());
  }

  @Test
  void hpcHandler_credentialsKeyMissing_molgenisException_becomes503() {
    when(ctx.method()).thenReturn(HandlerType.GET);
    when(ctx.path()).thenReturn("/api/hpc/test");

    io.javalin.http.Handler inner =
        c -> {
          throw new org.molgenis.emx2.MolgenisException("MOLGENIS_HPC_CREDENTIALS_KEY must be set");
        };

    io.javalin.http.Handler wrapped = HpcAuth.hpcHandler(inner);

    HpcException ex = assertThrows(HpcException.class, () -> wrapped.handle(ctx));
    assertEquals(503, ex.getStatus());
  }

  @Test
  void hpcHandler_credentialsKeyMissing_runtimeException_becomes503() {
    when(ctx.method()).thenReturn(HandlerType.GET);
    when(ctx.path()).thenReturn("/api/hpc/test");

    io.javalin.http.Handler inner =
        c -> {
          throw new RuntimeException("MOLGENIS_HPC_CREDENTIALS_KEY must be set");
        };

    io.javalin.http.Handler wrapped = HpcAuth.hpcHandler(inner);

    HpcException ex = assertThrows(HpcException.class, () -> wrapped.handle(ctx));
    assertEquals(503, ex.getStatus());
  }

  @Test
  void hpcHandler_hpcException500_logsAndRethrows() {
    when(ctx.method()).thenReturn(HandlerType.GET);
    when(ctx.path()).thenReturn("/api/hpc/test");

    io.javalin.http.Handler inner =
        c -> {
          throw HpcException.internal("server error", "req-1");
        };

    io.javalin.http.Handler wrapped = HpcAuth.hpcHandler(inner);

    HpcException ex = assertThrows(HpcException.class, () -> wrapped.handle(ctx));
    assertEquals(500, ex.getStatus());
  }

  @Test
  void hpcHandler_successDoesNotThrow() {
    io.javalin.http.Handler inner = c -> c.status(200);

    io.javalin.http.Handler wrapped = HpcAuth.hpcHandler(inner);

    assertDoesNotThrow(() -> wrapped.handle(ctx));
  }
}
