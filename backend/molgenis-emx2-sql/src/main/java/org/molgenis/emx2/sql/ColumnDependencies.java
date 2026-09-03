package org.molgenis.emx2.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.utils.JavaScriptParser;

final class ColumnDependencies {

  private final List<Column> columns;
  private final Map<String, Set<String>> variablesByColumn = new HashMap<>();
  private final List<Column> sorted = new ArrayList<>();
  private final Set<String> resolved = new HashSet<>();
  private final Set<String> resolving = new LinkedHashSet<>();

  private ColumnDependencies(List<Column> columns) {
    this.columns = columns;
    for (Column column : columns) {
      variablesByColumn.put(column.getName(), getExpressionVariables(column));
    }
  }

  static List<Column> sortByDependencies(List<Column> columns) {
    return new ColumnDependencies(columns).sort();
  }

  static Set<String> getExpressionVariables(Column column) {
    Set<String> variables = new HashSet<>();
    variables.addAll(JavaScriptParser.getReferencedVariables(column.getComputed()));
    variables.addAll(JavaScriptParser.getReferencedVariables(getDefaultValueExpression(column)));
    variables.addAll(JavaScriptParser.getReferencedVariables(column.getRequired()));
    variables.addAll(JavaScriptParser.getReferencedVariables(column.getValidation()));
    variables.addAll(JavaScriptParser.getReferencedVariables(column.getVisible()));
    return variables;
  }

  private static String getDefaultValueExpression(Column column) {
    String defaultValue = column.getDefaultValue();
    if (defaultValue == null || !defaultValue.startsWith("=")) {
      return null;
    }

    String expression = defaultValue.substring(1);
    return column.isRef() ? "(" + expression + ")" : expression;
  }

  private List<Column> sort() {
    for (Column column : columns) {
      resolve(column);
    }
    return List.copyOf(sorted);
  }

  private void resolve(Column column) {
    if (resolved.contains(column.getName())) {
      return;
    }

    resolving.add(column.getName());
    for (Column dependency : columns) {
      if (!dependsOn(column, dependency)) {
        continue;
      }

      if (resolving.contains(dependency.getName())) {
        throw new MolgenisException(
            "Circular dependency between " + column.getName() + " and " + dependency.getName());
      }

      resolve(dependency);
    }
    resolving.remove(column.getName());

    resolved.add(column.getName());
    sorted.add(column);
  }

  private boolean dependsOn(Column column, Column dependency) {
    return !dependency.getName().equals(column.getName())
        && variablesByColumn.get(column.getName()).contains(dependency.getIdentifier());
  }
}
