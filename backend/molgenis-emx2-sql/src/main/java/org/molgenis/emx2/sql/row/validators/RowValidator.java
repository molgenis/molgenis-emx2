package org.molgenis.emx2.sql.row.validators;

import java.util.List;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;

public interface RowValidator {

  void apply(List<Column> columns, Row row) throws MolgenisException;
}
