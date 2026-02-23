package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import io.javalin.http.Context;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.protocol.HpcHeaders;
import org.molgenis.emx2.hpc.protocol.ProblemDetail;
import org.molgenis.emx2.hpc.service.WorkerService;

/**
 * Worker registration endpoint. POST /api/hpc/workers/register
 *
 * <p>Workers call this on startup and periodically as a heartbeat. The request body contains worker
 * metadata and a list of (processor, profile, maxConcurrentJobs) capabilities.
 */
public class WorkersApi {

  private final WorkerService workerService;

  public WorkersApi(WorkerService workerService) {
    this.workerService = workerService;
  }

  /**
   * POST /api/hpc/workers/register
   *
   * <p>Request body:
   *
   * <pre>
   * {
   *   "worker_id": "hpc-cluster-01",
   *   "hostname": "login-node.cluster.local",
   *   "capabilities": [
   *     {"processor": "text-embedding", "profile": "gpu-medium", "max_concurrent_jobs": 4}
   *   ]
   * }
   * </pre>
   */
  @SuppressWarnings("unchecked")
  public void register(Context ctx) {
    try {
      Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
      String workerId = (String) body.get("worker_id");
      String hostname = (String) body.get("hostname");
      List<Map<String, Object>> capabilities = (List<Map<String, Object>>) body.get("capabilities");

      if (workerId == null || workerId.isBlank()) {
        ProblemDetail.send(
            ctx, 400, "Bad Request", "worker_id is required", ctx.header(HpcHeaders.REQUEST_ID));
        return;
      }

      Row worker = workerService.registerOrHeartbeat(workerId, hostname, capabilities);

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("worker_id", workerId);
      response.put("hostname", hostname);
      response.put("registered_at", worker.getString("registered_at"));
      response.put("last_heartbeat_at", worker.getString("last_heartbeat_at"));
      response.put(
          "_links",
          Map.of(
              "self", Map.of("href", "/api/hpc/workers/" + workerId, "method", "GET"),
              "heartbeat", Map.of("href", "/api/hpc/workers/register", "method", "POST"),
              "jobs", Map.of("href", "/api/hpc/jobs?status=PENDING", "method", "GET")));

      ctx.status(200);
      ctx.json(response);
    } catch (Exception e) {
      ProblemDetail.send(
          ctx, 500, "Internal Server Error", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
    }
  }

  /** DELETE /api/hpc/workers/{id} — remove a worker and its capabilities. */
  public void deleteWorker(Context ctx) {
    String workerId = ctx.pathParam("id");
    if (workerId == null || workerId.isBlank()) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", "worker id is required", ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
    try {
      Row deleted = workerService.deleteWorker(workerId);
      if (deleted == null) {
        ProblemDetail.send(
            ctx,
            404,
            "Not Found",
            "Worker " + workerId + " not found",
            ctx.header(HpcHeaders.REQUEST_ID));
        return;
      }
      ctx.status(204);
    } catch (Exception e) {
      ProblemDetail.send(
          ctx, 500, "Internal Server Error", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
    }
  }
}
