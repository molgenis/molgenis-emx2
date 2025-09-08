package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.EMX_2_METRICS_SESSION_TOTAL;
import static org.molgenis.emx2.web.MolgenisWebservice.oidcController;

import io.prometheus.metrics.core.metrics.Gauge;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.util.EventListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jetty.server.session.SessionHandler;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.sql.JWTgenerator;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.tasks.ScriptTableListener;
import org.molgenis.emx2.web.util.ContextHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MolgenisSessionManager {
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSessionManager.class);
  private Map<String, MolgenisSession> sessions = new ConcurrentHashMap<>();

  static final Gauge sessionGauge =
      Gauge.builder()
          .name(EMX_2_METRICS_SESSION_TOTAL)
          .help("Total number of active sessions")
          .register();

  public MolgenisSessionManager() {}

  public MolgenisSession getSession(HttpServletRequest request) {
    String authTokenKey = ContextHelpers.findUsedAuthTokenKey(request);
    if (authTokenKey != null) {
      return getNonPersistedSessionBasedOnToken(request, authTokenKey);
    } else {
      return getPersistentSessionBasedOnSessionId(request);
    }
  }

  private MolgenisSession getPersistentSessionBasedOnSessionId(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      request.getSession();
    }

    MolgenisSession molgenisSession = sessions.get(request.getSession().getId());
    if (molgenisSession.getSessionUser() == null) {
      throw new MolgenisException(
          "Invalid session found with user == null. This should not happen so please report as a bug");
    } else {
      logger.info(
          "get session for user({}) and key ({})",
          molgenisSession.getSessionUser(),
          request.getSession().getId());
    }
    return molgenisSession;
  }

  public MolgenisSession getNonPersistedSessionBasedOnToken(
      HttpServletRequest request, String authTokenKey) {
    SqlDatabase database = new SqlDatabase(false);
    database.addTableListener(new ScriptTableListener(TaskApi.taskSchedulerService));
    String user = JWTgenerator.getUserFromToken(database, request.getHeader(authTokenKey));
    database.setActiveUser(user);
    MolgenisSession session = new MolgenisSession(database, new GraphqlApiFactory());
    database.setListener(new MolgenisSessionManagerDatabaseListener(this, session));
    return session;
  }

  /**
   * this method is used to reset cache of all sessions, necessary when for example metadata changes
   */
  public void clearAllCaches() {
    oidcController.reloadConfig();
    for (MolgenisSession session : sessions.values()) {
      session.clearCache();
    }
  }

  /**
   * Because we cannot access jetty outside spark, we override SparkJava EmbeddedServersFactory to
   * add custom session listener for session create/destroy logic
   */
  public SessionHandler getSessionHandler() {
    SessionHandler sessionHandler = new SessionHandler();
    sessionHandler.setHttpOnly(true);
    sessionHandler.setMaxInactiveInterval(30 * 60); // Set session timeout to 30 minutes

    // Attach the session listener
    sessionHandler.addEventListener(createSessionListener());
    return sessionHandler;
  }

  /**
   * takes care of creating and destroying session attributes when Jetty creates/destroys sessions
   */
  private EventListener createSessionListener() {
    MolgenisSessionManager _this = this;
    return new HttpSessionListener() {
      public void sessionCreated(HttpSessionEvent httpSessionEvent) {

        logger.info("Initializing session");
        // create private database wrapper to session
        SqlDatabase database = new SqlDatabase(false);
        database.setActiveUser("anonymous"); // set default use to "anonymous"
        database.addTableListener(new ScriptTableListener(TaskApi.taskSchedulerService));

        // create session and add to sessions lists so we can also access all active
        // sessions
        MolgenisSession molgenisSession = new MolgenisSession(database, new GraphqlApiFactory());
        sessions.put(httpSessionEvent.getSession().getId(), molgenisSession);
        logger.info("session created: " + httpSessionEvent.getSession().getId());

        // create listener
        database.setListener(new MolgenisSessionManagerDatabaseListener(_this, molgenisSession));

        // Increment metrics gauge
        sessionGauge.inc();
      }

      public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        // remove from session pool
        sessions.remove(httpSessionEvent.getSession().getId());
        // Decrement metrics gauge
        sessionGauge.dec();
        logger.info("session destroyed: " + httpSessionEvent.getSession().getId());
      }
    };
  }
}
