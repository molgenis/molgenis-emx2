package org.molgenis.emx2.sql;

import org.molgenis.emx2.MolgenisException;

class SqlQueryGraphException extends MolgenisException {
  public SqlQueryGraphException(String title, String detail, Object... params) {
    super(title, String.format(detail, params));
  }
}
