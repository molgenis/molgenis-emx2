package org.molgenis.emx2.sql;

import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.MolgenisException;
import org.postgresql.util.PSQLException;

public class SqlMolgenisException extends MolgenisException {

  public SqlMolgenisException(DataAccessException dae) {
    super(getTitle(dae) + ": " + getDetail(dae), dae);
  }

  public SqlMolgenisException(String title, Exception e) {
    super(
        e instanceof DataAccessException
            ? title
                + ": "
                + getTitle((DataAccessException) e)
                + ". Details: "
                + getDetail((DataAccessException) e)
            : title,
        e);
  }

  private static String getTitle(DataAccessException dae) {
    if (dae.getCause() instanceof PSQLException) {
      PSQLException cause = (PSQLException) dae.getCause();
      if (cause.getServerErrorMessage() != null) {
        String message = cause.getServerErrorMessage().getMessage();
        if (message.equals("canceling statement due to user request")) {
          message = "canceling statement due to timeout or user request";
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
        return pe.getServerErrorMessage().getDetail();
      }
    }
    return "";
  }
}
