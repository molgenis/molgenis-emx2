package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;
import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import com.fasterxml.jackson.core.JacksonException;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
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
 * Entry point for HPC API route registration. Routes are registered unconditionally, but HPC schema
 * tables and services are initialized lazily on first request when {@code
 * MOLGENIS_HPC_SHARED_SECRET} is configured as a database setting.
 *
 * <p>When HPC is not configured, all endpoints except {@code /api/hpc/health} return 503 Service
 * Unavailable. The health endpoint always responds and indicates whether HPC is enabled.
 */
public class HpcApi {

  private static final Logger logger = LoggerFactory.getLogger(HpcApi.class);
  private static final String HPC_SECRET_SETTING = "MOLGENIS_HPC_SHARED_SECRET";

  private HpcApi() {}

  /** Holder for lazily-initialized HPC services. Created once when HPC is first needed. */
  private record HpcContext(
      HmacVerifier hmacVerifier,
      WorkerService workerService,
      JobService jobService,
      ArtifactService artifactService,
      WorkersApi workersApi,
      JobsApi jobsApi,
      ArtifactsApi artifactsApi) {}

  /** Call from MolgenisWebservice.start() to register all HPC routes. */
  public static void create(Javalin app) {
    SqlDatabase database = new SqlDatabase(false);

    // Lazy-init holder: initialized on first authenticated request when secret is configured
    AtomicReference<HpcContext> hpcContext = new AtomicReference<>(null);

    // Exception handler: converts HpcException to RFC 9457 problem+json responses
    app.exception(
        HpcException.class,
        (e, ctx) -> {
          ctx.status(e.getStatus());
          ctx.contentType("application/problem+json");
          try {
            ctx.result(MAPPER.writeValueAsString(e.toProblemDetail()));
          } catch (com.fasterxml.jackson.core.JsonProcessingException jsonEx) {
            ctx.contentType("text/plain");
            ctx.result(e.getTitle() + ": " + e.getMessage());
          }
        });

    // After-handler: propagate X-Trace-Id on all HPC responses
    app.after(
        "/api/hpc/*",
        ctx -> {
          String traceId = ctx.header(HpcHeaders.TRACE_ID);
          if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
          }
          ctx.header(HpcHeaders.TRACE_ID, traceId);
        });

    // Before-handler: lazy init + auth on all HPC endpoints
    app.before(
        "/api/hpc/*",
        ctx -> {
          // Health endpoint is always available, no init or auth needed
          if (ctx.path().equals("/api/hpc/health")) {
            return;
          }

          // Ensure HPC is initialized (cached after first successful init)
          HpcContext hpc = hpcContext.get();
          if (hpc == null) {
            hpc = tryInit(database);
            if (hpc == null) {
              logger.info(
                  "HPC API: not configured — set {} database setting to enable",
                  HPC_SECRET_SETTING);
              throw HpcException.serviceUnavailable(
                  "HPC not configured — set "
                      + HPC_SECRET_SETTING
                      + " database setting on "
                      + SYSTEM_SCHEMA
                      + " to enable",
                  ctx.header(HpcHeaders.REQUEST_ID));
            }
            hpcContext.set(hpc);
            logger.info("HPC API: initialized — schema tables created in {}", SYSTEM_SCHEMA);
          }
          ctx.attribute("hpcContext", hpc);

          // Protocol header validation
          try {
            HpcHeaders.validateAll(ctx);
          } catch (IllegalArgumentException e) {
            throw HpcException.badRequest(e.getMessage(), null);
          }

          // Authentication: try HMAC first, then JWT token, then session cookie, then reject
          String authHeader = ctx.header("Authorization");
          String tokenHeader = ctx.header("x-molgenis-token");
          if (authHeader != null && !authHeader.isBlank()) {
            // HMAC authentication (daemon — full access, no privilege check needed)
            if (hpc.hmacVerifier() != null) {
              verifyHmac(ctx, hpc.hmacVerifier());
            }
            ctx.attribute("hpcAuthMethod", "HMAC");
          } else if (tokenHeader != null && !tokenHeader.isBlank()) {
            // JWT token authentication
            String user = verifyToken(ctx, database, tokenHeader);
            ctx.attribute("hpcAuthMethod", "USER");
            ctx.attribute("hpcAuthUser", user);
          } else {
            // Session-based authentication (browser UI)
            String sessionUser = null;
            jakarta.servlet.http.HttpSession session = ctx.req().getSession(false);
            if (session != null) {
              sessionUser = (String) session.getAttribute("username");
            }
            if (sessionUser == null || sessionUser.isBlank()) {
              throw HpcException.unauthorized(
                  "Missing authentication: provide Authorization (HMAC), x-molgenis-token, or"
                      + " sign in",
                  ctx.header(HpcHeaders.REQUEST_ID));
            }
            ctx.attribute("hpcAuthMethod", "USER");
            ctx.attribute("hpcAuthUser", sessionUser);
            logger.debug("HPC session auth: user '{}'", sessionUser);
          }

          // Resolve effective privilege for user-authenticated requests
          if ("USER".equals(ctx.attribute("hpcAuthMethod"))) {
            String username = ctx.attribute("hpcAuthUser");
            Privileges privilege = resolveEffectivePrivilege(database, username);
            ctx.attribute("hpcPrivilege", privilege);
          }
        });

    // Health endpoint (exempt from auth, always available)
    app.get("/api/hpc/health", ctx -> healthCheck(ctx, database, hpcContext.get() != null));

    // Worker endpoints (MANAGER — daemon operations)
    app.post(
        "/api/hpc/workers/register",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.workersApi().register(ctx);
            }));
    app.delete(
        "/api/hpc/workers/{id}",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.workersApi().deleteWorker(ctx);
            }));
    app.post(
        "/api/hpc/workers/{id}/heartbeat",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute("hpcContext");
              String wid = ctx.pathParam("id");
              hpc.workerService().heartbeat(wid);
              ctx.json(Map.of("worker_id", wid, "status", "ok"));
            }));

    // Job endpoints
    app.post(
        "/api/hpc/jobs",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.EDITOR);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.jobsApi().createJob(ctx);
            }));
    app.get(
        "/api/hpc/jobs",
        hpcHandler(
            ctx -> {
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.jobsApi().listJobs(ctx);
            }));
    app.get(
        "/api/hpc/jobs/{id}",
        hpcHandler(
            ctx -> {
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.jobsApi().getJob(ctx);
            }));
    app.delete(
        "/api/hpc/jobs/{id}",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.jobsApi().deleteJob(ctx);
            }));
    app.post(
        "/api/hpc/jobs/{id}/claim",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.jobsApi().claimJob(ctx);
            }));
    app.post(
        "/api/hpc/jobs/{id}/transition",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.jobsApi().transitionJob(ctx);
            }));
    app.post(
        "/api/hpc/jobs/{id}/cancel",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.jobsApi().cancelJob(ctx);
            }));
    app.get(
        "/api/hpc/jobs/{id}/transitions",
        hpcHandler(
            ctx -> {
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.jobsApi().getTransitions(ctx);
            }));

    // Artifact endpoints
    app.post(
        "/api/hpc/artifacts",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.EDITOR);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.artifactsApi().createArtifact(ctx);
            }));
    app.get(
        "/api/hpc/artifacts/{id}",
        hpcHandler(
            ctx -> {
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.artifactsApi().getArtifact(ctx);
            }));
    app.delete(
        "/api/hpc/artifacts/{id}",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.artifactsApi().deleteArtifact(ctx);
            }));
    app.post(
        "/api/hpc/artifacts/{id}/files",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.EDITOR);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.artifactsApi().uploadFile(ctx);
            }));
    app.get(
        "/api/hpc/artifacts/{id}/files",
        hpcHandler(
            ctx -> {
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.artifactsApi().listFiles(ctx);
            }));
    app.put(
        "/api/hpc/artifacts/{id}/files/{path}",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.EDITOR);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.artifactsApi().uploadFileByPath(ctx);
            }));
    app.get(
        "/api/hpc/artifacts/{id}/files/{path}",
        hpcHandler(
            ctx -> {
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.artifactsApi().downloadFile(ctx);
            }));
    app.head(
        "/api/hpc/artifacts/{id}/files/{path}",
        hpcHandler(
            ctx -> {
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.artifactsApi().headFile(ctx);
            }));
    app.delete(
        "/api/hpc/artifacts/{id}/files/{path}",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.artifactsApi().deleteFile(ctx);
            }));
    app.post(
        "/api/hpc/artifacts/{id}/commit",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.EDITOR);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.artifactsApi().commitArtifact(ctx);
            }));

    logger.info(
        "HPC API: routes registered (lazy init — tables created on first use when {} is set)",
        HPC_SECRET_SETTING);
  }

  /**
   * Wraps a route handler to convert uncaught exceptions to {@link HpcException}. HpcExceptions are
   * re-thrown for the registered exception handler. Other exceptions are mapped to appropriate HTTP
   * status codes.
   */
  private static Handler hpcHandler(Handler inner) {
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

  /**
   * Attempts to initialize HPC services. Returns null if {@code MOLGENIS_HPC_SHARED_SECRET} is not
   * set. Reads the setting directly from the database via a transaction to pick up changes made
   * after this SqlDatabase instance was created (e.g. secret set via GraphQL on a fresh DB).
   */
  private static HpcContext tryInit(SqlDatabase database) {
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
    String secret = secretHolder[0];
    if (secret == null || secret.isBlank()) {
      return null;
    }

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
        hmacVerifier,
        workerService,
        jobService,
        artifactService,
        workersApi,
        jobsApi,
        artifactsApi);
  }

  private static String verifyToken(Context ctx, SqlDatabase database, String token) {
    try {
      String user = JWTgenerator.getUserFromToken(database, token);
      logger.debug("HPC JWT auth: authenticated as user '{}'", user);
      return user;
    } catch (MolgenisException e) {
      logger.warn("HPC JWT auth failed: {} (path={}, ip={})", e.getMessage(), ctx.path(), ctx.ip());
      throw HpcException.unauthorized(e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
    }
  }

  private static void verifyHmac(Context ctx, HmacVerifier verifier) {
    try {
      // For non-JSON bodies (binary uploads), sign over empty string to avoid
      // encoding issues with large binary payloads
      String ct = ctx.header("Content-Type");
      boolean isJson = ct != null && ct.startsWith("application/json");
      String body = isJson ? ctx.body() : "";
      // Use path + query string so the signature covers query parameters too
      String signedPath =
          ctx.queryString() != null ? ctx.path() + "?" + ctx.queryString() : ctx.path();
      verifier.verify(
          ctx.method().name(),
          signedPath,
          body,
          ctx.header("Authorization"),
          ctx.header(HpcHeaders.TIMESTAMP),
          ctx.header(HpcHeaders.NONCE));
    } catch (SecurityException e) {
      logger.warn("HPC auth failed: {} (path={}, ip={})", e.getMessage(), ctx.path(), ctx.ip());
      throw HpcException.unauthorized(e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
    }
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
   * access). Users with an explicit role on _SYSTEM_ get that role. Authenticated users without an
   * explicit role default to VIEWER (read-only).
   */
  private static Privileges resolveEffectivePrivilege(SqlDatabase database, String username) {
    if (username == null) {
      return Privileges.VIEWER;
    }
    Privileges[] result = {Privileges.VIEWER};
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
            // User has no access to system schema — default VIEWER
            logger.debug("Could not resolve role for user '{}': {}", username, e.getMessage());
          }
        });
    return result[0];
  }

  private static void healthCheck(Context ctx, SqlDatabase database, boolean hpcEnabled) {
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
}
