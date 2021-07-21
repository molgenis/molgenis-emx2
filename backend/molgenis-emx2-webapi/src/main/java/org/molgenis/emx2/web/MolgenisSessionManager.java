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

public class MolgenisSessionManager {
  public static final String MOLGENIS_TOKEN = "x-molgenis-token";
  private static final String SESSION_ATTRIBUTE = "molgenis_session";
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSessionManager.class);
  private static final int SESSION_TIMEOUT = 1;

  // map so we can track the sessions
  // session id is the key, todo in case of session less requests outside browser
  private Map<String, MolgenisSession> sessions = new LinkedHashMap<>();

  public MolgenisSessionManager() {}

  public MolgenisSession getSession(Request request) {

    // check if already in session
    if (request.session().attribute(SESSION_ATTRIBUTE) != null) {
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

        // else return session
      }
      // refresh timeout to 'now'
      session.setCreateTime(DateTime.now());

      logger.info("Reusing session for user({})", session.getSessionUser());
      return session;
    }

    // else create session
    else {
      // todo: implement tokens. Now removed.
      //    final String user =
      //        request.headers(MOLGENIS_TOKEN) == null
      //            ? "anonymous"
      //            : request.headers(MOLGENIS_TOKEN).replaceAll("[\n|\r|\t]", "_");
      String user = "anonymous"; // default user

      // create new session
      Database database = new SqlDatabase(false);
      if (!database.hasUser(user)) {
        throw new MolgenisException("Authentication failed: User " + user + " not known");
      }
      database.setActiveUser(user);
      database.setListener(new MolgenisSessionManagerDatabaseListener(this, database));
      logger.info("Initializing session for user: {}", database.getActiveUser());
      MolgenisSession session = new MolgenisSession(database);
      logger.info("Initializing session complete for user: {}", database.getActiveUser());

      // put in session lists so we can easily access all session
      sessions.put(request.session().id(), session);

      // put in request session so we can easily access for this session
      request.session().attribute(SESSION_ATTRIBUTE, session);

      // return
      return session;
    }
  }

  void clearAllCaches() {
    for (MolgenisSession session : sessions.values()) {
      session.clearCache();
    }
  }
}
