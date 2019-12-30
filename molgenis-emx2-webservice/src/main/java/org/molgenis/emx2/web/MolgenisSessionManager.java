package org.molgenis.emx2.web;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.DatabaseListener;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.sql.SqlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.joda.time.Minutes.minutesBetween;

public class MolgenisSessionManager implements DatabaseListener {
  public static final String MOLGENIS_TOKEN = "x-molgenis-token";
  private static final String SESSION_ATTRIBUTE = "session";
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSessionManager.class);

  private Map<String, MolgenisSession> sessions = new LinkedHashMap<>();
  private DataSource dataSource;

  public MolgenisSessionManager(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  synchronized MolgenisSession getSession(Request request) {
    // todo authentication

    // already in a session, then return that
    if (request.session().attribute(SESSION_ATTRIBUTE) != null) {
      MolgenisSession session = request.session().attribute(SESSION_ATTRIBUTE);
      // timeout
      if (minutesBetween(session.getCreateTime(), DateTime.now())
          .isGreaterThan(Minutes.minutes(30))) {
        request.session(false); // destroy session
        logger.info(
            "Destroyed session for user("
                + session.getSessionUser()
                + ") because timeout more than 30mins");

      } else {
        logger.info("Reusing session for user(" + session.getSessionUser() + ")");
        return session;
      }
    }

    // todo delete stale sessions

    // otherwise try tokens (also in case of sessionless requests)
    final String user =
        request.headers(MOLGENIS_TOKEN) == null
            ? "anonymous"
            : request.headers(MOLGENIS_TOKEN).replaceAll("[\n|\r|\t]", "_");
    ;

    // todo remove cached after a while!!!!
    return sessions.computeIfAbsent(
        user,
        t -> {
          Database database = new SqlDatabase(dataSource);
          if (!database.hasUser(user)) {
            throw new MolgenisException("Authentication failed", "User " + user + " not known");
          }
          database.setListener(this);
          database.setActiveUser(user);
          logger.info("Initializing session for user: " + database.getActiveUser());
          return new MolgenisSession(database);
        });
  }

  void updateSession(Request request, Response response) {
    MolgenisSession session = getSession(request);

    // check if we need to put in session because user has logged in
    if (session.getDatabase().getActiveUser() != null && request.session() == null) {
      request.session(true);
      request.session().attribute(SESSION_ATTRIBUTE, session);
      logger.info("Saved session for user: " + session.getDatabase().getActiveUser());
    }

    // check if session state and session user map still in sync
    if (!session.getSessionUser().equals(session.getDatabase().getActiveUser())) {
      // remove old sessions
      sessions.remove(session.getSessionUser());
      request.session(false);
      logger.info("Destroyed session because user " + session.getSessionUser() + " logged out");

      // only create new session is user != null
      if (session.getDatabase().getActiveUser() != null) {
        MolgenisSession newSession = new MolgenisSession(session.getDatabase());
        request.session(true);
        request.session().attribute(SESSION_ATTRIBUTE, newSession);
        sessions.put(newSession.getSessionUser(), newSession);
        logger.info(
            "Changed session from old user("
                + session.getSessionUser()
                + ") to new user("
                + newSession.getSessionUser()
                + ") because login changed");
      }
    }
  }

  @Override
  public void schemaRemoved(String schemaName) {
    for (MolgenisSession session : sessions.values()) {
      session.clearSchemaCache(schemaName);
    }
  }

  @Override
  public void userChanged() {
    for (MolgenisSession session : sessions.values()) {
      session.clearCache();
    }
  }

  @Override
  public void schemaChanged(String schemaName) {
    schemaRemoved(schemaName);
  }
}
