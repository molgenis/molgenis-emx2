package org.molgenis.emx2.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.TableMetadata;

public class TableSort {

  private TableSort() {
    // hide constructor
  }

  public static void sortTableByDependency(List<TableMetadata> tableList) {
    ArrayList<TableMetadata> result = new ArrayList<>();
    ArrayList<TableMetadata> todo = new ArrayList<>(tableList);

    // ensure deterministic order
    todo.sort(
        new Comparator<TableMetadata>() {
          @Override
          public int compare(TableMetadata o1, TableMetadata o2) {
            return o1.getTableName().compareTo(o2.getTableName());
          }
        });

    // dependency come from foreign key and from inheritance

    while (!todo.isEmpty()) {
      int size = todo.size();
      for (int i = 0; i < todo.size(); i++) {
        TableMetadata current = todo.get(i);
        boolean depends = false;

        for (int j = 0; j < todo.size(); j++) {
          if (current.getInheritName() != null
              && current.getImportSchema() == null
              && todo.get(j).equals(current.getInheritedTable())) {
            depends = true;
            break;
          }
        }
        if (!depends)
          for (Column c : current.getPrimaryKeyColumns()) {
            if (c.getRefTableName() != null && !c.isRefback()) {
              for (int j = 0; j < todo.size(); j++) {
                if (i != j && (todo.get(j).getTableName().equals(c.getRefTableName()))) {
                  depends = true;
                  break;
                }
              }
            }
          }
        if (!depends) {
          result.add(todo.get(i));
          todo.remove(i);
        }
      }
      // check for circular relationship
      if (size == todo.size()) {
        throw new MolgenisException(
            "circular dependency error: following tables have circular dependency: "
                + todo.stream().map(TableMetadata::getTableName).collect(Collectors.joining(",")));
      }
    }
    tableList.clear();
    tableList.addAll(result);
  }

  public static void sortTableByImportDependency(List<TableMetadata> tableList) {
    sortTableByDependency(tableList);

    Map<String, Set<String>> dependenciesByTableName = new HashMap<>();
    for (TableMetadata table : tableList) {
      dependenciesByTableName.put(table.getTableName(), getImportDependencies(table));
    }

    List<TableMetadata> todo = new ArrayList<>(tableList);
    tableList.clear();
    while (!todo.isEmpty()) {
      Set<String> pending =
          todo.stream().map(TableMetadata::getTableName).collect(Collectors.toSet());
      TableMetadata next =
          todo.stream()
              .filter(
                  table ->
                      dependenciesByTableName.get(table.getTableName()).stream()
                          .noneMatch(pending::contains))
              .findFirst()
              .orElse(todo.getFirst());
      todo.remove(next);
      tableList.add(next);
    }
  }

  private static Set<String> getImportDependencies(TableMetadata table) {
    Set<String> dependencies = new HashSet<>();
    if (table.getInheritName() != null && table.getImportSchema() == null) {
      dependencies.add(table.getInheritName());
    }
    for (Column column : table.getPrimaryKeyColumns()) {
      if (column.getRefTableName() != null && !column.isRefback()) {
        dependencies.add(column.getRefTableName());
      }
    }

    Set<String> expressionVariables =
        table.getColumns().stream()
            .flatMap(column -> column.getExpressionVariables().stream())
            .collect(Collectors.toSet());
    for (Column column : table.getColumns()) {
      if (column.isReference()
          && expressionVariables.contains(column.getIdentifier())
          && column.getRefSchemaName().equals(table.getSchemaName())) {
        dependencies.add(column.getRefTableName());
      }
    }

    dependencies.remove(table.getTableName());
    return dependencies;
  }
}
