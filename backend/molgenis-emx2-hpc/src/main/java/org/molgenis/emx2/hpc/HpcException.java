package org.molgenis.emx2.hpc;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exception type for HPC API errors. Carries an HTTP status code, RFC 9457 title, and optional
 * request ID for correlation. Thrown from handlers and converted to {@code
 * application/problem+json} responses by the Javalin exception handler registered in {@link
 * HpcApi}.
 */
public class HpcException extends RuntimeException {

  private final int status;
  private final String title;
  private final String requestId;

  public HpcException(int status, String title, String detail, String requestId) {
    super(detail);
    this.status = status;
    this.title = title;
    this.requestId = requestId;
  }

  public int getStatus() {
    return status;
  }

  public String getTitle() {
    return title;
  }

  public String getRequestId() {
    return requestId;
  }

  /** Builds the RFC 9457 problem+json response body. */
  public Map<String, Object> toProblemDetail() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "about:blank");
    body.put("title", title);
    body.put("status", status);
    body.put("detail", getMessage());
    if (requestId != null) {
      body.put("instance", "urn:request:" + requestId);
    }
    return body;
  }

  public static HpcException badRequest(String detail, String requestId) {
    return new HpcException(400, "Bad Request", detail, requestId);
  }

  public static HpcException unauthorized(String detail, String requestId) {
    return new HpcException(401, "Unauthorized", detail, requestId);
  }

  public static HpcException forbidden(String detail, String requestId) {
    return new HpcException(403, "Forbidden", detail, requestId);
  }

  public static HpcException notFound(String detail, String requestId) {
    return new HpcException(404, "Not Found", detail, requestId);
  }

  public static HpcException conflict(String detail, String requestId) {
    return new HpcException(409, "Conflict", detail, requestId);
  }

  public static HpcException internal(String detail, String requestId) {
    return new HpcException(500, "Internal Server Error", detail, requestId);
  }

  public static HpcException serviceUnavailable(String detail, String requestId) {
    return new HpcException(503, "Service Unavailable", detail, requestId);
  }
}
