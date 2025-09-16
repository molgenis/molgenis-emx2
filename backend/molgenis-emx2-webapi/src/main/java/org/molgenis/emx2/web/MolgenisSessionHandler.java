package org.molgenis.emx2.web;

import static org.molgenis.emx2.Constants.ANONYMOUS;
import static org.molgenis.emx2.web.Constants.EMX_2_METRICS_SESSION_TOTAL;
import static org.pac4j.core.util.Pac4jConstants.USERNAME;

import io.prometheus.metrics.core.metrics.Gauge;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.molgenis.emx2.graphql.GraphqlSessionHandlerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MolgenisSessionHandler implements GraphqlSessionHandlerInterface {
  static final Logger logger = LoggerFactory.getLogger(MolgenisSessionHandler.class);
  private static final Map<String, Set<HttpSession>> userSessions = new ConcurrentHashMap<>();
  static final Gauge sessionGauge =
      Gauge.builder()
          .name(EMX_2_METRICS_SESSION_TOTAL)
          .help("Total number of active sessions")
          .register();

  private final HttpServletRequest request;

  public MolgenisSessionHandler(HttpServletRequest request) {
    this.request = request;
  }

  @Override
  public void createSession(String username) {
    HttpSession session = request.getSession(); // get existing session or create
    if (session.getAttribute(USERNAME).equals(username)) {
      return;
    }
    session.setMaxInactiveInterval(30 * 60); // 30 minutes
    session.setAttribute(USERNAME, username);

    // register this session
    userSessions.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(session);
    sessionGauge.inc(1);

    logger.info(
        "Session {} created. User {} now has {} sessions with {} total",
        session.getId(),
        username,
        userSessions.get(username).size(),
        sessionGauge.get());
  }

  @Override
  public synchronized void destroySession() {
    HttpSession session = request.getSession(false);
    if (session != null) {
      String username = (String) session.getAttribute("username");
      session.invalidate();
      sessionGauge.dec(1);

      // remove from registry
      Set<HttpSession> sessions = userSessions.get(username);
      int sessionCountForUser = 0;
      if (sessions != null) {
        sessions.remove(session);
        sessionCountForUser = sessions.size();
      }
      logger.info(
          "session {} destroyed. User {} now has {} sessions with {} total",
          session.getId(),
          username,
          sessionCountForUser,
          sessionGauge.get());
    }
  }

  @Override
  public String getCurrentUser() {
    HttpSession session = request.getSession(false);
    if (session == null) return ANONYMOUS;
    String username = (String) session.getAttribute(USERNAME);
    if (username == null || username.isEmpty()) return ANONYMOUS;
    return username;
  }
}
