package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;

import com.fasterxml.jackson.core.JacksonException;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.hpc.protocol.HmacVerifier;
import org.molgenis.emx2.hpc.protocol.HpcHeaders;
import org.molgenis.emx2.hpc.service.ArtifactService;
import org.molgenis.emx2.hpc.service.HpcSchemaInitializer;
import org.molgenis.emx2.hpc.service.JobService;
import org.molgenis.emx2.hpc.service.WorkerCredentialService;
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

  static final String HPC_ENABLED_SETTING = "MOLGENIS_HPC_ENABLED";
  static final String HPC_CREDENTIALS_KEY_SETTING = "MOLGENIS_HPC_CREDENTIALS_KEY";

  // Attribute keys shared between HpcAuth and HpcApi
  static final String HPC_CONTEXT_ATTR = "hpcContext";
  static final String HPC_AUTH_METHOD_ATTR = "hpcAuthMethod";
  static final String HPC_AUTH_USER_ATTR = "hpcAuthUser";
  static final String HPC_PRIVILEGE_ATTR = "hpcPrivilege";

  private static final long SETTINGS_CACHE_TTL_MILLIS = 5000;
  private static final SettingsProvider SETTINGS_PROVIDER =
      new SettingsProvider(SETTINGS_CACHE_TTL_MILLIS);

  private HpcAuth() {}

  /** Holder for lazily-initialized HPC services. Created once when HPC is first needed. */
  record HpcContext(
      WorkerService workerService,
      WorkerCredentialService workerCredentialService,
      JobService jobService,
      ArtifactService artifactService,
      WorkersApi workersApi,
      WorkerCredentialsApi workerCredentialsApi,
      JobsApi jobsApi,
      ArtifactsApi artifactsApi) {}

  /** Reads setting values via a short-lived TTL cache to avoid DB/cache churn on hot paths. */
  static String readSetting(SqlDatabase database, String key) {
    return SETTINGS_PROVIDER.get(database, key);
  }

  static void invalidateSettingsCache() {
    SETTINGS_PROVIDER.invalidateAll();
  }

  static boolean isHpcEnabled(SqlDatabase database) {
    String enabled = readSetting(database, HPC_ENABLED_SETTING);
    return enabled != null && Boolean.parseBoolean(enabled.trim());
  }

  /** Initializes HPC services for an enabled HPC deployment. */
  static HpcContext initContext(SqlDatabase database) {
    HpcSchemaInitializer.init(database, SYSTEM_SCHEMA);

    WorkerService workerService = new WorkerService(database, SYSTEM_SCHEMA);
    WorkerCredentialService workerCredentialService =
        new WorkerCredentialService(database, SYSTEM_SCHEMA);
    JobService jobService = new JobService(database, SYSTEM_SCHEMA);
    ArtifactService artifactService = new ArtifactService(database, SYSTEM_SCHEMA);

    WorkersApi workersApi = new WorkersApi(workerService);
    WorkerCredentialsApi workerCredentialsApi = new WorkerCredentialsApi(workerCredentialService);
    JobsApi jobsApi = new JobsApi(jobService, artifactService);
    ArtifactsApi artifactsApi = new ArtifactsApi(artifactService, database);

    return new HpcContext(
        workerService,
        workerCredentialService,
        jobService,
        artifactService,
        workersApi,
        workerCredentialsApi,
        jobsApi,
        artifactsApi);
  }

  static String verifyToken(Context ctx, SqlDatabase database, String token) {
    try {
      String user = JWTgenerator.getUserFromToken(database, token);
      logger.debug("HPC JWT auth: authenticated as user '{}'", user);
      return user;
    } catch (MolgenisException e) {
      logger.warn(
          "HPC JWT auth failed: {} (path={}, ip={})", e.getMessage(), ctx.path(), ctx.ip(), e);
      throw HpcException.unauthorized(e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID), e);
    }
  }

  static void verifyHmac(Context ctx, WorkerCredentialService workerCredentialService) {
    try {
      String workerId = ctx.header(HpcHeaders.WORKER_ID);
      if (workerId == null || workerId.isBlank()) {
        throw new SecurityException("Missing X-Worker-Id header");
      }
      WorkerCredentialService.AuthenticatedCredential credential =
          workerCredentialService.resolveActiveCredential(workerId);
      HmacVerifier verifier = new HmacVerifier(credential.secret());

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
      workerCredentialService.markCredentialUsed(credential.id());
    } catch (IllegalStateException e) {
      logger.warn("HPC HMAC auth unavailable: {}", e.getMessage(), e);
      throw HpcException.serviceUnavailable(e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID), e);
    } catch (SecurityException e) {
      logger.warn("HPC auth failed: {} (path={}, ip={})", e.getMessage(), ctx.path(), ctx.ip(), e);
      throw HpcException.unauthorized(e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID), e);
    }
  }

  /** Returns true if the request likely has a body (PUT/POST with content). */
  static boolean hasBody(Context ctx) {
    String method = ctx.method().name();
    if ("GET".equals(method) || "HEAD".equals(method) || "DELETE".equals(method)) {
      return false;
    }
    String transferEncoding = ctx.header("Transfer-Encoding");
    if (transferEncoding != null && !transferEncoding.isBlank()) {
      return true;
    }
    String contentLength = ctx.header("Content-Length");
    if (contentLength == null || contentLength.isBlank()) {
      return false;
    }
    try {
      return Long.parseLong(contentLength) > 0;
    } catch (NumberFormatException e) {
      // Unparseable Content-Length — assume body is present to be safe
      logger.debug("Unparseable Content-Length header: {}", contentLength, e);
      return true;
    }
  }

  /**
   * Checks that the current request has at least the required privilege level. HMAC-authenticated
   * requests (daemon) always pass. For user-authenticated requests, compares the resolved privilege
   * (stored by the before-handler) against the required level. Throws {@link HpcException} with 403
   * if the check fails.
   */
  static void requireHpcPrivilege(Context ctx, Privileges required) {
    if ("HMAC".equals(ctx.attribute(HPC_AUTH_METHOD_ATTR))) {
      return;
    }
    Privileges effective = ctx.attribute(HPC_PRIVILEGE_ATTR);
    if (effective != null && effective.ordinal() >= required.ordinal()) {
      return;
    }
    throw HpcException.forbidden(
        "Requires " + required + " privilege on HPC resources", ctx.header(HpcHeaders.REQUEST_ID));
  }

  /**
   * Requires user authentication (no worker principal) and a minimum system privilege.
   *
   * <p>Used for credential-management endpoints which workers are not allowed to call.
   */
  static void requireUserHpcPrivilege(Context ctx, Privileges required) {
    if ("HMAC".equals(ctx.attribute(HPC_AUTH_METHOD_ATTR))) {
      throw HpcException.forbidden(
          "Worker principal is not allowed on credential-management endpoints",
          ctx.header(HpcHeaders.REQUEST_ID));
    }
    requireHpcPrivilege(ctx, required);
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
          result[0] = resolvePrivilegeInTransaction(db, username);
        });
    return result[0];
  }

  private static Privileges resolvePrivilegeInTransaction(
      org.molgenis.emx2.Database db, String username) {
    if (db.isAdmin()) {
      return Privileges.OWNER;
    }
    try {
      Schema schema = db.getSchema(SYSTEM_SCHEMA);
      if (schema == null) {
        return null;
      }
      String role = schema.getRoleForActiveUser();
      if (role == null) {
        return null;
      }
      return mapRoleToPrivilege(role);
    } catch (Exception e) {
      logger.debug("Could not resolve role for user '{}': {}", username, e.getMessage());
      return null;
    }
  }

  private static Privileges mapRoleToPrivilege(String role) {
    for (Privileges p : Privileges.values()) {
      if (p.toString().equalsIgnoreCase(role)) {
        return p;
      }
    }
    return null;
  }

  static void healthCheck(
      Context ctx, SqlDatabase database, boolean hpcEnabled, boolean hpcInitialized) {
    Map<String, Object> health = new LinkedHashMap<>();
    health.put("status", "ok");
    health.put("api_version", "2025-01");
    health.put("hpc_enabled", hpcEnabled);
    health.put("hpc_initialized", hpcInitialized);
    health.put("worker_auth_mode", "per-worker-hmac");
    String credentialsKey = readSetting(database, HPC_CREDENTIALS_KEY_SETTING);
    health.put(
        "credentials_key_configured", credentialsKey != null && !credentialsKey.trim().isEmpty());
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
      String requestId = ctx.header(HpcHeaders.REQUEST_ID);
      String traceId = ctx.header(HpcHeaders.TRACE_ID);
      try {
        inner.handle(ctx);
      } catch (HpcException e) {
        if (e.getStatus() >= 500) {
          logger.error(
              "HPC error {} {} -> {} {} [request_id={}, trace_id={}]",
              ctx.method().name(),
              ctx.path(),
              e.getStatus(),
              e.getMessage(),
              requestId,
              traceId);
        }
        throw e;
      } catch (IllegalArgumentException e) {
        throw HpcException.badRequest(errorMessage(e), requestId, e);
      } catch (JacksonException e) {
        throw HpcException.badRequest(
            "Invalid request body: " + e.getOriginalMessage(), requestId, e);
      } catch (MolgenisException e) {
        throw handleMolgenisException(e, ctx, requestId, traceId);
      } catch (Exception e) {
        throw handleUnexpectedException(e, ctx, requestId, traceId);
      }
    };
  }

  private static HpcException handleMolgenisException(
      MolgenisException e, Context ctx, String requestId, String traceId) {
    if (isCredentialsKeyMissing(e)) {
      return HpcException.serviceUnavailable(rootMessage(e), requestId);
    }
    String method = ctx.method().name();
    String path = ctx.path();
    logger.error(
        "HPC backend exception on {} {} [request_id={}, trace_id={}]",
        method,
        path,
        requestId,
        traceId,
        e);
    if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")) {
      return HpcException.notFound(e.getMessage(), requestId);
    }
    return HpcException.internal(
        "Internal error processing request; check server logs with request id " + requestId,
        requestId);
  }

  private static HpcException handleUnexpectedException(
      Exception e, Context ctx, String requestId, String traceId) {
    if (isCredentialsKeyMissing(e)) {
      return HpcException.serviceUnavailable(rootMessage(e), requestId);
    }
    String method = ctx.method().name();
    String path = ctx.path();
    logger.error(
        "Unhandled HPC exception on {} {} [request_id={}, trace_id={}]",
        method,
        path,
        requestId,
        traceId,
        e);
    return HpcException.internal(
        "Internal error processing request; check server logs with request id " + requestId,
        requestId);
  }

  private static String errorMessage(Throwable e) {
    if (e.getMessage() != null && !e.getMessage().isBlank()) {
      return e.getMessage();
    }
    return e.getClass().getSimpleName();
  }

  private static boolean isCredentialsKeyMissing(Throwable e) {
    String message = rootMessage(e);
    return message != null
        && message.contains(HPC_CREDENTIALS_KEY_SETTING)
        && message.contains("must be set");
  }

  private static String rootMessage(Throwable e) {
    Throwable root = e;
    while (root.getCause() != null) {
      root = root.getCause();
    }
    return root.getMessage();
  }

  private record CachedSetting(String value, long expiresAtMillis) {}

  private static final class SettingsProvider {
    private final long ttlMillis;
    private final ConcurrentHashMap<String, CachedSetting> cache = new ConcurrentHashMap<>();

    private SettingsProvider(long ttlMillis) {
      this.ttlMillis = ttlMillis;
    }

    String get(SqlDatabase database, String key) {
      long now = System.currentTimeMillis();
      CachedSetting cached = cache.get(key);
      if (cached != null && cached.expiresAtMillis() > now) {
        return cached.value();
      }
      String[] valueHolder = new String[1];
      database.tx(
          db -> {
            db.becomeAdmin();
            valueHolder[0] = db.getSetting(key);
          });
      cache.put(key, new CachedSetting(valueHolder[0], now + ttlMillis));
      return valueHolder[0];
    }

    void invalidateAll() {
      cache.clear();
    }
  }
}
