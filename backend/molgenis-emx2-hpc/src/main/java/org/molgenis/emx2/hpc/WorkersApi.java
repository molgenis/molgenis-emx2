package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.hpc.HpcFields.*;
import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.http.Context;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.protocol.HpcHeaders;
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

  @SuppressWarnings("unchecked")
  public void register(Context ctx) throws JsonProcessingException {
    String headerWorkerId = HpcHeaders.requireWorkerId(ctx);
    Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
    String workerId = (String) body.get(WORKER_ID);
    String hostname = (String) body.get(HOSTNAME);
    List<Map<String, Object>> capabilities = (List<Map<String, Object>>) body.get("capabilities");

    if (workerId == null || workerId.isBlank()) {
      throw HpcException.badRequest("worker_id is required", ctx.header(HpcHeaders.REQUEST_ID));
    }
    if (!headerWorkerId.equals(workerId)) {
      throw HpcException.badRequest(
          "worker_id in request body must match X-Worker-Id header",
          ctx.header(HpcHeaders.REQUEST_ID));
    }

    Row worker = workerService.registerOrHeartbeat(workerId, hostname, capabilities);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put(WORKER_ID, workerId);
    response.put(HOSTNAME, hostname);
    response.put(REGISTERED_AT, worker.getString(REGISTERED_AT));
    response.put(LAST_HEARTBEAT_AT, worker.getString(LAST_HEARTBEAT_AT));
    response.put(
        LINKS,
        Map.of(
            "self", Map.of("href", WORKERS_PATH + workerId, METHOD, "GET"),
            "heartbeat", Map.of("href", WORKERS_PATH + workerId + "/heartbeat", METHOD, "POST"),
            "jobs", Map.of("href", "/api/hpc/jobs?status=PENDING", METHOD, "GET")));

    ctx.status(200);
    ctx.json(response);
  }

  /** DELETE /api/hpc/workers/{id} — remove a worker, capabilities, and credentials. */
  public void deleteWorker(Context ctx) {
    String workerId = ctx.pathParam(ID);
    if (workerId.isBlank()) {
      throw HpcException.badRequest("worker id is required", ctx.header(HpcHeaders.REQUEST_ID));
    }

    Row deleted = workerService.deleteWorker(workerId);
    if (deleted == null) {
      throw HpcException.notFound(
          "Worker " + workerId + NOT_FOUND_SUFFIX, ctx.header(HpcHeaders.REQUEST_ID));
    }
    ctx.status(204);
  }
}
