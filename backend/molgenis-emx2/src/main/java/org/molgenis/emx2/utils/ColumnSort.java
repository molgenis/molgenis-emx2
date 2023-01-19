package org.molgenis.emx2.utils;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Column;

public class ColumnSort {
  private ColumnSort() {
    // hide constructor
  }

  public static void sortColumnsByDependency(List<Column> columnList) {
    List<Column> result = new ArrayList<>();

    for (Column column : columnList) {
      // keys must be last
      if (!column.isPrimaryKey()) {
        result.add(column);
      }
    }
    for (Column column : columnList) {
      if (column.isPrimaryKey()) {
        result.add(column);
      }
    }

    columnList.clear();
    columnList.addAll(result);
  }
}
