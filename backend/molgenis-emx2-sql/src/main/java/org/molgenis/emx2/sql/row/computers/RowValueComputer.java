package org.molgenis.emx2.sql.row.computers;

import java.util.List;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;

public interface RowValueComputer {

  default void apply(Column column, List<Row> rows) {
    for (Row row : rows) {
      apply(column, row);
    }
  }

  void apply(Column column, Row row);

  boolean shouldComputeForColumn(Column column, Row row);
}
