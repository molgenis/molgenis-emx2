package org.molgenis.emx2.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.sql.SqlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.embeddedserver.EmbeddedServers;

public class MolgenisSessionManager {
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSessionManager.class);

  // map so we can track the sessions, necessary for 'clearCache' in case of schema changes
  // session id is the key
  private Map<String, MolgenisSession> sessions = new ConcurrentHashMap<>();

  public MolgenisSessionManager() {
    MolgenisSessionManager _this = this;
    // Register custom server to sparkjava so we can listen to the session changes
    EmbeddedServers.add(
        EmbeddedServers.Identifiers.JETTY,
        new SessionListeningJettyFactory(
            new HttpSessionListener() {

              public void sessionCreated(HttpSessionEvent httpSessionEvent) {
                logger.info("Initializing session");
                // create private database wrapper to session
                Database database = new SqlDatabase(false);
                database.setActiveUser("anonymous"); // set default use to "anonymous"
                database.setListener(new MolgenisSessionManagerDatabaseListener(_this, database));

                // create session and add to sessions lists so we can also access all active
                // sessions
                MolgenisSession molgenisSession = new MolgenisSession(database);
                sessions.put(httpSessionEvent.getSession().getId(), molgenisSession);
                logger.info("session created: " + httpSessionEvent.getSession().getId());
              }

              public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
                // remove from session pool
                sessions.remove(httpSessionEvent.getSession().getId());
                logger.info("session destroyed: " + httpSessionEvent.getSession().getId());
              }
            }));
  }

  /** Retrieve MolgenisSession for current request */
  public MolgenisSession getSession(Request request) {

    // if new session create a MolgenisSession object
    if (request.session().isNew()) {
      // jetty session will manage session create/destroy, see handler in constructor
      request.session(true);
    }

    // get the session
    MolgenisSession session = sessions.get(request.session().id());
    logger.info("get session for user({})", session.getSessionUser());
    return session;
  }

  /**
   * this method is used to reset cache of all sessions, necessary when for example metadata changes
   */
  void clearAllCaches() {
    for (MolgenisSession session : sessions.values()) {
      session.clearCache();
    }
  }
}
