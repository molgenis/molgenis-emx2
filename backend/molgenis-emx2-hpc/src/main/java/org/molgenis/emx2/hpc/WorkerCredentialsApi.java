package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.hpc.HpcApiUtils.requestId;
import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import io.javalin.http.Context;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.service.WorkerCredentialService;

/** Worker credential management endpoints. */
public class WorkerCredentialsApi {

  private final WorkerCredentialService credentialService;

  public WorkerCredentialsApi(WorkerCredentialService credentialService) {
    this.credentialService = credentialService;
  }

  @SuppressWarnings("unchecked")
  public void issueCredential(Context ctx) throws Exception {
    String workerId = requirePathWorkerId(ctx);
    Map<String, Object> body =
        ctx.body() == null || ctx.body().isBlank()
            ? Map.of()
            : MAPPER.readValue(ctx.body(), Map.class);
    String label = optionalString(body.get("label"));
    LocalDateTime expiresAt = parseOptionalDateTime(body.get("expires_at"), "expires_at", ctx);
    String createdBy =
        "USER".equals(ctx.attribute("hpcAuthMethod")) ? ctx.attribute("hpcAuthUser") : null;

    WorkerCredentialService.IssuedCredential issued;
    try {
      issued = credentialService.issueCredential(workerId, label, expiresAt, createdBy);
    } catch (RuntimeException e) {
      String rootMessage = rootMessage(e);
      if (isCredentialsKeyMissing(rootMessage)) {
        throw HpcException.serviceUnavailable(rootMessage, requestId(ctx));
      }
      if (rootMessage != null && rootMessage.contains("already has an active credential")) {
        throw HpcException.conflict(rootMessage, requestId(ctx));
      }
      throw e;
    }

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", issued.id());
    response.put("worker_id", issued.workerId());
    response.put("status", issued.status());
    response.put("label", issued.label());
    response.put("created_at", issued.createdAt());
    response.put("expires_at", issued.expiresAt());
    response.put("secret", issued.secret());
    response.put(
        "_links",
        Map.of(
            "self",
            Map.of(
                "href",
                "/api/hpc/workers/" + workerId + "/credentials/" + issued.id(),
                "method",
                "GET"),
            "list",
            Map.of("href", "/api/hpc/workers/" + workerId + "/credentials", "method", "GET"),
            "revoke",
            Map.of(
                "href",
                "/api/hpc/workers/" + workerId + "/credentials/" + issued.id() + "/revoke",
                "method",
                "POST")));
    ctx.status(201);
    ctx.json(response);
  }

  @SuppressWarnings("unchecked")
  public void rotateCredential(Context ctx) throws Exception {
    String workerId = requirePathWorkerId(ctx);
    Map<String, Object> body =
        ctx.body() == null || ctx.body().isBlank()
            ? Map.of()
            : MAPPER.readValue(ctx.body(), Map.class);
    String label = optionalString(body.get("label"));
    LocalDateTime expiresAt = parseOptionalDateTime(body.get("expires_at"), "expires_at", ctx);
    String createdBy =
        "USER".equals(ctx.attribute("hpcAuthMethod")) ? ctx.attribute("hpcAuthUser") : null;

    WorkerCredentialService.IssuedCredential issued;
    try {
      issued = credentialService.rotateCredential(workerId, label, expiresAt, createdBy);
    } catch (RuntimeException e) {
      String rootMessage = rootMessage(e);
      if (isCredentialsKeyMissing(rootMessage)) {
        throw HpcException.serviceUnavailable(rootMessage, requestId(ctx));
      }
      throw e;
    }

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", issued.id());
    response.put("worker_id", issued.workerId());
    response.put("status", issued.status());
    response.put("label", issued.label());
    response.put("created_at", issued.createdAt());
    response.put("expires_at", issued.expiresAt());
    response.put("secret", issued.secret());
    response.put(
        "_links",
        Map.of(
            "list",
            Map.of("href", "/api/hpc/workers/" + workerId + "/credentials", "method", "GET")));
    ctx.status(200);
    ctx.json(response);
  }

  public void revokeCredential(Context ctx) {
    String workerId = requirePathWorkerId(ctx);
    String credentialId = ctx.pathParam("credentialId");
    if (credentialId == null || credentialId.isBlank()) {
      throw HpcException.badRequest("credential id is required", requestId(ctx));
    }

    Row revoked = credentialService.revokeCredential(workerId, credentialId);
    if (revoked == null) {
      throw HpcException.notFound(
          "Credential " + credentialId + " for worker " + workerId + " not found", requestId(ctx));
    }
    ctx.status(200);
    ctx.json(WorkerCredentialService.toMetadata(revoked));
  }

  public void listCredentials(Context ctx) {
    String workerId = requirePathWorkerId(ctx);
    List<Map<String, Object>> items =
        credentialService.listCredentials(workerId).stream()
            .map(WorkerCredentialService::toMetadata)
            .toList();
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("items", items);
    response.put("count", items.size());
    response.put(
        "_links",
        Map.of(
            "self",
            Map.of("href", "/api/hpc/workers/" + workerId + "/credentials", "method", "GET")));
    ctx.status(200);
    ctx.json(response);
  }

  private static String requirePathWorkerId(Context ctx) {
    String workerId = ctx.pathParam("id");
    if (workerId == null || workerId.isBlank()) {
      throw HpcException.badRequest("worker id is required", requestId(ctx));
    }
    return workerId;
  }

  private static String optionalString(Object value) {
    if (value == null) {
      return null;
    }
    String s = String.valueOf(value).trim();
    return s.isEmpty() ? null : s;
  }

  private static LocalDateTime parseOptionalDateTime(Object value, String field, Context ctx) {
    if (value == null) {
      return null;
    }
    String s = String.valueOf(value).trim();
    if (s.isEmpty()) {
      return null;
    }
    try {
      return LocalDateTime.parse(s.replace(' ', 'T'));
    } catch (Exception e) {
      throw HpcException.badRequest(field + " must be an ISO-8601 datetime", requestId(ctx));
    }
  }

  private static boolean isCredentialsKeyMissing(String message) {
    return message != null
        && message.contains(WorkerCredentialService.CREDENTIALS_KEY_SETTING)
        && message.contains("must be set");
  }

  private static String rootMessage(Throwable throwable) {
    Throwable root = throwable;
    while (root.getCause() != null) {
      root = root.getCause();
    }
    return root.getMessage();
  }
}
