package org.molgenis.emx2.web;

import java.util.EventListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.sql.JWTgenerator;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.tasks.ScriptTableListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ExceptionMapper;
import spark.Request;
import spark.embeddedserver.EmbeddedServers;
import spark.embeddedserver.jetty.EmbeddedJettyServer;
import spark.embeddedserver.jetty.JettyHandler;
import spark.embeddedserver.jetty.JettyServerFactory;
import spark.http.matching.MatcherFilter;
import spark.route.Routes;
import spark.staticfiles.StaticFilesConfiguration;

public class MolgenisSessionManager {
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSessionManager.class);
  private Map<String, MolgenisSession> sessions = new ConcurrentHashMap<>();

  public MolgenisSessionManager() {
    createCustomJettyServerFactoryWithCustomSessionListener();
  }

  public MolgenisSession getSession(Request request) {
    String authTokenKey = findUsedAuthTokenKey(request);
    if (authTokenKey != null) {
      return getNonPersistedSessionBasedOnToken(request, authTokenKey);
    } else {
      return getPersistentSessionBasedOnSessionId(request);
    }
  }

  private MolgenisSession getPersistentSessionBasedOnSessionId(Request request) {
    if (request.session().isNew()) {
      request.session(true); // see createCustomJettyServerFactoryWithCustomSessionListener
    }
    MolgenisSession session = sessions.get(request.session().id());
    if (session.getSessionUser() == null) {
      throw new MolgenisException(
          "Invalid session found with user == null. This should not happen so please report as a bug");
    } else {
      logger.info(
          "get session for user({}) and key ({})",
          session.getSessionUser(),
          request.session().id());
    }
    return session;
  }

  private MolgenisSession getNonPersistedSessionBasedOnToken(Request request, String authTokenKey) {
    SqlDatabase database = new SqlDatabase(false);
    database.addTableListener(new ScriptTableListener(TaskApi.taskSchedulerService));
    String user = JWTgenerator.getUserFromToken(database, request.headers(authTokenKey));
    database.setActiveUser(user);
    return new MolgenisSession(database);
  }

  /**
   * From the request, get the name of the auth token key that was used to supply the auth token in
   * the header, or return null if none of the options are present.
   *
   * @param request
   * @return
   */
  public String findUsedAuthTokenKey(Request request) {
    for (String authTokenKey : Constants.MOLGENIS_TOKEN) {
      if (request.headers(authTokenKey) != null) {
        return authTokenKey;
      }
    }
    return null;
  }

  /**
   * this method is used to reset cache of all sessions, necessary when for example metadata changes
   */
  public void clearAllCaches() {
    for (MolgenisSession session : sessions.values()) {
      session.clearCache();
    }
  }

  /**
   * Because we cannot access jetty outside spark, we override SparkJava EmbeddedServersFactory to
   * add custom session listener for session create/destroy logic
   */
  private void createCustomJettyServerFactoryWithCustomSessionListener() {

    EmbeddedServers.add(
        EmbeddedServers.Identifiers.JETTY,
        (Routes routeMatcher,
            StaticFilesConfiguration staticFilesConfiguration,
            ExceptionMapper exceptionMapper,
            boolean hasMultipleHandler) -> {

          // this part has been copied from sparkjava
          MatcherFilter matcherFilter =
              new MatcherFilter(
                  routeMatcher,
                  staticFilesConfiguration,
                  exceptionMapper,
                  false,
                  hasMultipleHandler);
          matcherFilter.init(null);
          JettyHandler handler = new JettyHandler(matcherFilter);
          handler.getSessionCookieConfig().setHttpOnly(true);
          handler.setMaxInactiveInterval(30 * 60); // default session timeout 30mins

          // our custom logic for session create and destroy, reason for us setting up this factory
          handler.addEventListener(createSessionListener());

          // again copied from docs, simplified because we use default thread pool
          return new EmbeddedJettyServer(
              new JettyServerFactory() {
                @Override
                public Server create(int maxThreads, int minThreads, int threadTimeoutMillis) {
                  return new Server();
                }

                @Override
                public Server create(ThreadPool threadPool) {
                  return new Server();
                }
              },
              handler);
        });
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
