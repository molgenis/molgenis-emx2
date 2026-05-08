package org.molgenis.emx2.sql;

import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.MolgenisException;
import org.postgresql.util.PSQLException;

public class SqlMolgenisException extends MolgenisException {

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

  private static final String COLUMN_DENIED_PREFIX = "permission denied for column ";

  private static String getTitle(DataAccessException dae) {
    if (dae.getCause() instanceof PSQLException) {
      PSQLException cause = (PSQLException) dae.getCause();
      if (cause.getServerErrorMessage() != null) {
        String message = cause.getServerErrorMessage().getMessage();
        if (cause.getSQLState().equals("57014")) {
          message = "canceling statement due to timeout or by user request";
        } else if (cause.getSQLState().equals("42501")
            && message != null
            && message.startsWith(COLUMN_DENIED_PREFIX)) {
          String columnName = message.substring(COLUMN_DENIED_PREFIX.length());
          if (Constants.MG_OWNER_COLUMN.equals(columnName)) {
            return "cannot change row owner";
          } else if (Constants.MG_GROUPS_COLUMN.equals(columnName)) {
            return "cannot change row groups";
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
