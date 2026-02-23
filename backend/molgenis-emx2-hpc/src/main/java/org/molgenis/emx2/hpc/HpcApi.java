package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.LinkedHashMap;
import java.util.Map;
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
 * Entry point for HPC API route registration. Initializes the HPC schema tables and wires all
 * sub-API handlers to Javalin routes under /api/hpc/*.
 *
 * <p>A before-handler on /api/hpc/* validates required protocol headers and HMAC authentication
 * centrally so individual handlers don't need to repeat this.
 */
public class HpcApi {

  private static final Logger logger = LoggerFactory.getLogger(HpcApi.class);
  private static final String HPC_SECRET_SETTING = "MOLGENIS_HPC_SHARED_SECRET";

  private HpcApi() {}

  /** Call from MolgenisWebservice.start() to register all HPC routes. */
  public static void create(Javalin app) {
    // Initialize database tables
    SqlDatabase database = new SqlDatabase(false);
    HpcSchemaInitializer.init(database, SYSTEM_SCHEMA);
    logger.info("HPC API: schema initialized in {}", SYSTEM_SCHEMA);

    // Load shared secret for HMAC verification
    HmacVerifier hmacVerifier = initHmacVerifier(database);

    // Create services
    WorkerService workerService = new WorkerService(database, SYSTEM_SCHEMA);
    JobService jobService = new JobService(database, SYSTEM_SCHEMA);
    ArtifactService artifactService = new ArtifactService(database, SYSTEM_SCHEMA);

    // Create API handlers
    WorkersApi workersApi = new WorkersApi(workerService);
    JobsApi jobsApi = new JobsApi(jobService);
    ArtifactsApi artifactsApi = new ArtifactsApi(artifactService);

    // Before-handler: validate protocol headers and HMAC auth on all HPC endpoints
    app.before(
        "/api/hpc/*",
        ctx -> {
          // Exempt health endpoint from auth
          if (ctx.path().equals("/api/hpc/health")) {
            return;
          }

          try {
            HpcHeaders.validateAll(ctx);
          } catch (IllegalArgumentException e) {
            ProblemDetail.send(ctx, 400, "Bad Request", e.getMessage(), null);
            throw new io.javalin.http.BadRequestResponse(e.getMessage());
          }

          // Authentication: try HMAC first, then JWT token, then reject if HMAC was configured
          String authHeader = ctx.header("Authorization");
          String tokenHeader = ctx.header("x-molgenis-token");
          if (authHeader != null && !authHeader.isBlank()) {
            // HMAC authentication (existing path)
            if (hmacVerifier != null) {
              verifyHmac(ctx, hmacVerifier);
            }
          } else if (tokenHeader != null && !tokenHeader.isBlank()) {
            // JWT token authentication
            verifyToken(ctx, database, tokenHeader);
          } else if (hmacVerifier != null) {
            // HMAC was configured but no auth credentials provided
            ProblemDetail.send(
                ctx,
                401,
                "Unauthorized",
                "Missing authentication: provide Authorization (HMAC) or x-molgenis-token header",
                ctx.header(HpcHeaders.REQUEST_ID));
            throw new io.javalin.http.UnauthorizedResponse("Missing authentication credentials");
          }
        });

    // Health endpoint (exempt from auth)
    app.get("/api/hpc/health", ctx -> healthCheck(ctx, database));

    // Worker endpoints
    app.post("/api/hpc/workers/register", workersApi::register);
    app.post(
        "/api/hpc/workers/{id}/heartbeat",
        ctx -> {
          String wid = ctx.pathParam("id");
          workerService.heartbeat(wid);
          ctx.json(Map.of("worker_id", wid, "status", "ok"));
        });

    // Job endpoints
    app.post("/api/hpc/jobs", jobsApi::createJob);
    app.get("/api/hpc/jobs", jobsApi::listJobs);
    app.get("/api/hpc/jobs/{id}", jobsApi::getJob);
    app.delete("/api/hpc/jobs/{id}", jobsApi::deleteJob);
    app.post("/api/hpc/jobs/{id}/claim", jobsApi::claimJob);
    app.post("/api/hpc/jobs/{id}/transition", jobsApi::transitionJob);
    app.post("/api/hpc/jobs/{id}/cancel", jobsApi::cancelJob);
    app.get("/api/hpc/jobs/{id}/transitions", jobsApi::getTransitions);

    // Artifact endpoints
    app.post("/api/hpc/artifacts", artifactsApi::createArtifact);
    app.get("/api/hpc/artifacts/{id}", artifactsApi::getArtifact);
    app.post("/api/hpc/artifacts/{id}/files", artifactsApi::uploadFile);
    app.get("/api/hpc/artifacts/{id}/files", artifactsApi::listFiles);
    app.post("/api/hpc/artifacts/{id}/commit", artifactsApi::commitArtifact);

    logger.info("HPC API: routes registered under /api/hpc/*");
  }

  private static HmacVerifier initHmacVerifier(SqlDatabase database) {
    try {
      String secret = database.getSetting(HPC_SECRET_SETTING);
      if (secret != null && !secret.isBlank()) {
        HmacVerifier verifier = new HmacVerifier(secret);
        logger.info("HPC API: HMAC authentication enabled");
        return verifier;
      }
      logger.warn(
          "HPC API: No {} configured — HMAC authentication disabled."
              + " Set this in database settings for production use.",
          HPC_SECRET_SETTING);
    } catch (Exception e) {
      logger.warn("HPC API: Failed to load shared secret — HMAC authentication disabled", e);
    }
    return null;
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
      verifier.verify(
          ctx.method().name(),
          ctx.path(),
          ctx.body(),
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

  private static void healthCheck(Context ctx, SqlDatabase database) {
    Map<String, Object> health = new LinkedHashMap<>();
    health.put("status", "ok");
    health.put("api_version", "2025-01");
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
