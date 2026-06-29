package org.molgenis.emx2.sql;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.row.resolvers.PrefixEditRole;
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
    columnsToProcess =
        columns.stream().filter(not(Column::isHeading)).filter(not(Column::isAutoId)).toList();
  }

  public void validateAndCompute(List<Row> rows) {
    for (Row row : rows) {
      validateAndCompute(row);
    }
  }

  public void validateAndCompute(Row row) throws MolgenisException {
    Map<String, Object> jsGraph = ContextGraphBuilder.fromRow(columns, row);

    for (Column column : columnsToProcess) {
      if (column.isMgEditRoleColumn()) {
        PrefixEditRole.apply(column, row);
      } else if (column.hasDefaultValue() && !row.notNull(column.getName())) {
        ResolveDefaultValue.apply(jsGraph, column, row);
      } else if (column.hasComputed()) {
        ResolveComputedValue.apply(jsGraph, column, row);
      } else if (isColumnVisible(column, jsGraph)) {
        ValidateRequired.apply(jsGraph, column, row);
        ValidateExpression.apply(jsGraph, column);
      } else {
        row.clear(column);
      }
    }
  }

  private static boolean isColumnVisible(Column column, Map<String, Object> jsContext) {
    if (column.getVisible() == null) {
      return true;
    }

    Object visibleResult = JavaScriptUtils.executeJavascriptOnMap(column.getVisible(), jsContext);
    return visibleResult != null && !Boolean.FALSE.equals(visibleResult);
  }
}
