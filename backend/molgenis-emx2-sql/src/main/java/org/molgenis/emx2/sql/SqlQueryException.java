package org.molgenis.emx2.sql;

import org.molgenis.emx2.MolgenisException;

class SqlQueryException extends MolgenisException {
  public SqlQueryException(String title, String detail, Object... params) {
    super(title, String.format(detail, params));
  }
}
