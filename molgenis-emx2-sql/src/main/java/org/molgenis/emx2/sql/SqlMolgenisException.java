package org.molgenis.emx2.sql;

import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.utils.MolgenisException;
import org.postgresql.util.PSQLException;

public class SqlMolgenisException extends MolgenisException {

  public SqlMolgenisException(DataAccessException dae) {
    super(getTitle(dae) + getDetail(dae), dae);
    Throwable cause = dae.getCause();
    if (cause instanceof PSQLException) translate((PSQLException) cause);
  }

  public SqlMolgenisException(String message, DataAccessException dae) {
    super(message + getTitle(dae) + "." + getDetail(dae), dae);
  }

  private static String getTitle(DataAccessException dae) {
    if (dae.getCause() instanceof PSQLException) {
      return ((PSQLException) dae.getCause()).getServerErrorMessage().getMessage();
    }
    return dae.getMessage();
  }

  private static String getDetail(DataAccessException dae) {
    if (dae.getCause() instanceof PSQLException) {
      PSQLException pe = (PSQLException) dae.getCause();
      if (pe.getServerErrorMessage().getDetail() != null) {
        return pe.getServerErrorMessage().getDetail();
      } else return pe.getMessage();
    }
    return "";
  }

  private static String translate(PSQLException cause) {
    String errorCode = cause.getSQLState();
    // https://www.postgresql.org/docs/current/protocol-error-fields.html

    switch (errorCode) {
      case "23502":
        return "not_null_violation";
      case "23503":
        return "foreign_key_violation";
      case "23505":
        return "unique_violation";
      case "42P06":
        return "duplicate_schema";
      case "42501":
        return "insufficent_privilege";
      case "42703":
        return "column_doesnt_exist";
      default:
        return "SqlMolgenisException has not yet implemented translation for type " + errorCode;
    }
  }
}
