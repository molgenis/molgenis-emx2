package org.molgenis.emx2.hpc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.service.WorkerService;

class WorkersApiTest {

  private WorkerService workerService;
  private WorkersApi api;
  private Context ctx;

  private static final String WORKER_ID = "worker-1";

  @BeforeEach
  void setUp() {
    workerService = mock(WorkerService.class);
    api = new WorkersApi(workerService);
    ctx = mock(Context.class);
    when(ctx.header("X-Worker-Id")).thenReturn(WORKER_ID);
    when(ctx.header("X-Request-Id")).thenReturn("req-1");
  }

  // ── register ───────────────────────────────────────────────────────────────

  @Test
  void register_success() throws Exception {
    when(ctx.body())
        .thenReturn(
            "{\"worker_id\":\"worker-1\",\"hostname\":\"node01\","
                + "\"capabilities\":[{\"processor\":\"text\",\"profile\":\"gpu\"}]}");
    Row workerRow = new Row();
    workerRow.set("registered_at", "2025-01-01T00:00:00");
    workerRow.set("last_heartbeat_at", "2025-01-01T00:00:00");
    when(workerService.registerOrHeartbeat(eq(WORKER_ID), eq("node01"), anyList()))
        .thenReturn(workerRow);

    api.register(ctx);

    verify(ctx).status(200);
    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return WORKER_ID.equals(map.get("worker_id"))
                      && "node01".equals(map.get("hostname"));
                }));
  }

  @Test
  void register_missingWorkerId_throws400() {
    when(ctx.body()).thenReturn("{\"hostname\":\"node01\"}");

    HpcException ex = assertThrows(HpcException.class, () -> api.register(ctx));
    assertEquals(400, ex.getStatus());
    assertTrue(ex.getMessage().contains("worker_id is required"));
  }

  @Test
  void register_blankWorkerId_throws400() {
    when(ctx.body()).thenReturn("{\"worker_id\":\"\",\"hostname\":\"node01\"}");

    HpcException ex = assertThrows(HpcException.class, () -> api.register(ctx));
    assertEquals(400, ex.getStatus());
  }

  @Test
  void register_headerMismatch_throws400() {
    when(ctx.header("X-Worker-Id")).thenReturn("different-worker");
    when(ctx.body()).thenReturn("{\"worker_id\":\"worker-1\",\"hostname\":\"node01\"}");

    HpcException ex = assertThrows(HpcException.class, () -> api.register(ctx));
    assertEquals(400, ex.getStatus());
    assertTrue(ex.getMessage().contains("must match X-Worker-Id header"));
  }

  @Test
  void register_missingWorkerIdHeader_throwsIllegalArgument() {
    when(ctx.header("X-Worker-Id")).thenReturn(null);
    when(ctx.body()).thenReturn("{\"worker_id\":\"worker-1\",\"hostname\":\"node01\"}");

    // HpcHeaders.requireWorkerId throws IllegalArgumentException when header is missing
    assertThrows(IllegalArgumentException.class, () -> api.register(ctx));
  }

  // ── deleteWorker ───────────────────────────────────────────────────────────

  @Test
  void deleteWorker_success() {
    when(ctx.pathParam("id")).thenReturn(WORKER_ID);
    Row deletedRow = new Row();
    deletedRow.set("worker_id", WORKER_ID);
    when(workerService.deleteWorker(WORKER_ID)).thenReturn(deletedRow);

    api.deleteWorker(ctx);

    verify(ctx).status(204);
  }

  @Test
  void deleteWorker_notFound() {
    when(ctx.pathParam("id")).thenReturn(WORKER_ID);
    when(workerService.deleteWorker(WORKER_ID)).thenReturn(null);

    HpcException ex = assertThrows(HpcException.class, () -> api.deleteWorker(ctx));
    assertEquals(404, ex.getStatus());
    assertTrue(ex.getMessage().contains("not found"));
  }

  @Test
  void deleteWorker_blankId_throws400() {
    when(ctx.pathParam("id")).thenReturn("");

    HpcException ex = assertThrows(HpcException.class, () -> api.deleteWorker(ctx));
    assertEquals(400, ex.getStatus());
  }
}
