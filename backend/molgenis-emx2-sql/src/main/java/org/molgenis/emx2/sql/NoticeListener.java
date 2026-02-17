package org.molgenis.emx2.sql;

import java.sql.SQLException;
import java.sql.SQLWarning;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoticeListener implements ExecuteListener {

  private static final Logger logger = LoggerFactory.getLogger(NoticeListener.class);

  @Override
  public void executeEnd(ExecuteContext ctx) {
    try {
      SQLWarning warning = ctx.connection().getWarnings();
      while (warning != null) {
        logger.warn(warning.getMessage());
        warning = warning.getNextWarning();
      }
      ctx.connection().clearWarnings();
    } catch (SQLException e) {
      // ignore
    }
  }
}
