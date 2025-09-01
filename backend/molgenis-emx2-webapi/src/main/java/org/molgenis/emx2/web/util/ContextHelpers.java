package org.molgenis.emx2.web.util;

import static org.molgenis.emx2.web.MolgenisWebservice.sessionManager;

import io.javalin.http.Context;
import jakarta.servlet.http.HttpServletRequest;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.web.Constants;
import org.molgenis.emx2.web.MolgenisSession;

public class ContextHelpers {
  private ContextHelpers() {}

  public static Database getDatabaseForContext(Context ctx) {

    HttpServletRequest request = ctx.req();
    String authTokenKey = findUsedAuthTokenKey(request);
    if (authTokenKey != null) {
      MolgenisSession session =
          sessionManager.getNonPersistedSessionBasedOnToken(request, authTokenKey);
      return session.getDatabase();
    }
    if (request.getSession(false) != null) {
      MolgenisSession session = sessionManager.getSession(request);
      return session.getDatabase();
    } else {
      Database database = new SqlDatabase(false);
      database.setActiveUser("anonymous");
      return database;
    }
  }

  /**
   * From the request, get the name of the auth token key that was used to supply the auth token in
   * the header, or return null if none of the options are present.
   *
   * @param request
   * @return
   */
  public static String findUsedAuthTokenKey(HttpServletRequest request) {
    for (String authTokenKey : Constants.MOLGENIS_TOKEN) {
      if (request.getHeader(authTokenKey) != null) {
        return authTokenKey;
      }
    }
    return null;
  }
}
