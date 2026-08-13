package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.SelectColumn.s;

import java.util.*;
import org.molgenis.emx2.SelectColumn;
import org.molgenis.emx2.Table;

// todo: refactor code to include PrimaryKey as well & this class defines the tables to process
public class TableColumnsSelector {
  // todo: should use Table instead, but would cause failing get due to getTablesSorted Tables are
  // not equal to the ones stored here
  private final Map<String, Set<SelectColumn>> tableColumns = new HashMap<>();

  public void addTable(Table table) {
    if (!tableColumns.containsKey(table.getIdentifier())) {
      tableColumns.put(table.getIdentifier(), new HashSet<>());
    }
  }

  public void addTables(Collection<Table> tables) {
    tables.forEach(this::addTable);
  }

  public void addColumn(Table table, String column) {
    addTable(table);
    tableColumns.get(table.getIdentifier()).add(s(column));
  }

  public void addColumns(Table table, List<String> columns) {
    addTable(table);
    tableColumns.get(table.getIdentifier()).addAll(columns.stream().map(SelectColumn::s).toList());
  }

  public boolean isDefined(Table table) {
    return tableColumns.get(table.getIdentifier()) != null;
  }

  public SelectColumn[] getSelectColumns(Table table) {
    return tableColumns.get(table.getIdentifier()).toArray(SelectColumn[]::new);
  }
}
