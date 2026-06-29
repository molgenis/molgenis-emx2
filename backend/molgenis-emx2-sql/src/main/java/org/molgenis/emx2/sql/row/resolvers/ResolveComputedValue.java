package org.molgenis.emx2.sql.row.resolvers;

import static org.molgenis.emx2.ColumnType.AUTO_ID;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.sql.ContextGraphBuilder;
import org.molgenis.emx2.utils.TypeUtils;

public class ResolveComputedValue {

  private ResolveComputedValue() {
    throw new UnsupportedOperationException("Cannot instantiate utility class");
  }

  public static void apply(Map<String, Object> contextGraph, Column column, Row row) {
    apply(contextGraph, List.of(column), row);
  }

  public static void apply(List<Column> columns, List<Row> rows) {
    for (Row row : rows) {
      Map<String, Object> javascriptContext = ContextGraphBuilder.fromRow(columns, row);
      apply(javascriptContext, columns, row);
    }
  }

  public static void apply(Map<String, Object> contextGraph, List<Column> columns, Row row) {
    for (Column column : columns) {
      if (!AUTO_ID.equals(column.getColumnType()) && column.getComputed() != null) {
        Object computedValue = executeJavascriptOnMap(column.getComputed(), contextGraph);
        TypeUtils.addFieldObjectToRow(column, computedValue, row);
      }
    }
  }
}
