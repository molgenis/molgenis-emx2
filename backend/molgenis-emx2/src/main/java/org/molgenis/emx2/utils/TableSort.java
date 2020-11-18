package org.molgenis.emx2.utils;

import static org.molgenis.emx2.ColumnType.REFBACK;

import java.util.ArrayList;
import java.util.List;
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

    while (!todo.isEmpty()) {
      int size = todo.size();
      for (int i = 0; i < todo.size(); i++) {
        TableMetadata current = todo.get(i);
        boolean depends = false;
        for (int j = 0; j < todo.size(); j++) {
          if (current.getInherit() != null
              && current.getImportSchema() == null
              && todo.get(j).equals(current.getInheritedTable())) {
            depends = true;
            break;
          }
        }
        if (!depends)
          for (Column c : current.getColumns()) {
            if (c.getRefTableName() != null && !c.getColumnType().equals(REFBACK)) {
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
}
