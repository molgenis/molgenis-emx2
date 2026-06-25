package org.molgenis.emx2.sql.processors;

import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascript;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;

public class DefaultValueProcessor {

  private DefaultValueProcessor() {
    throw new UnsupportedOperationException("Cannot instantiate utility class");
  }

  public static void apply(Map<String, Object> javascriptContext, Column column, Row row) {
    if (isComputed(column)) {
      applyComputedDefaultValue(row, column, javascriptContext);
    } else {
      row.set(column.getName(), column.getDefaultValue());
    }
  }

  private static void applyComputedDefaultValue(
      Row row, Column column, Map<String, Object> context) {
    String expression = column.getDefaultValue().substring(1);
    try {
      if (column.isRefArray()) {
        List<Map<String, Object>> result =
            (List<Map<String, Object>>) executeJavascriptOnMap(expression, context, List.class);
        TypeUtils.convertRefArrayToRow(result, row, column);
      } else if (column.isRef()) {
        Map<String, Object> result =
            (Map<String, Object>)
                executeJavascriptOnMap("(" + expression + ")", context, Map.class);
        TypeUtils.convertRefToRow(result, row, column);
      } else {
        row.set(column.getName(), executeJavascript(expression));
      }
    } catch (Exception e) {
      throw new MolgenisException(
          "Error in defaultValue of column " + column.getName() + ": " + e.getMessage());
    }
  }

  private static boolean isComputed(Column column) {
    return column.getDefaultValue().startsWith("=");
  }
}
