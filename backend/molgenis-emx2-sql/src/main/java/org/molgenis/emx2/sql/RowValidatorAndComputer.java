package org.molgenis.emx2.sql;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.resolvers.*;
import org.molgenis.emx2.sql.resolvers.validators.ExpressionValidator;
import org.molgenis.emx2.sql.resolvers.validators.RequiredValidator;
import org.molgenis.emx2.utils.JavaScriptUtils;

public class RowValidatorAndComputer {

  private final List<Column> columnsToProcess;
  private final List<Column> columns;

  public RowValidatorAndComputer(List<Column> columns) {
    this.columns = columns;
    columnsToProcess =
        columns.stream().filter(not(Column::isHeading)).filter(not(Column::isAutoId)).toList();
  }

  public void validateAndCompute(Row row) throws MolgenisException {
    Map<String, Object> javascriptContext = JavascriptContextBuilder.fromRow(columns, row);

    for (Column column : columnsToProcess) {
      if (column.isMgEditRoleColumn()) {
        SystemRolePrefixResolver.apply(column, row);
      }
      if (column.hasDefaultValue() && !row.notNull(column.getName())) {
        DefaultValueResolver.apply(javascriptContext, column, row);
      }
      if (column.hasComputed()) {
        ComputedExpressionResolver.apply(javascriptContext, column, row);
      }
      if (isColumnVisible(column, row, columnsToProcess)) {
        RequiredValidator.apply(javascriptContext, column, row);
        ExpressionValidator.apply(javascriptContext, column, row);
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
