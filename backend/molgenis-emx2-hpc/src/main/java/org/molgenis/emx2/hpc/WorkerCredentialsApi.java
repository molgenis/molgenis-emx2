package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.hpc.HpcApiUtils.requestId;
import static org.molgenis.emx2.hpc.HpcFields.*;
import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import com.fasterxml.jackson.core.JsonProcessingException;
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
  public void issueCredential(Context ctx) throws JsonProcessingException {
    String workerId = requirePathWorkerId(ctx);
    Map<String, Object> body =
        ctx.body().isBlank() ? Map.of() : MAPPER.readValue(ctx.body(), Map.class);
    String label = optionalString(body.get(LABEL));
    LocalDateTime expiresAt = parseOptionalDateTime(body.get(EXPIRES_AT), EXPIRES_AT, ctx);
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
    response.put(ID, issued.id());
    response.put(WORKER_ID, issued.workerId());
    response.put(STATUS, issued.status());
    response.put(LABEL, issued.label());
    response.put(CREATED_AT, issued.createdAt());
    response.put(EXPIRES_AT, issued.expiresAt());
    response.put("secret", issued.secret());
    String credPath = WORKERS_PATH + workerId + CREDENTIALS_PATH;
    response.put(
        LINKS,
        Map.of(
            "self", Map.of("href", credPath + "/" + issued.id(), METHOD, "GET"),
            "list", Map.of("href", credPath, METHOD, "GET"),
            "revoke", Map.of("href", credPath + "/" + issued.id() + "/revoke", METHOD, "POST")));
    ctx.status(201);
    ctx.json(response);
  }

  @SuppressWarnings("unchecked")
  public void rotateCredential(Context ctx) throws JsonProcessingException {
    String workerId = requirePathWorkerId(ctx);
    Map<String, Object> body =
        ctx.body().isBlank() ? Map.of() : MAPPER.readValue(ctx.body(), Map.class);
    String label = optionalString(body.get(LABEL));
    LocalDateTime expiresAt = parseOptionalDateTime(body.get(EXPIRES_AT), EXPIRES_AT, ctx);
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
    response.put(ID, issued.id());
    response.put(WORKER_ID, issued.workerId());
    response.put(STATUS, issued.status());
    response.put(LABEL, issued.label());
    response.put(CREATED_AT, issued.createdAt());
    response.put(EXPIRES_AT, issued.expiresAt());
    response.put("secret", issued.secret());
    response.put(
        LINKS,
        Map.of("list", Map.of("href", WORKERS_PATH + workerId + CREDENTIALS_PATH, METHOD, "GET")));
    ctx.status(200);
    ctx.json(response);
  }

  public void revokeCredential(Context ctx) {
    String workerId = requirePathWorkerId(ctx);
    String credentialId = ctx.pathParam("credentialId");
    if (credentialId.isBlank()) {
      throw HpcException.badRequest("credential id is required", requestId(ctx));
    }

    Row revoked = credentialService.revokeCredential(workerId, credentialId);
    if (revoked == null) {
      throw HpcException.notFound(
          "Credential " + credentialId + " for worker " + workerId + NOT_FOUND_SUFFIX,
          requestId(ctx));
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
        LINKS,
        Map.of("self", Map.of("href", WORKERS_PATH + workerId + CREDENTIALS_PATH, METHOD, "GET")));
    ctx.status(200);
    ctx.json(response);
  }

  private static String requirePathWorkerId(Context ctx) {
    String workerId = ctx.pathParam(ID);
    if (workerId.isBlank()) {
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
    } catch (java.time.format.DateTimeParseException e) {
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
