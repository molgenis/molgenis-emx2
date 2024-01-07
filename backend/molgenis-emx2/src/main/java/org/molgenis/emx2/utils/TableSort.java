package org.molgenis.emx2.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.molgenis.emx2.Column;
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
          for (Column c : current.getColumns()) {
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
        result.addAll(todo);
        break;
        //        throw new MolgenisException(
        //            "circular dependency error: following tables have circular dependency: "
        //                +
        // todo.stream().map(TableMetadata::getTableName).collect(Collectors.joining(",")));
      }
    }
    tableList.clear();
    tableList.addAll(result);
  }
}
