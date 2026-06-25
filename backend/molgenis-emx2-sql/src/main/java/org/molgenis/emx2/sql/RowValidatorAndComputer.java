package org.molgenis.emx2.sql;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.resolvers.*;
import org.molgenis.emx2.utils.JavaScriptUtils;

public class RowValidatorAndComputer {

  private final List<Column> columnsToProcess;

  public RowValidatorAndComputer(List<Column> columns) {
    columnsToProcess =
        columns.stream().filter(not(Column::isHeading)).filter(not(Column::isAutoId)).toList();
  }

  public void validateAndCompute(Row row) throws MolgenisException {
    for (Column column : columnsToProcess) {
      if (column.isMgEditRoleColumn()) {
        new SystemRolePrefixResolver().apply(column, row);
      }
      if (column.hasDefaultValue() && !row.notNull(column.getName())) {
        new DefaultValueResolver(columnsToProcess).apply(column, row);
      }
      if (column.hasComputed()) {
        new ComputedExpressionResolver(columnsToProcess).apply(column, row);
      }
      if (isColumnVisible(column, row, columnsToProcess)) {
        new VisibilityResolver(columnsToProcess).apply(column, row);
      } else {
        row.clear(column);
      }
    }
  }

  private static boolean isColumnVisible(Column column, Row row, List<Column> contextColumns) {
    if (column.getVisible() == null) {
      return true;
    }

    Map<String, Object> javascriptContext = JavascriptContextBuilder.fromRow(contextColumns, row);
    Object visibleResult =
        JavaScriptUtils.executeJavascriptOnMap(column.getVisible(), javascriptContext);
    return (visibleResult == null || Boolean.FALSE.equals(visibleResult));
  }
}
