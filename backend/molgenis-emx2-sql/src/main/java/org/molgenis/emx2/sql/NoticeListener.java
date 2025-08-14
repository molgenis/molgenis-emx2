package org.molgenis.emx2.sql;

import java.sql.SQLException;
import java.sql.SQLWarning;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;

public class NoticeListener implements ExecuteListener {

  public void executeEnd(ExecuteContext ctx) {
    try {
      SQLWarning warning = ctx.connection().getWarnings();
      while (warning != null) {
        System.out.println("[PG NOTICE] " + warning.getMessage());
        warning = warning.getNextWarning();
      }
      ctx.connection().clearWarnings();
    } catch (SQLException e) {
      // ignore
    }
  }
}
