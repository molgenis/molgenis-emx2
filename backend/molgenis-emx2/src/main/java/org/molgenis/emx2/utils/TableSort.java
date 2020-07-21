package org.molgenis.emx2.utils;

import org.molgenis.emx2.Column;
import org.molgenis.emx2.TableMetadata;

import java.util.ArrayList;
import java.util.List;

import static org.molgenis.emx2.ColumnType.REFBACK;

public class TableSort {

  private TableSort() {
    // hide constructor
  }

  public static void sortTableByDependency(List<TableMetadata> tableList) {
    ArrayList<TableMetadata> result = new ArrayList<>();
    ArrayList<TableMetadata> todo = new ArrayList<>(tableList);

    while (!todo.isEmpty()) {
      for (int i = 0; i < todo.size(); i++) {
        TableMetadata current = todo.get(i);
        boolean depends = false;
        for (int j = 0; j < todo.size(); j++) {
          if (todo.get(j).equals(current.getInheritedTable())) {
            depends = true;
            break;
          }
        }
        if (!depends)
          for (Column c : current.getLocalColumns()) {
            if (c.getRefTableName() != null && !c.getColumnType().equals(REFBACK)) {
              for (int j = 0; j < todo.size(); j++) {
                // if depends on on in todo, than skip to next
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
    }
    tableList.clear();
    tableList.addAll(result);
  }
}
