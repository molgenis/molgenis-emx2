package org.molgenis.emx2.sql;

import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.row.resolvers.ResolveComputedValue;
import org.molgenis.emx2.sql.row.resolvers.ResolveDefaultValue;
import org.molgenis.emx2.sql.row.validators.ValidateExpression;
import org.molgenis.emx2.sql.row.validators.ValidateRequired;
import org.molgenis.emx2.utils.JavaScriptUtils;

public class SqlRowProcessor {

  private static final ColumnDependencyComparator DEPENDENCY_COMPARATOR =
      new ColumnDependencyComparator();

  private final List<Column> columnsToProcess;
  private final List<Column> columns;

  public SqlRowProcessor(List<Column> columns) {
    this.columns = columns;
    this.columnsToProcess =
        columns.stream()
            .filter(not(Column::isHeading))
            .filter(not(Column::isAutoId))
            .sorted(DEPENDENCY_COMPARATOR)
            .toList();
  }

  public void validateAndCompute(List<Row> rows) {
    for (Row row : rows) {
      validateAndCompute(row);
    }
  }

  public static void validateAndCompute(
      List<Column> baseColumns,
      List<Row> rows,
      boolean hasModuleColumns,
      Function<Row, List<Column>> activeModuleColumns) {
    if (!hasModuleColumns) {
      new SqlRowProcessor(baseColumns).validateAndCompute(rows);
      return;
    }
    for (Row row : rows) {
      List<Column> union = union(baseColumns, activeModuleColumns.apply(row));
      new SqlRowProcessor(union).validateAndCompute(row);
    }
  }

  private static List<Column> union(List<Column> baseColumns, List<Column> moduleColumns) {
    List<Column> union = new ArrayList<>(baseColumns);
    Set<String> unionNames =
        new LinkedHashSet<>(baseColumns.stream().map(Column::getName).toList());
    for (Column column : moduleColumns) {
      if (unionNames.add(column.getName())) {
        union.add(column);
      }
    }
    return union;
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

  private static final class ColumnDependencyComparator implements Comparator<Column> {

    @Override
    public int compare(Column o1, Column o2) {
      if (o1.getComputed() == null && o2.getComputed() == null) {
        return 0;
      }

      int order = 0;
      if (o1.hasDependencyOn(o2)) {
        order = 1;
      }

      if (o2.hasDependencyOn(o1)) {
        if (order == 1) {
          throw new MolgenisException(
              "Circular dependency between " + o1.getName() + " and " + o2.getName());
        }

        order = -1;
      }

      return order;
    }
  }
}
