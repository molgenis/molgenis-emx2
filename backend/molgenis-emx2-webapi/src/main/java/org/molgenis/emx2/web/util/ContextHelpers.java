package org.molgenis.emx2.web.util;

import static org.molgenis.emx2.web.MolgenisWebservice.sessionManager;

import io.javalin.http.Context;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.web.MolgenisSession;

public class ContextHelpers {
  private ContextHelpers() {}

  public static Database getDatabaseForContext(Context ctx) {
    if (ctx.req().getSession(false) != null) {
      MolgenisSession session = sessionManager.getSession(ctx.req());
      return session.getDatabase();
    } else {
      Database database = new SqlDatabase(false);
      database.setActiveUser("anonymous");
      return database;
    }
  }
}
