package org.molgenis.emx2.sql;

import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.utils.MolgenisException;
import org.postgresql.util.PSQLException;

public class SqlMolgenisException extends MolgenisException {

  public SqlMolgenisException(DataAccessException dae) {
    super(getTitle(dae) + getDetail(dae), dae);
    Throwable cause = dae.getCause();
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
}
