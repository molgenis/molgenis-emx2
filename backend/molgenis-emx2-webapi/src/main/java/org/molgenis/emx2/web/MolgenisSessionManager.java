package org.molgenis.emx2.web;

import static org.joda.time.Minutes.minutesBetween;

import java.util.LinkedHashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.sql.SqlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

public class MolgenisSessionManager {
  public static final String MOLGENIS_TOKEN = "x-molgenis-token";
  private static final String SESSION_ATTRIBUTE = "molgenis_session";
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSessionManager.class);
  private static final int SESSION_TIMEOUT = 30;

  // map so we can track the sessions
  // session id is the key, todo in case of session less requests outside browser
  private Map<String, MolgenisSession> sessions = new LinkedHashMap<>();

  public MolgenisSessionManager() {}

  public synchronized MolgenisSession getSession(Request request) {

    // if valid token return session from token
    if (request.headers(MOLGENIS_TOKEN) != null) {
      String token = request.headers(MOLGENIS_TOKEN);
      if (sessions.containsKey(token)) {
        return sessions.get(token);
      }
    }

    // if new session create a MolgenisSession object
    if (request.session().isNew()) {
      // add session into session pool
      MolgenisSession session = createSession(request.session().id());
      // put in request session so we can easily access for this session
      request.session().attribute(SESSION_ATTRIBUTE, session);
    }

    // get the session
    MolgenisSession session = request.session().attribute(SESSION_ATTRIBUTE);

    // check session is invalid
    if (minutesBetween(session.getCreateTime(), DateTime.now())
        .isGreaterThan(Minutes.minutes(SESSION_TIMEOUT))) {

      logger.info(
          "Invalidating session for user({}) because timeout more than {} mins",
          session.getSessionUser(),
          SESSION_TIMEOUT);

      // invalidate the session by signing out
      session.getDatabase().setActiveUser("anonymous");
    }
    // refresh timeout to 'now'
    session.setCreateTime(DateTime.now());

    logger.info("Reusing session for user({})", session.getSessionUser());
    return session;
  }

  public MolgenisSession createSession(String token) {
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
