package org.molgenis.emx2.sql.row.computers;

import static org.molgenis.emx2.ColumnType.AUTO_ID;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascript;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;

public class DefaultValueRowValueComputer implements RowValueComputer {

  private static final RowToMapConverter converter = new RowToMapConverter();

  @Override
  public void apply(List<Column> columns, Row row) {
    Map<String, Object> graph = converter.convertRowToMap(columns, row);

    List<Column> toValidateAndCompute =
        columns.stream()
            .filter(c -> !c.isHeading())
            .filter(c -> !AUTO_ID.equals(c.getColumnType()))
            .filter(c -> c.getDefaultValue() != null && !row.notNull(c.getName()))
            .toList();

    for (Column c : toValidateAndCompute) {
      if (isComputed(c)) {
        computeDefaultValue(row, c, graph);
      } else {
        row.set(c.getName(), c.getDefaultValue());
      }
    }
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
