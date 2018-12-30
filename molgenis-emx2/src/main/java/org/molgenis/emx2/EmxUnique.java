package org.molgenis.emx2;

import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.TypeName;

import java.util.ArrayList;
import java.util.List;

@TypeName("unique")
public class EmxUnique {
  EmxTable table;
  List<EmxColumn> columns = new ArrayList<>();

  public EmxUnique(EmxTable forTable) {
    this.table = forTable;
  }

  public EmxTable getTable() {
    return table;
  }

  public void setTable(EmxTable table) {
    this.table = table;
  }

  public EmxUnique addColumn(EmxColumn c) {
    this.columns.add(c);
    return this;
  }

  public List<EmxColumn> getColumns() {
    return columns;
  }

  public List<String> getColumnNames() {
    List<String> columnNames = new ArrayList<>();
    for (EmxColumn col : columns) {
      columnNames.add(col.getName());
    }
    return columnNames;
  }

  public void setColumns(List<EmxColumn> columns) {
    this.columns = columns;
  }

  @Id
  public String print() {
    StringBuilder builder = new StringBuilder();
    builder.append("EmxUnique(");
    for (EmxColumn c : columns) {
      builder.append("\n\t\t").append(c.toString());
    }
    builder.append(")");
    return builder.toString();
  }
}
