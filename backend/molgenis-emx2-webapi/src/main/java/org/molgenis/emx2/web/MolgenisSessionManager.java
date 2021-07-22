package org.molgenis.emx2.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.eclipse.jetty.server.Server;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.sql.SqlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.embeddedserver.EmbeddedServers;

public class MolgenisSessionManager {
  public static final String MOLGENIS_TOKEN = "x-molgenis-token";
  private static final String MOLGENIS_SESSION_ATTRIBUTE = "molgenis_session";
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSessionManager.class);
  private static Server server;

  // map so we can track the sessions
  // session id is the key
  private Map<String, MolgenisSession> sessions = new ConcurrentHashMap<>();

  public MolgenisSessionManager() {
    // Register custom server that listens to the sessions
    EmbeddedServers.add(
        EmbeddedServers.Identifiers.JETTY,
        new SessionListeningJettyFactory(
            new HttpSessionListener() {
              @Override
              public void sessionCreated(HttpSessionEvent httpSessionEvent) {
                // add session into session pool
                MolgenisSession session = createSession(httpSessionEvent.getSession().getId());
                // put in request session so we can easily access for this session
                httpSessionEvent.getSession().setAttribute(MOLGENIS_SESSION_ATTRIBUTE, session);
                logger.info("session created: " + httpSessionEvent.getSession().getId());
              }

              @Override
              public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
                // remove from session pool
                sessions.remove(httpSessionEvent.getSession().getId());
                logger.info("session destroyed: " + httpSessionEvent.getSession().getId());
              }
            }));
  }

  public MolgenisSession getSession(Request request) {

    // if valid token return session from token
    // todo: this is only for testing, we will implement JWT tokens properly later
    if (request.headers(MOLGENIS_TOKEN) != null) {
      String token = request.headers(MOLGENIS_TOKEN);
      if (sessions.containsKey(token)) {
        return sessions.get(token);
      }
    }

    // if new session create a MolgenisSession object
    if (request.session().isNew()) {
      request.session(true); // will create session stuff, see handler above
    }

    // get the session
    MolgenisSession session = request.session().attribute(MOLGENIS_SESSION_ATTRIBUTE);

    logger.info("Reusing session for user({})", session.getSessionUser());
    return session;
  }

  MolgenisSession createSession(String token) {
    // default user
    String user = "anonymous";
    // create new session
    Database database = new SqlDatabase(false);
    database.setActiveUser(user);
    database.setListener(new MolgenisSessionManagerDatabaseListener(this, database));
    logger.info("Initializing session for user: {}", database.getActiveUser());
    MolgenisSession session = new MolgenisSession(database, token);
    logger.info("Initializing session complete for user: {}", database.getActiveUser());

    // put in session lists so we can easily access all session
    sessions.put(session.getToken(), session);
    return session;
  }

  void clearAllCaches() {
    for (MolgenisSession session : sessions.values()) {
      session.clearCache();
    }
  }
}
