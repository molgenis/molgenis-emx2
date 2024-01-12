package org.molgenis.emx2.sql;

import org.molgenis.emx2.MolgenisException;

class SqlQueryException extends MolgenisException {
  public SqlQueryException(String message, Object... params) {
    super(String.format(message, params));
  }
}
