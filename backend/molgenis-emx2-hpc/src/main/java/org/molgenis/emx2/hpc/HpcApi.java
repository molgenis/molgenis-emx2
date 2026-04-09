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
 * tables and services are initialized lazily on first request after {@code
 * MOLGENIS_HPC_ENABLED=true}.
 *
 * <p>When HPC is disabled, all endpoints except {@code /api/hpc/health} return 503 Service
 * Unavailable. The health endpoint always responds and indicates whether HPC is enabled.
 *
 * <p>Authentication and middleware infrastructure is provided by {@link HpcAuth}.
 */
public class HpcApi {

  private static final Logger logger = LoggerFactory.getLogger(HpcApi.class);
  private static final String ARTIFACT_FILES_PATH = "/api/hpc/artifacts/{id}/files/<path>";

  private HpcApi() {}

  /** Call from MolgenisWebservice.start() to register all HPC routes. */
  public static void create(Javalin app) {
    SqlDatabase database = new SqlDatabase(false);

    // Lazy-init holder: initialized on first request while enabled; cleared when disabled
    AtomicReference<HpcContext> hpcContext = new AtomicReference<>(null);

    registerExceptionHandler(app);
    registerAfterHandler(app);
    registerBeforeHandler(app, database, hpcContext);

    // Health endpoint (exempt from auth, always available)
    app.get(
        "/api/hpc/health",
        ctx -> healthCheck(ctx, database, isHpcEnabled(database), hpcContext.get() != null));

    registerWorkerRoutes(app);
    registerJobRoutes(app);
    registerArtifactRoutes(app);

    logger.info("HPC API: routes registered (lazy init while {}=true)", HPC_ENABLED_SETTING);
  }

  private static void registerExceptionHandler(Javalin app) {
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
  }

  private static void registerAfterHandler(Javalin app) {
    app.after(
        "/api/hpc/*",
        ctx -> {
          String traceId = ctx.header(HpcHeaders.TRACE_ID);
          if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
          }
          ctx.header(HpcHeaders.TRACE_ID, traceId);
        });
  }

  private static void registerBeforeHandler(
      Javalin app, SqlDatabase database, AtomicReference<HpcContext> hpcContext) {
    app.before(
        "/api/hpc/*",
        ctx -> {
          // Health endpoint is always available, no init or auth needed
          if (ctx.path().equals("/api/hpc/health")) {
            return;
          }

          boolean enabled = isHpcEnabled(database);
          if (!enabled) {
            if (hpcContext.getAndSet(null) != null) {
              logger.info("HPC API: disabled — cleared cached context");
            }
            invalidateSettingsCache();
            logger.info("HPC API: disabled — set {}=true to enable", HPC_ENABLED_SETTING);
            throw HpcException.serviceUnavailable(
                "HPC is disabled — set "
                    + HPC_ENABLED_SETTING
                    + "=true on "
                    + SYSTEM_SCHEMA
                    + " to enable the HPC bridge",
                ctx.header(HpcHeaders.REQUEST_ID));
          }

          // Ensure HPC context exists while enabled.
          HpcContext hpc = hpcContext.get();
          if (hpc == null) {
            HpcContext initialized = initContext(database);
            hpcContext.set(initialized);
            hpc = initialized;
            invalidateSettingsCache();
            logger.info("HPC API: initialized — schema tables created in {}", SYSTEM_SCHEMA);
          }
          ctx.attribute(HPC_CONTEXT_ATTR, hpc);

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
            // Worker HMAC authentication using per-worker credentials.
            verifyHmac(ctx, hpc.workerCredentialService());
            ctx.attribute(HPC_AUTH_METHOD_ATTR, "HMAC");
          } else if (tokenHeader != null && !tokenHeader.isBlank()) {
            // JWT token authentication
            String user = verifyToken(ctx, database, tokenHeader);
            ctx.attribute(HPC_AUTH_METHOD_ATTR, "USER");
            ctx.attribute(HPC_AUTH_USER_ATTR, user);
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
            ctx.attribute(HPC_AUTH_METHOD_ATTR, "USER");
            ctx.attribute(HPC_AUTH_USER_ATTR, sessionUser);
            logger.debug("HPC session auth: user '{}'", sessionUser);
          }

          // Resolve effective privilege for user-authenticated requests
          if ("USER".equals(ctx.attribute(HPC_AUTH_METHOD_ATTR))) {
            String username = ctx.attribute(HPC_AUTH_USER_ATTR);
            Privileges privilege = resolveEffectivePrivilege(database, username);
            ctx.attribute(HPC_PRIVILEGE_ATTR, privilege);
          }
        });
  }

  private static void registerWorkerRoutes(Javalin app) {
    app.post(
        "/api/hpc/workers/register",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcHeaders.requireWorkerId(ctx);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.workersApi().register(ctx);
            }));
    app.delete(
        "/api/hpc/workers/{id}",
        hpcHandler(
            ctx -> {
              // Worker deletion is an explicit user operation only.
              // Daemon/HMAC principals must never remove worker identities.
              requireUserHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.workersApi().deleteWorker(ctx);
            }));
    app.post(
        "/api/hpc/workers/{id}/heartbeat",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
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
              ctx.json(Map.of(HpcFields.WORKER_ID, wid, HpcFields.STATUS, "ok"));
            }));
    app.post(
        "/api/hpc/workers/{id}/credentials/issue",
        hpcHandler(
            ctx -> {
              requireUserHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.workerCredentialsApi().issueCredential(ctx);
            }));
    app.post(
        "/api/hpc/workers/{id}/credentials/rotate",
        hpcHandler(
            ctx -> {
              requireUserHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.workerCredentialsApi().rotateCredential(ctx);
            }));
    app.post(
        "/api/hpc/workers/{id}/credentials/{credentialId}/revoke",
        hpcHandler(
            ctx -> {
              requireUserHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.workerCredentialsApi().revokeCredential(ctx);
            }));
    app.get(
        "/api/hpc/workers/{id}/credentials",
        hpcHandler(
            ctx -> {
              requireUserHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.workerCredentialsApi().listCredentials(ctx);
            }));
  }

  private static void registerJobRoutes(Javalin app) {
    app.post(
        "/api/hpc/jobs",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.EDITOR);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.jobsApi().createJob(ctx);
            }));
    app.get(
        "/api/hpc/jobs",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.VIEWER);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.jobsApi().listJobs(ctx);
            }));
    app.get(
        "/api/hpc/jobs/{id}",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.VIEWER);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.jobsApi().getJob(ctx);
            }));
    app.delete(
        "/api/hpc/jobs/{id}",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.EDITOR);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.jobsApi().deleteJob(ctx);
            }));
    app.post(
        "/api/hpc/jobs/{id}/claim",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcHeaders.requireWorkerId(ctx);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.jobsApi().claimJob(ctx);
            }));
    app.post(
        "/api/hpc/jobs/{id}/transition",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcHeaders.requireWorkerId(ctx);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.jobsApi().transitionJob(ctx);
            }));
    app.post(
        "/api/hpc/jobs/{id}/complete",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcHeaders.requireWorkerId(ctx);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.jobsApi().completeJob(ctx);
            }));
    app.post(
        "/api/hpc/jobs/{id}/cancel",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.EDITOR);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.jobsApi().cancelJob(ctx);
            }));
    app.get(
        "/api/hpc/jobs/{id}/transitions",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.VIEWER);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.jobsApi().getTransitions(ctx);
            }));
  }

  private static void registerArtifactRoutes(Javalin app) {
    app.post(
        "/api/hpc/artifacts",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.EDITOR);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.artifactsApi().createArtifact(ctx);
            }));
    app.get(
        "/api/hpc/artifacts/{id}",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.VIEWER);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.artifactsApi().getArtifact(ctx);
            }));
    app.delete(
        "/api/hpc/artifacts/{id}",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.artifactsApi().deleteArtifact(ctx);
            }));
    app.get(
        "/api/hpc/artifacts/{id}/files",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.VIEWER);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.artifactsApi().listFiles(ctx);
            }));
    app.put(
        ARTIFACT_FILES_PATH,
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.EDITOR);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.artifactsApi().uploadFileByPath(ctx);
            }));
    app.get(
        ARTIFACT_FILES_PATH,
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.VIEWER);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.artifactsApi().downloadFile(ctx);
            }));
    app.head(
        ARTIFACT_FILES_PATH,
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.VIEWER);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.artifactsApi().headFile(ctx);
            }));
    app.delete(
        ARTIFACT_FILES_PATH,
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.MANAGER);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.artifactsApi().deleteFile(ctx);
            }));
    app.post(
        "/api/hpc/artifacts/{id}/commit",
        hpcHandler(
            ctx -> {
              requireHpcPrivilege(ctx, Privileges.EDITOR);
              HpcContext hpc = ctx.attribute(HPC_CONTEXT_ATTR);
              hpc.artifactsApi().commitArtifact(ctx);
            }));
  }
}
