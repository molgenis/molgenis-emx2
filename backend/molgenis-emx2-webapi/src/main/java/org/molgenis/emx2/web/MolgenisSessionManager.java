package org.molgenis.emx2.web;

import static org.joda.time.Minutes.minutesBetween;

import java.util.LinkedHashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.sql.SqlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class MolgenisSessionManager {
  public static final String MOLGENIS_TOKEN = "x-molgenis-token";
  private static final String SESSION_ATTRIBUTE = "session";
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSessionManager.class);
  private static final int SESSION_TIMEOUT = 30;

  // key is the user, might lead to trouble
  private Map<String, MolgenisSession> sessions = new LinkedHashMap<>();

  public MolgenisSessionManager() {}

  public MolgenisSession getSession(Request request) {
    // already in a session, then return that
    if (request.session().attribute(SESSION_ATTRIBUTE) != null) {
      MolgenisSession session = request.session().attribute(SESSION_ATTRIBUTE);

      // timeout
      if (minutesBetween(session.getCreateTime(), DateTime.now())
          .isGreaterThan(Minutes.minutes(SESSION_TIMEOUT))) {
        request.session().removeAttribute(SESSION_ATTRIBUTE); // destroy session
        if (logger.isInfoEnabled()) {
          logger.info(
              "Destroyed session for user({}) because timeout more than {} mins",
              session.getSessionUser(),
              SESSION_TIMEOUT);
        }

      } else {
        logger.info("Reusing session for user({})", session.getSessionUser());

        // refresh timeout to 'now'
        session.setCreateTime(DateTime.now());
        return session;
      }
    }

    // otherwise try tokens (also in case of sessionless requests)
    final String user =
        request.headers(MOLGENIS_TOKEN) == null
            ? "anonymous"
            : request.headers(MOLGENIS_TOKEN).replaceAll("[\n|\r|\t]", "_");

    return sessions.computeIfAbsent(
        user,
        t -> {
          Database database = new SqlDatabase(false);
          if (!database.hasUser(user)) {
            throw new MolgenisException("Authentication failed: User " + user + " not known");
          }
          database.setActiveUser(user);
          database.setListener(new MolgenisSessionManagerDatabaseListener(this, database));
          logger.info("Initializing session for user: {}", database.getActiveUser());
          MolgenisSession session = new MolgenisSession(database);
          logger.info("Initializing session complete for user: {}", database.getActiveUser());
          return session;
        });
  }

  void updateSession(Request request, Response response) {
    MolgenisSession session = getSession(request);

    // check if we need to put in session because user has logged in
    if (session.getDatabase().getActiveUser() != null && request.session() == null) {
      request.session(true);
      request.session().attribute(SESSION_ATTRIBUTE, session);
      logger.info("Saved session for user: {}", session.getDatabase().getActiveUser());
    }

    // check if session state and session user map still in sync
    if (session.getSessionUser() != null
        && !session.getSessionUser().equals(session.getDatabase().getActiveUser())) {
      // remove old sessions
      sessions.remove(session.getSessionUser());
      request.session().removeAttribute(SESSION_ATTRIBUTE); // clear session
      logger.info("Destroyed session because user {} logged out", session.getSessionUser());

      // only create new session is user != null
      if (session.getDatabase().getActiveUser() != null) {
        MolgenisSession newSession = new MolgenisSession(session.getDatabase());
        request.session().attribute(SESSION_ATTRIBUTE, newSession);
        sessions.put(newSession.getSessionUser(), newSession);
        logger.info(
            "Changed session from old user({}]) to new user({}) because login changed",
            session.getSessionUser(),
            newSession.getSessionUser());
      }
    }
  }

  void clearAllCaches() {
    for (MolgenisSession session : sessions.values()) {
      session.clearCache();
    }
  }
}
