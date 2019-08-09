package org.molgenis.beans;

import org.molgenis.Column;
import org.molgenis.Table;
import org.molgenis.Unique;

import java.util.*;

public class UniqueBean implements Unique {
  private Table table;
  private List<? extends Column> columns;

  public UniqueBean(TableMetadata table, List<Column> columns) {
    this.table = table;
    this.columns = columns;
  }

  @Override
  public Table getTable() {
    return table;
  }

  @Override
  public Collection<String> getColumnNames() {
    List<String> names = new ArrayList<>();
    for (Column col : columns) {
      names.add(col.getName());
    }
    return Collections.unmodifiableList(names);
  }

  @Override
  public String getSchemaName() {
    return getTable().getSchemaName();
  }

  @Override
  public String getTableName() {
    return getTable().getName();
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
