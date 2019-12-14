package org.molgenis.emx2.sql;

import org.molgenis.emx2.utils.MolgenisException;

class SqlGraphQueryException extends MolgenisException {
  public SqlGraphQueryException(String detail, Object... params) {
    super("QUERY_ERROR", "query error", String.format(detail, params));
  }
}
