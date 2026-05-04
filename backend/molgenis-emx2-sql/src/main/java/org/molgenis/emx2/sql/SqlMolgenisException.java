package org.molgenis.emx2.sql;

import java.util.Map;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.MolgenisException;
import org.postgresql.util.PSQLException;

public class SqlMolgenisException extends MolgenisException {

  private static final String SQLSTATE_INSUFFICIENT_PRIVILEGE = "42501";
  private static final String COLUMN_DENIED_PREFIX = "permission denied for column ";
  private static final Map<String, String> COLUMN_FRIENDLY_MESSAGES =
      Map.of(
          "mg_owner", "cannot change row owner",
          "mg_groups", "cannot change row groups");

  public SqlMolgenisException(DataAccessException dae) {
    super(getTitle(dae) + ". " + getDetail(dae));
  }

  public SqlMolgenisException(String title, Exception e) {
    super(
        e instanceof DataAccessException
            ?
            // if dae we can add details
            title
                + ": "
                + getTitle((DataAccessException) e)
                + ". "
                + getDetail((DataAccessException) e)
            // otherwise simply return in usual 'title: message' format
            : title + ": " + e.getMessage());
  }

  private static String getTitle(DataAccessException dae) {
    if (dae.getCause() instanceof PSQLException) {
      PSQLException cause = (PSQLException) dae.getCause();
      if (cause.getServerErrorMessage() != null) {
        String message = cause.getServerErrorMessage().getMessage();
        if (cause.getSQLState().equals("57014")) {
          // i.e. message.equals("canceling statement due to user request")
          // might because of our timeout setting in SqlDatabase, therefore rewrite:
          message = "canceling statement due to timeout or by user request";
        } else if (SQLSTATE_INSUFFICIENT_PRIVILEGE.equals(cause.getSQLState())
            && message != null
            && message.startsWith(COLUMN_DENIED_PREFIX)) {
          String columnName = message.substring(COLUMN_DENIED_PREFIX.length());
          String friendly = COLUMN_FRIENDLY_MESSAGES.get(columnName);
          if (friendly != null) {
            message = friendly;
          }
        }
        return message;
      } else {
        return cause.getMessage();
      }
    } else {
      return dae.getMessage();
    }
  }

  private static String getDetail(DataAccessException dae) {
    if (dae.getCause() instanceof PSQLException) {
      PSQLException pe = (PSQLException) dae.getCause();
      if (pe.getServerErrorMessage() != null && pe.getServerErrorMessage().getDetail() != null) {
        return "Details: " + pe.getServerErrorMessage().getDetail();
      }
    }
    return "";
  }
}
