package org.molgenis.emx2.web;

import static org.joda.time.Minutes.minutesBetween;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
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

  // key is the user, might lead to trouble
  private Map<String, MolgenisSession> sessions = new LinkedHashMap<>();
  private DataSource dataSource;

  public MolgenisSessionManager(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public synchronized MolgenisSession getSession(Request request) {
    // already in a session, then return that
    if (request.session().attribute(SESSION_ATTRIBUTE) != null) {
      MolgenisSession session = request.session().attribute(SESSION_ATTRIBUTE);
      // timeout
      if (minutesBetween(session.getCreateTime(), DateTime.now())
          .isGreaterThan(Minutes.minutes(30))) {
        request.session(false); // destroy session
        if (logger.isInfoEnabled()) {
          logger.info(
              "Destroyed session for user({0}) because timeout more than 30mins",
              session.getSessionUser());
        }

      } else {
        logger.info("Reusing session for user({})", session.getSessionUser());
        // refresh timeout
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
          Database database = new SqlDatabase(dataSource, false);
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
    if (!session.getSessionUser().equals(session.getDatabase().getActiveUser())) {
      // remove old sessions
      sessions.remove(session.getSessionUser());
      request.session(false);
      logger.info("Destroyed session because user {} logged out", session.getSessionUser());

      // only create new session is user != null
      if (session.getDatabase().getActiveUser() != null) {
        MolgenisSession newSession = new MolgenisSession(session.getDatabase());
        request.session(true);
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
