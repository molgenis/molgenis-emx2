package org.molgenis.emx2.sql;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.row.resolvers.ResolveComputedValue;
import org.molgenis.emx2.sql.row.resolvers.ResolveDefaultValue;
import org.molgenis.emx2.sql.row.validators.ValidateExpression;
import org.molgenis.emx2.sql.row.validators.ValidateRequired;
import org.molgenis.emx2.utils.JavaScriptUtils;

public class SqlRowProcessor {

  private final List<Column> columnsToProcess;
  private final List<Column> columns;

  public SqlRowProcessor(List<Column> columns) {
    this.columns = columns;
    this.columnsToProcess =
        ColumnDependencies.sortByDependencies(
            columns.stream().filter(not(Column::isHeading)).filter(not(Column::isAutoId)).toList());
  }

  public void validateAndCompute(List<Row> rows) {
    for (Row row : rows) {
      validateAndCompute(row);
    }
  }

  public void validateAndCompute(Row row) throws MolgenisException {
    Map<String, Object> context = JavascriptContextBuilder.fromRow(columns, row);

    for (Column column : columnsToProcess) {
      if (column.hasDefaultValue() && !row.notNull(column.getName())) {
        ResolveDefaultValue.apply(context, column, row);
      } else if (column.hasComputed()) {
        ResolveComputedValue.apply(context, column, row);
      } else if (isColumnVisible(column, context)) {
        ValidateRequired.apply(context, column, row);
        ValidateExpression.apply(context, column);
      } else {
        row.clear(column);
      }

      JavascriptContextBuilder.updateContext(context, row, column);
    }
  }

  private static boolean isColumnVisible(Column column, Map<String, Object> context) {
    if (column.getVisible() == null) {
      return true;
    }

    Object visibleResult = JavaScriptUtils.executeJavascriptOnMap(column.getVisible(), context);
    return visibleResult != null && !Boolean.FALSE.equals(visibleResult);
  }
}
