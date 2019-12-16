package org.molgenis.emx2.sql;

import org.molgenis.emx2.MolgenisException;

class SqlGraphQueryException extends MolgenisException {
  public SqlGraphQueryException(String title, String detail, Object... params) {
    super(title, String.format(detail, params));
  }
}
