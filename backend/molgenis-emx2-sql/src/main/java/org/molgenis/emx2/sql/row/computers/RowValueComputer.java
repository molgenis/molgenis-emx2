package org.molgenis.emx2.sql.row.computers;

import java.util.List;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;

public interface RowValueComputer {

  void apply(List<Column> columns, Row row);
}
