package org.molgenis.emx2.hpc.protocol;

import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.http.Context;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RFC 9457 (Problem Details for HTTP APIs) response builder. Produces application/problem+json
 * responses for error cases.
 */
public final class ProblemDetail {

  private ProblemDetail() {}

  /**
   * Sends a problem+json error response.
   *
   * @param ctx Javalin context
   * @param status HTTP status code
   * @param title Short human-readable summary
   * @param detail Longer explanation specific to this occurrence
   */
  public static void send(Context ctx, int status, String title, String detail) {
    send(ctx, status, title, detail, null);
  }

  /**
   * Sends a problem+json error response with an optional request ID for correlation.
   *
   * @param ctx Javalin context
   * @param status HTTP status code
   * @param title Short human-readable summary
   * @param detail Longer explanation
   * @param requestId the X-Request-Id for correlation, or null
   */
  public static void send(Context ctx, int status, String title, String detail, String requestId) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "about:blank");
    body.put("title", title);
    body.put("status", status);
    body.put("detail", detail);
    if (requestId != null) {
      body.put("instance", "urn:request:" + requestId);
    }
    ctx.status(status);
    ctx.contentType("application/problem+json");
    try {
      ctx.result(MAPPER.writeValueAsString(body));
    } catch (JsonProcessingException e) {
      // Fallback: plain text
      ctx.contentType("text/plain");
      ctx.result(title + ": " + detail);
    }
  }
}
