package org.molgenis.emx2.sql.row.computers.validators;

import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;

public interface RowValidator {

  void apply(Column columns, Row row) throws MolgenisException;
}
