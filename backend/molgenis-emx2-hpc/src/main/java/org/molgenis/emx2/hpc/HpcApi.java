package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.hpc.protocol.HmacVerifier;
import org.molgenis.emx2.hpc.protocol.HpcHeaders;
import org.molgenis.emx2.hpc.protocol.ProblemDetail;
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
              ProblemDetail.send(
                  ctx,
                  503,
                  "Service Unavailable",
                  "HPC not configured — set "
                      + HPC_SECRET_SETTING
                      + " database setting on "
                      + SYSTEM_SCHEMA
                      + " to enable",
                  ctx.header(HpcHeaders.REQUEST_ID));
              throw new MolgenisException("HPC not configured");
            }
            hpcContext.set(hpc);
            logger.info("HPC API: initialized — schema tables created in {}", SYSTEM_SCHEMA);
          }
          ctx.attribute("hpcContext", hpc);

          // Protocol header validation
          try {
            HpcHeaders.validateAll(ctx);
          } catch (IllegalArgumentException e) {
            ProblemDetail.send(ctx, 400, "Bad Request", e.getMessage(), null);
            throw new io.javalin.http.BadRequestResponse(e.getMessage());
          }

          // Authentication: try HMAC first, then JWT token, then session cookie, then reject
          String authHeader = ctx.header("Authorization");
          String tokenHeader = ctx.header("x-molgenis-token");
          if (authHeader != null && !authHeader.isBlank()) {
            // HMAC authentication (daemon)
            if (hpc.hmacVerifier() != null) {
              verifyHmac(ctx, hpc.hmacVerifier());
            }
          } else if (tokenHeader != null && !tokenHeader.isBlank()) {
            // JWT token authentication
            verifyToken(ctx, database, tokenHeader);
          } else {
            // Session-based authentication (browser UI)
            String sessionUser = null;
            jakarta.servlet.http.HttpSession session = ctx.req().getSession(false);
            if (session != null) {
              sessionUser = (String) session.getAttribute("username");
            }
            if (sessionUser == null || sessionUser.isBlank()) {
              ProblemDetail.send(
                  ctx,
                  401,
                  "Unauthorized",
                  "Missing authentication: provide Authorization (HMAC), x-molgenis-token, or"
                      + " sign in",
                  ctx.header(HpcHeaders.REQUEST_ID));
              throw new io.javalin.http.UnauthorizedResponse("Missing authentication credentials");
            }
            logger.debug("HPC session auth: user '{}'", sessionUser);
          }
        });

    // Health endpoint (exempt from auth, always available)
    app.get("/api/hpc/health", ctx -> healthCheck(ctx, database, hpcContext.get() != null));

    // Worker endpoints
    app.post(
        "/api/hpc/workers/register",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.workersApi().register(ctx);
        });
    app.delete(
        "/api/hpc/workers/{id}",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.workersApi().deleteWorker(ctx);
        });
    app.post(
        "/api/hpc/workers/{id}/heartbeat",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          String wid = ctx.pathParam("id");
          hpc.workerService().heartbeat(wid);
          ctx.json(Map.of("worker_id", wid, "status", "ok"));
        });

    // Job endpoints
    app.post(
        "/api/hpc/jobs",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.jobsApi().createJob(ctx);
        });
    app.get(
        "/api/hpc/jobs",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.jobsApi().listJobs(ctx);
        });
    app.get(
        "/api/hpc/jobs/{id}",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.jobsApi().getJob(ctx);
        });
    app.delete(
        "/api/hpc/jobs/{id}",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.jobsApi().deleteJob(ctx);
        });
    app.post(
        "/api/hpc/jobs/{id}/claim",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.jobsApi().claimJob(ctx);
        });
    app.post(
        "/api/hpc/jobs/{id}/transition",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.jobsApi().transitionJob(ctx);
        });
    app.post(
        "/api/hpc/jobs/{id}/cancel",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.jobsApi().cancelJob(ctx);
        });
    app.get(
        "/api/hpc/jobs/{id}/transitions",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.jobsApi().getTransitions(ctx);
        });

    // Artifact endpoints
    app.post(
        "/api/hpc/artifacts",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.artifactsApi().createArtifact(ctx);
        });
    app.get(
        "/api/hpc/artifacts/{id}",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.artifactsApi().getArtifact(ctx);
        });
    app.delete(
        "/api/hpc/artifacts/{id}",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.artifactsApi().deleteArtifact(ctx);
        });
    app.post(
        "/api/hpc/artifacts/{id}/files",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.artifactsApi().uploadFile(ctx);
        });
    app.get(
        "/api/hpc/artifacts/{id}/files",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.artifactsApi().listFiles(ctx);
        });
    app.put(
        "/api/hpc/artifacts/{id}/files/{path}",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.artifactsApi().uploadFileByPath(ctx);
        });
    app.get(
        "/api/hpc/artifacts/{id}/files/{path}",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.artifactsApi().downloadFile(ctx);
        });
    app.head(
        "/api/hpc/artifacts/{id}/files/{path}",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.artifactsApi().headFile(ctx);
        });
    app.delete(
        "/api/hpc/artifacts/{id}/files/{path}",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.artifactsApi().deleteFile(ctx);
        });
    app.post(
        "/api/hpc/artifacts/{id}/commit",
        ctx -> {
          HpcContext hpc = ctx.attribute("hpcContext");
          hpc.artifactsApi().commitArtifact(ctx);
        });

    logger.info(
        "HPC API: routes registered (lazy init — tables created on first use when {} is set)",
        HPC_SECRET_SETTING);
  }

  /**
   * Attempts to initialize HPC services. Returns null if {@code MOLGENIS_HPC_SHARED_SECRET} is not
   * set. Reads the setting inside a transaction to get the current value from the database, not the
   * potentially stale in-memory cache.
   */
  private static HpcContext tryInit(SqlDatabase database) {
    // Read setting inside a tx so we get the current DB value, not the startup-time cache
    String[] secretHolder = new String[1];
    database.tx(
        db -> {
          db.becomeAdmin();
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

  private static void verifyToken(Context ctx, SqlDatabase database, String token) {
    try {
      String user = JWTgenerator.getUserFromToken(database, token);
      logger.debug("HPC JWT auth: authenticated as user '{}'", user);
    } catch (MolgenisException e) {
      logger.warn("HPC JWT auth failed: {} (path={}, ip={})", e.getMessage(), ctx.path(), ctx.ip());
      ProblemDetail.send(
          ctx, 401, "Unauthorized", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      throw new io.javalin.http.UnauthorizedResponse(e.getMessage());
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
      ProblemDetail.send(
          ctx, 401, "Unauthorized", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      throw new io.javalin.http.UnauthorizedResponse(e.getMessage());
    }
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
