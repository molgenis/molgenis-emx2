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
  private final Map<String, Set<String>> variablesByIdentifier = new HashMap<>();
  private final List<Column> sorted = new ArrayList<>();
  private final Set<String> resolved = new HashSet<>();
  private final Set<String> resolving = new LinkedHashSet<>();

  private ColumnDependencies(List<Column> columns) {
    this.columns = columns;
    for (Column column : columns) {
      variablesByIdentifier.put(column.getIdentifier(), getExpressionVariables(column));
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
    String expression = column.getDefaultValueExpression();
    if (expression == null) {
      return null;
    }

    return column.isRef() ? "(" + expression + ")" : expression;
  }

  private List<Column> sort() {
    for (Column column : columns) {
      resolve(column);
    }
    return List.copyOf(sorted);
  }

  private void resolve(Column column) {
    if (resolved.contains(column.getIdentifier())) {
      return;
    }

    resolving.add(column.getIdentifier());
    for (Column dependency : columns) {
      if (!dependsOn(column, dependency)) {
        continue;
      }

      if (resolving.contains(dependency.getIdentifier())) {
        throw new MolgenisException(
            "Circular dependency between " + column.getName() + " and " + dependency.getName());
      }

      resolve(dependency);
    }
    resolving.remove(column.getIdentifier());

    resolved.add(column.getIdentifier());
    sorted.add(column);
  }

  private boolean dependsOn(Column column, Column dependency) {
    return !dependency.getIdentifier().equals(column.getIdentifier())
        && variablesByIdentifier.get(column.getIdentifier()).contains(dependency.getIdentifier());
  }
}
