package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;

import com.fasterxml.jackson.core.JacksonException;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.util.LinkedHashMap;
import java.util.Map;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.hpc.protocol.HmacVerifier;
import org.molgenis.emx2.hpc.protocol.HpcHeaders;
import org.molgenis.emx2.hpc.service.ArtifactService;
import org.molgenis.emx2.hpc.service.HpcSchemaInitializer;
import org.molgenis.emx2.hpc.service.JobService;
import org.molgenis.emx2.hpc.service.WorkerService;
import org.molgenis.emx2.sql.JWTgenerator;
import org.molgenis.emx2.sql.SqlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication and middleware infrastructure for the HPC API. Extracted from {@link HpcApi} to
 * separate concerns: this class handles auth cascade (HMAC / JWT / session), privilege resolution,
 * lazy context initialization, and the exception-wrapping handler. {@link HpcApi} handles route
 * registration.
 */
class HpcAuth {

  private static final Logger logger = LoggerFactory.getLogger(HpcAuth.class);

  static final String HPC_SECRET_SETTING = "MOLGENIS_HPC_SHARED_SECRET";

  private HpcAuth() {}

  /** Holder for lazily-initialized HPC services. Created once when HPC is first needed. */
  record HpcContext(
      String sharedSecret,
      HmacVerifier hmacVerifier,
      WorkerService workerService,
      JobService jobService,
      ArtifactService artifactService,
      WorkersApi workersApi,
      JobsApi jobsApi,
      ArtifactsApi artifactsApi) {}

  /** Reads the latest shared secret directly from the database setting table. */
  static String readSharedSecret(SqlDatabase database) {
    // Read setting via tx + becomeAdmin so we get the current DB value,
    // not the potentially stale in-memory cache of this SqlDatabase instance
    String[] secretHolder = new String[1];
    database.tx(
        db -> {
          db.becomeAdmin();
          // clearCache reloads settings from the database; safe inside a tx copy
          db.clearCache();
          secretHolder[0] = db.getSetting(HPC_SECRET_SETTING);
        });
    return secretHolder[0];
  }

  static boolean isHpcConfigured(SqlDatabase database) {
    String secret = readSharedSecret(database);
    return secret != null && !secret.isBlank();
  }

  /** Initializes HPC services for a specific configured shared secret. */
  static HpcContext initContext(SqlDatabase database, String secret) {
    HpcSchemaInitializer.init(database, SYSTEM_SCHEMA);

    HmacVerifier hmacVerifier;
    try {
      hmacVerifier = new HmacVerifier(secret);
      logger.info("HPC API: HMAC authentication enabled");
    } catch (Exception e) {
      logger.warn("HPC API: Failed to create HMAC verifier — HMAC authentication disabled", e);
      hmacVerifier = null;
    }

    WorkerService workerService = new WorkerService(database, SYSTEM_SCHEMA);
    JobService jobService = new JobService(database, SYSTEM_SCHEMA);
    ArtifactService artifactService = new ArtifactService(database, SYSTEM_SCHEMA);

    WorkersApi workersApi = new WorkersApi(workerService);
    JobsApi jobsApi = new JobsApi(jobService, artifactService, workerService);
    ArtifactsApi artifactsApi = new ArtifactsApi(artifactService);

    return new HpcContext(
        secret,
        hmacVerifier,
        workerService,
        jobService,
        artifactService,
        workersApi,
        jobsApi,
        artifactsApi);
  }

  static String verifyToken(Context ctx, SqlDatabase database, String token) {
    try {
      String user = JWTgenerator.getUserFromToken(database, token);
      logger.debug("HPC JWT auth: authenticated as user '{}'", user);
      return user;
    } catch (MolgenisException e) {
      logger.warn("HPC JWT auth failed: {} (path={}, ip={})", e.getMessage(), ctx.path(), ctx.ip());
      throw HpcException.unauthorized(e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
    }
  }

  static void verifyHmac(Context ctx, HmacVerifier verifier) {
    try {
      String ct = ctx.header("Content-Type");
      boolean isJson = ct != null && ct.startsWith("application/json");

      // For non-JSON bodies (binary uploads), the client MUST send a Content-SHA256
      // header with the hex-encoded SHA-256 of the body. This is used directly as
      // the body hash in the HMAC canonical string, providing integrity without
      // requiring the server to buffer the entire body before signature verification.
      String body;
      String contentSha256;
      if (isJson) {
        body = ctx.body();
        contentSha256 = null;
      } else {
        body = "";
        contentSha256 = ctx.header(HpcHeaders.CONTENT_SHA256);
        if (hasBody(ctx) && (contentSha256 == null || contentSha256.isBlank())) {
          throw new SecurityException(
              "Content-SHA256 header is required for non-JSON request bodies");
        }
      }

      // Use path + query string so the signature covers query parameters too
      String signedPath =
          ctx.queryString() != null ? ctx.path() + "?" + ctx.queryString() : ctx.path();
      verifier.verify(
          ctx.method().name(),
          signedPath,
          body,
          ctx.header("Authorization"),
          ctx.header(HpcHeaders.TIMESTAMP),
          ctx.header(HpcHeaders.NONCE),
          contentSha256);
    } catch (SecurityException e) {
      logger.warn("HPC auth failed: {} (path={}, ip={})", e.getMessage(), ctx.path(), ctx.ip());
      throw HpcException.unauthorized(e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
    }
  }

  /** Returns true if the request likely has a body (PUT/POST with content). */
  static boolean hasBody(Context ctx) {
    String method = ctx.method().name();
    if ("GET".equals(method) || "HEAD".equals(method) || "DELETE".equals(method)) {
      return false;
    }
    String contentLength = ctx.header("Content-Length");
    return contentLength != null && !"0".equals(contentLength);
  }

  /**
   * Checks that the current request has at least the required privilege level. HMAC-authenticated
   * requests (daemon) always pass. For user-authenticated requests, compares the resolved privilege
   * (stored by the before-handler) against the required level. Throws {@link HpcException} with 403
   * if the check fails.
   */
  static void requireHpcPrivilege(Context ctx, Privileges required) {
    if ("HMAC".equals(ctx.attribute("hpcAuthMethod"))) {
      return;
    }
    Privileges effective = ctx.attribute("hpcPrivilege");
    if (effective != null && effective.ordinal() >= required.ordinal()) {
      return;
    }
    throw HpcException.forbidden(
        "Requires " + required + " privilege on HPC resources", ctx.header(HpcHeaders.REQUEST_ID));
  }

  /**
   * Resolves the effective privilege for a user on the system schema. Admin users get OWNER (full
   * access). Users with an explicit role on _SYSTEM_ get that role. Users without an explicit role
   * get no HPC access.
   */
  static Privileges resolveEffectivePrivilege(SqlDatabase database, String username) {
    if (username == null) {
      return null;
    }
    Privileges[] result = {null};
    database.tx(
        db -> {
          db.setActiveUser(username);
          if (db.isAdmin()) {
            result[0] = Privileges.OWNER;
            return;
          }
          try {
            Schema schema = db.getSchema(SYSTEM_SCHEMA);
            if (schema != null) {
              String role = schema.getRoleForActiveUser();
              if (role != null) {
                for (Privileges p : Privileges.values()) {
                  if (p.toString().equalsIgnoreCase(role)) {
                    result[0] = p;
                    return;
                  }
                }
              }
            }
          } catch (Exception e) {
            // User has no access to system schema.
            logger.debug("Could not resolve role for user '{}': {}", username, e.getMessage());
          }
        });
    return result[0];
  }

  static void healthCheck(Context ctx, SqlDatabase database, boolean hpcEnabled) {
    Map<String, Object> health = new LinkedHashMap<>();
    health.put("status", "ok");
    health.put("api_version", "2025-01");
    health.put("hpc_enabled", hpcEnabled);
    try {
      database.tx(
          db -> {
            db.becomeAdmin();
            // Simple connectivity check
            db.getSchema(SYSTEM_SCHEMA).getTableNames();
          });
      health.put("database", "connected");
    } catch (Exception e) {
      health.put("database", "error");
      health.put("database_error", e.getMessage());
      health.put("status", "degraded");
    }
    ctx.json(health);
  }

  /**
   * Wraps a route handler to convert uncaught exceptions to {@link HpcException}. HpcExceptions are
   * re-thrown for the registered exception handler. Other exceptions are mapped to appropriate HTTP
   * status codes.
   */
  static Handler hpcHandler(Handler inner) {
    return ctx -> {
      try {
        inner.handle(ctx);
      } catch (HpcException e) {
        throw e;
      } catch (IllegalArgumentException e) {
        throw HpcException.badRequest(e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      } catch (JacksonException e) {
        throw HpcException.badRequest(
            "Invalid request body: " + e.getOriginalMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      } catch (MolgenisException e) {
        if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")) {
          throw HpcException.notFound(e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
        }
        throw HpcException.internal(e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      } catch (Exception e) {
        throw HpcException.internal(e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      }
    };
  }
}
