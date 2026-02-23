package org.molgenis.emx2.hpc.protocol;

import io.javalin.http.Context;

/**
 * Extracts and validates required protocol headers from incoming HPC API requests. Every request to
 * /api/hpc/* must include these headers.
 */
public final class HpcHeaders {

  public static final String REQUEST_ID = "X-Request-Id";
  public static final String TRACE_ID = "X-Trace-Id";
  public static final String TIMESTAMP = "X-Timestamp";
  public static final String NONCE = "X-Nonce";
  public static final String WORKER_ID = "X-Worker-Id";

  private HpcHeaders() {}

  /** Extracts the request ID header, throwing if absent. */
  public static String requireRequestId(Context ctx) {
    return requireHeader(ctx, REQUEST_ID);
  }

  /** Extracts the worker ID header (optional for some endpoints). */
  public static String getWorkerId(Context ctx) {
    return ctx.header(WORKER_ID);
  }

  /** Extracts the API version and validates it against the current version. */
  public static void validateApiVersion(Context ctx) {
    ApiVersion.validate(ctx.header(ApiVersion.HEADER_NAME));
  }

  /** Validates all required protocol headers are present. */
  public static void validateAll(Context ctx) {
    validateApiVersion(ctx);
    requireHeader(ctx, REQUEST_ID);
    requireHeader(ctx, TIMESTAMP);
  }

  private static String requireHeader(Context ctx, String name) {
    String value = ctx.header(name);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing required header: " + name);
    }
    return value;
  }
}
