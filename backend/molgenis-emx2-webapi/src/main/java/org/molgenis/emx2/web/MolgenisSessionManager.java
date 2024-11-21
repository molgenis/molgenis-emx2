package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.oidcController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.util.EventListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jetty.server.session.SessionHandler;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.sql.JWTgenerator;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.tasks.ScriptTableListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MolgenisSessionManager {
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSessionManager.class);
  private Map<String, MolgenisSession> sessions = new ConcurrentHashMap<>();

  public MolgenisSessionManager() {}

  public MolgenisSession getSession(HttpServletRequest request) {
    String authTokenKey = findUsedAuthTokenKey(request);
    if (authTokenKey != null) {
      return getNonPersistedSessionBasedOnToken(request, authTokenKey);
    } else {
      return getPersistentSessionBasedOnSessionId(request);
    }
  }

  private MolgenisSession getPersistentSessionBasedOnSessionId(HttpServletRequest request) {
    if (request.getSession().isNew()) {
      request.getSession(true); // see createCustomJettyServerFactoryWithCustomSessionListener
    }
    MolgenisSession session = sessions.get(request.getSession().getId());
    if (session.getSessionUser() == null) {
      throw new MolgenisException(
          "Invalid session found with user == null. This should not happen so please report as a bug");
    } else {
      logger.info(
          "get session for user({}) and key ({})",
          session.getSessionUser(),
          request.getSession().getId());
    }
    return session;
  }

  private MolgenisSession getNonPersistedSessionBasedOnToken(
      HttpServletRequest request, String authTokenKey) {
    SqlDatabase database = new SqlDatabase(false);
    database.addTableListener(new ScriptTableListener(TaskApi.taskSchedulerService));
    String user = JWTgenerator.getUserFromToken(database, request.getHeader(authTokenKey));
    database.setActiveUser(user);
    MolgenisSession session = new MolgenisSession(database);
    database.setListener(new MolgenisSessionManagerDatabaseListener(this, session));
    return session;
  }

  /**
   * From the request, get the name of the auth token key that was used to supply the auth token in
   * the header, or return null if none of the options are present.
   *
   * @param request
   * @return
   */
  public String findUsedAuthTokenKey(HttpServletRequest request) {
    for (String authTokenKey : Constants.MOLGENIS_TOKEN) {
      if (request.getHeader(authTokenKey) != null) {
        return authTokenKey;
      }
    }
    return null;
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
        MolgenisSession molgenisSession = new MolgenisSession(database);
        sessions.put(httpSessionEvent.getSession().getId(), molgenisSession);
        logger.info("session created: " + httpSessionEvent.getSession().getId());

        // create listener
        database.setListener(new MolgenisSessionManagerDatabaseListener(_this, molgenisSession));
      }

      public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        // remove from session pool
        sessions.remove(httpSessionEvent.getSession().getId());
        logger.info("session destroyed: " + httpSessionEvent.getSession().getId());
      }
    };
  }
}
