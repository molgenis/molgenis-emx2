package org.molgenis.sql;

import org.jooq.exception.DataAccessException;
import org.molgenis.MolgenisException;
import org.postgresql.util.PSQLException;

public class SqlMolgenisException extends MolgenisException {
  public SqlMolgenisException(DataAccessException dae) {
    super(dae);

    Throwable cause = dae.getCause();
    if (cause instanceof PSQLException) translate((PSQLException) cause);
  }

  private void translate(PSQLException cause) {
    String errorCode = cause.getSQLState();
    // https://www.postgresql.org/docs/current/protocol-error-fields.html

    switch (errorCode) {
      case "23503":
        this.setType("foreign_key_violation");
        break;
      case "23505":
        this.setType("unique_violation");
        break;
      default:
        this.setType("not_yet_typed");
    }
    this.setTitle(cause.getServerErrorMessage().getMessage());
    this.setDetail(cause.getServerErrorMessage().getDetail());
  }
}
