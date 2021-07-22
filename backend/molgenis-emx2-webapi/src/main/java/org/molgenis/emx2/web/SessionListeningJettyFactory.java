package org.molgenis.emx2.web;

import javax.servlet.http.HttpSessionListener;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import spark.ExceptionMapper;
import spark.embeddedserver.EmbeddedServer;
import spark.embeddedserver.EmbeddedServerFactory;
import spark.embeddedserver.jetty.EmbeddedJettyServer;
import spark.embeddedserver.jetty.JettyHandler;
import spark.embeddedserver.jetty.JettyServerFactory;
import spark.http.matching.MatcherFilter;
import spark.route.Routes;
import spark.staticfiles.StaticFilesConfiguration;

// copied from EmbeddedJettyFactory
public class SessionListeningJettyFactory implements EmbeddedServerFactory {
  private static int SESSION_TIMEOUT_SECONDS = 60 * 30; // 30 minutes timeout
  private JettyServerFactory serverFactory;
  private boolean httpOnly = true;
  private HttpSessionListener sessionListener = null;

  public SessionListeningJettyFactory(HttpSessionListener sessionListener) {
    this.sessionListener = sessionListener;
    this.serverFactory =
        // copied from JettyServer, which was package private unfortunately
        new JettyServerFactory() {
          @Override
          public Server create(int maxThreads, int minThreads, int threadTimeoutMillis) {
            Server server;
            if (maxThreads > 0) {
              int max = maxThreads;
              int min = (minThreads > 0) ? minThreads : 8;
              int idleTimeout = (threadTimeoutMillis > 0) ? threadTimeoutMillis : 60000;
              server = new Server(new QueuedThreadPool(max, min, idleTimeout));
            } else {
              server = new Server();
            }
            return server;
          }

          @Override
          public Server create(ThreadPool threadPool) {
            // not needed in our case
            throw new UnsupportedOperationException("not implemented");
          }
        };
  }

  public EmbeddedServer create(
      Routes routeMatcher,
      StaticFilesConfiguration staticFilesConfiguration,
      ExceptionMapper exceptionMapper,
      boolean hasMultipleHandler) {
    MatcherFilter matcherFilter =
        new MatcherFilter(
            routeMatcher, staticFilesConfiguration, exceptionMapper, false, hasMultipleHandler);
    matcherFilter.init(null);
    JettyHandler handler = new JettyHandler(matcherFilter);
    handler.getSessionCookieConfig().setHttpOnly(httpOnly);
    handler.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS); // session timeout
    if (sessionListener != null) {
      handler.addEventListener(
          sessionListener); // add the event listener so we can help manage sessions
    }
    return new EmbeddedJettyServer(serverFactory, handler);
  }
}
