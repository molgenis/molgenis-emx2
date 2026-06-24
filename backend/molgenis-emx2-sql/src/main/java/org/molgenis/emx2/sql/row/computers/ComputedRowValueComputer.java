package org.molgenis.emx2.sql.row.computers;

import static org.molgenis.emx2.ColumnType.AUTO_ID;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.utils.TypeUtils;

public class ComputedRowValueComputer implements RowValueComputer {

  private final List<Column> contextColumns;

  public ComputedRowValueComputer(List<Column> contextColumns) {
    this.contextColumns = contextColumns;
  }

  public void apply(List<Row> rows) {
    for (Row row : rows) {
      apply(contextColumns, row);
    }
  }

  @Override
  public void apply(Column column, Row row) {
    apply(List.of(column), row);
  }

  public void apply(List<Column> columns, Row row) {
    Map<String, Object> context = JavascriptContextBuilder.fromRow(this.contextColumns, row);
    for (Column column : columns) {
      if (!AUTO_ID.equals(column.getColumnType()) && column.getComputed() != null) {
        Object computedValue = executeJavascriptOnMap(column.getComputed(), context);
        TypeUtils.addFieldObjectToRow(column, computedValue, row);
      }
    }
  }

  @Override
  public boolean shouldComputeForColumn(Column column, Row row) {
    return column.getComputed() != null;
  }
}
