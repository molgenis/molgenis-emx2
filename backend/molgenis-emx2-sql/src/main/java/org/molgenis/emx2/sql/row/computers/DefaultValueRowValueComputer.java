package org.molgenis.emx2.sql.row.computers;

import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascript;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;

public class DefaultValueRowValueComputer implements RowValueComputer {

  private final List<Column> columns;

  public DefaultValueRowValueComputer(List<Column> columns) {
    this.columns = columns;
  }

  @Override
  public void apply(Column column, Row row) {
    Map<String, Object> javascriptContext = JavascriptContextBuilder.fromRow(columns, row);

    if (isComputed(column)) {
      computeDefaultValue(row, column, javascriptContext);
    } else {
      row.set(column.getName(), column.getDefaultValue());
    }
  }

  @Override
  public boolean shouldComputeForColumn(Column column, Row row) {
    return column.getDefaultValue() != null && !row.notNull(column.getName());
  }

  private static void computeDefaultValue(Row row, Column c, Map<String, Object> graph) {
    try {
      if (c.isRefArray()) {
        TypeUtils.convertRefArrayToRow(
            (List) executeJavascriptOnMap(c.getDefaultValue().substring(1), graph, List.class),
            row,
            c);
      } else if (c.isRef()) {
        TypeUtils.convertRefToRow(
            (Map)
                executeJavascriptOnMap(
                    "(" + c.getDefaultValue().substring(1) + ")", graph, Map.class),
            row,
            c);
      } else {
        row.set(c.getName(), executeJavascript(c.getDefaultValue().substring(1)));
      }
    } catch (Exception e) {
      throw new MolgenisException(
          "Error in defaultValue of column " + c.getName() + ": " + e.getMessage());
    }
  }

  private static boolean isComputed(Column c) {
    return c.getDefaultValue().startsWith("=");
  }
}
