package org.molgenis.bean;

import org.molgenis.Column;
import org.molgenis.Table;
import org.molgenis.Unique;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UniqueBean implements Unique {
  private Table table;
  private List<? extends Column> columns;

  public UniqueBean(Table table, List<Column> columns) {
    this.table = table;
    this.columns = columns;
  }

  @Override
  public Table getTable() {
    return table;
  }

  @Override
  public Collection<Column> getColumns() {
    return Collections.unmodifiableList(columns);
  }

  @Override
  public Collection<String> getColumnNames() {
    List<String> names = new ArrayList<>();
    for (Column col : columns) {
      names.add(col.getName());
    }
    return Collections.unmodifiableList(names);
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("UNIQUE(");
    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) builder.append(", ");
      builder.append(columns.get(i).getName());
    }
    builder.append(")");
    return builder.toString();
  }
}
