package org.molgenis.emx2.sql.resolvers;

import java.util.List;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;

public interface RowValueResolver {

  default void apply(Column column, List<Row> rows) {
    for (Row row : rows) {
      apply(column, row);
    }
  }

  void apply(Column column, Row row);

  boolean shouldResolveForColumn(Column column, Row row);
}
