package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;
import static org.molgenis.emx2.hpc.HpcAuth.*;
import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import io.javalin.Javalin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.hpc.HpcAuth.HpcContext;
import org.molgenis.emx2.hpc.protocol.HpcHeaders;
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
 *
 * <p>Authentication and middleware infrastructure is provided by {@link HpcAuth}.
 */
public class HpcApi {

  private static final Logger logger = LoggerFactory.getLogger(HpcApi.class);

  private HpcApi() {}

  /** Call from MolgenisWebservice.start() to register all HPC routes. */
  public static void create(Javalin app) {
    SqlDatabase database = new SqlDatabase(false);

    // Lazy-init holder: initialized on first request and refreshed when secret rotates
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

          String configuredSecret = readSharedSecret(database);
          if (configuredSecret == null || configuredSecret.isBlank()) {
            if (hpcContext.getAndSet(null) != null) {
              logger.info("HPC API: disabled — {} was cleared", HPC_SECRET_SETTING);
            }
            logger.info(
                "HPC API: not configured — set {} database setting to enable", HPC_SECRET_SETTING);
            throw HpcException.serviceUnavailable(
                "HPC not configured — set "
                    + HPC_SECRET_SETTING
                    + " database setting on "
                    + SYSTEM_SCHEMA
                    + " to enable",
                ctx.header(HpcHeaders.REQUEST_ID));
          }

          // Ensure HPC context matches the latest configured secret.
          HpcContext hpc = hpcContext.get();
          if (hpc == null || !configuredSecret.equals(hpc.sharedSecret())) {
            HpcContext refreshed = initContext(database, configuredSecret);
            HpcContext previous = hpcContext.getAndSet(refreshed);
            hpc = refreshed;
            if (previous == null) {
              logger.info("HPC API: initialized — schema tables created in {}", SYSTEM_SCHEMA);
            } else {
              logger.info("HPC API: shared secret rotated — refreshed cached context");
            }
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
    app.get("/api/hpc/health", ctx -> healthCheck(ctx, database, isHpcConfigured(database)));

    // Worker endpoints (MANAGER — daemon operations)
    app.post(
        "/api/hpc/workers/register",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcHeaders.requireWorkerId(ctx);
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
              String headerWorkerId = HpcHeaders.requireWorkerId(ctx);
              if (!wid.equals(headerWorkerId)) {
                throw HpcException.badRequest(
                    "X-Worker-Id header must match worker id in path",
                    ctx.header(HpcHeaders.REQUEST_ID));
              }
              if (!hpc.workerService().heartbeat(wid)) {
                throw HpcException.notFound(
                    "Worker " + wid + " not found", ctx.header(HpcHeaders.REQUEST_ID));
              }
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
              requireHpcPrivilege(ctx, Privileges.VIEWER);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.jobsApi().listJobs(ctx);
            }));
    app.get(
        "/api/hpc/jobs/{id}",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.VIEWER);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.jobsApi().getJob(ctx);
            }));
    app.delete(
        "/api/hpc/jobs/{id}",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.EDITOR);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.jobsApi().deleteJob(ctx);
            }));
    app.post(
        "/api/hpc/jobs/{id}/claim",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcHeaders.requireWorkerId(ctx);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.jobsApi().claimJob(ctx);
            }));
    app.post(
        "/api/hpc/jobs/{id}/transition",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcHeaders.requireWorkerId(ctx);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.jobsApi().transitionJob(ctx);
            }));
    app.post(
        "/api/hpc/jobs/{id}/complete",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcHeaders.requireWorkerId(ctx);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.jobsApi().completeJob(ctx);
            }));
    app.post(
        "/api/hpc/jobs/{id}/cancel",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.EDITOR);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.jobsApi().cancelJob(ctx);
            }));
    app.get(
        "/api/hpc/jobs/{id}/transitions",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.VIEWER);
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
              requireHpcPrivilege(ctx, Privileges.VIEWER);
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
    app.get(
        "/api/hpc/artifacts/{id}/files",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.VIEWER);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.artifactsApi().listFiles(ctx);
            }));
    app.put(
        "/api/hpc/artifacts/{id}/files/<path>",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.EDITOR);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.artifactsApi().uploadFileByPath(ctx);
            }));
    app.get(
        "/api/hpc/artifacts/{id}/files/<path>",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.VIEWER);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.artifactsApi().downloadFile(ctx);
            }));
    app.head(
        "/api/hpc/artifacts/{id}/files/<path>",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.VIEWER);
              HpcContext hpc = ctx.attribute("hpcContext");
              hpc.artifactsApi().headFile(ctx);
            }));
    app.delete(
        "/api/hpc/artifacts/{id}/files/<path>",
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
}
