package org.molgenis.emx2.sql;

import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.utils.MolgenisException;
import org.postgresql.util.PSQLException;

public class SqlMolgenisException extends MolgenisException {
  public SqlMolgenisException(DataAccessException dae) {
    super(dae);

    Throwable cause = dae.getCause();
    if (cause instanceof PSQLException) translate((PSQLException) cause);
  }

  public SqlMolgenisException(String type, String title, DataAccessException dae) {
    this(type, title, "", dae);
  }

  public SqlMolgenisException(
      String type, String title, String detailMessage, DataAccessException dae) {
    super(dae);
    this.type = type;
    this.title = title;
    Throwable cause = dae.getCause();
    if (cause instanceof PSQLException) {
      detailMessage =
          (detailMessage + " " + ((PSQLException) cause).getServerErrorMessage().getDetail())
              .trim();
      this.detail =
          ((PSQLException) cause).getServerErrorMessage().getMessage()
              + (detailMessage == null ? "" : " " + detailMessage);
    }
  }

  private void translate(PSQLException cause) {
    String errorCode = cause.getSQLState();
    // https://www.postgresql.org/docs/current/protocol-error-fields.html

    switch (errorCode) {
      case "23503":
        this.type = "foreign_key_violation";
        break;
      case "23505":
        this.type = "unique_violation";
        break;
      case "42P06":
        this.type = "duplicate_schema";
        break;
      case "42501":
        this.type = "insufficent_privilege";
        break;
      default:
        this.type =
            "SqlMolgenisException has not yet implemented translation for type " + errorCode;
    }
    this.title = cause.getServerErrorMessage().getMessage();
    this.detail = cause.getServerErrorMessage().getDetail();
  }
}
