package org.molgenis.emx2.sql;

import org.molgenis.emx2.utils.MolgenisException;

class SqlGraphQueryException extends MolgenisException {
  public SqlGraphQueryException(String detail, Object... params) {
    super(String.format(detail, params));
  }
}
