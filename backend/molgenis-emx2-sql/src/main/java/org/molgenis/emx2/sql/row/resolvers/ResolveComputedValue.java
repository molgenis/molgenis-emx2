package org.molgenis.emx2.sql.row.resolvers;

import static org.molgenis.emx2.ColumnType.AUTO_ID;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.sql.JavascriptContextBuilder;
import org.molgenis.emx2.utils.TypeUtils;

public class ResolveComputedValue {

  private ResolveComputedValue() {
    // hide constructor
  }

  public static void apply(Map<String, Object> context, Column column, Row row) {
    apply(context, List.of(column), row);
  }

  public static void apply(List<Column> columns, List<Row> rows) {
    for (Row row : rows) {
      Map<String, Object> context = JavascriptContextBuilder.fromRow(columns, row);
      apply(context, columns, row);
    }
  }

  public static void apply(Map<String, Object> javascriptContext, List<Column> columns, Row row) {
    for (Column column : columns) {
      if (!AUTO_ID.equals(column.getColumnType()) && column.getComputed() != null) {
        Object computedValue = executeJavascriptOnMap(column.getComputed(), javascriptContext);
        TypeUtils.addFieldObjectToRow(column, computedValue, row);
      }
    }
  }
}
