package org.molgenis.emx2.sql;

import static org.molgenis.emx2.ColumnType.AUTO_ID;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.sql.row.computers.RowToMapConverter;
import org.molgenis.emx2.utils.TypeUtils;

public class RowComputer {

  private static final RowToMapConverter converter = new RowToMapConverter();

  public void applyComputed(List<Column> columns, List<Row> rows) {
    for (Row row : rows) {
      applyComputed(columns, row);
    }
  }

  public void applyComputed(List<Column> columns, Row row) {
    Map<String, Object> graph = converter.convertRowToMap(columns, row);
    for (Column column : columns) {
      if (!AUTO_ID.equals(column.getColumnType()) && column.getComputed() != null) {
        Object computedValue = executeJavascriptOnMap(column.getComputed(), graph);
        TypeUtils.addFieldObjectToRow(column, computedValue, row);
      }
    }
  }
}
