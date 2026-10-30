package org.molgenis.emx2.web;

import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;
import static org.molgenis.emx2.web.Constants.EMX_2_METRICS_SESSION_TOTAL;
import static org.pac4j.core.util.Pac4jConstants.USERNAME;

import io.prometheus.metrics.core.metrics.Gauge;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.molgenis.emx2.Username;
import org.molgenis.emx2.graphql.GraphqlSessionHandlerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MolgenisSessionHandler implements GraphqlSessionHandlerInterface {
  static final Logger logger = LoggerFactory.getLogger(MolgenisSessionHandler.class);
  private static final Map<Username, Set<String>> userSessions = new ConcurrentHashMap<>();
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
  public synchronized void createSession(Username username) {
    HttpSession session = request.getSession(true);
    String previousUsername = (String) session.getAttribute(USERNAME);
    if (previousUsername != null) {
      unregisterSession(new Username(previousUsername), session.getId());
    }

    String sessionId = request.changeSessionId();
    session.setMaxInactiveInterval(30 * 60); // 30 minutes
    session.setAttribute(USERNAME, username.value());
    registerSession(username, sessionId);

    logger.info(
        "Session {} linked to user {}. This user now has {} sessions with {} total",
        sessionId,
        username,
        userSessions.get(username).size(),
        sessionGauge.get());
  }

  @Override
  public synchronized void destroySession() {
    HttpSession session = request.getSession(false);
    if (session != null) {
      String usernameAttribute = (String) session.getAttribute(USERNAME);
      String sessionId = session.getId();
      session.invalidate();
      if (usernameAttribute != null) {
        Username username = new Username(usernameAttribute);
        unregisterSession(username, sessionId);
        logger.info(
            "session {} invalidated. User {} now has {} sessions with {} total",
            sessionId,
            username,
            userSessions.getOrDefault(username, Set.of()).size(),
            sessionGauge.get());
      }
    }
  }

  private static void registerSession(Username username, String sessionId) {
    Set<String> sessions =
        userSessions.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet());
    if (sessions.add(sessionId)) {
      sessionGauge.inc(1);
    }
  }

  private static void unregisterSession(Username username, String sessionId) {
    Set<String> sessions = userSessions.get(username);
    if (sessions != null && sessions.remove(sessionId)) {
      sessionGauge.dec(1);
    }
  }

  @Override
  public Username getCurrentUser() {
    HttpSession session = request.getSession(false);
    if (session == null) return ANONYMOUS;
    String attribute = (String) session.getAttribute(USERNAME);
    if (attribute == null) return ANONYMOUS;
    return new Username(attribute);
  }
}
