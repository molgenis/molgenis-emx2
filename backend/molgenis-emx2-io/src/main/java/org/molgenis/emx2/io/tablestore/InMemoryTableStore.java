package org.molgenis.emx2.io.tablestore;

import java.util.*;
import java.util.stream.StreamSupport;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.tablestore.processor.RowProcessor;

public class InMemoryTableStore implements TableStore {

  private final Map<String, List<Row>> store = new HashMap<>();

  @Override
  public void writeTable(String name, List<String> columnNames, Iterable<Row> rows) {
    List<Row> materialized =
        StreamSupport.stream(rows.spliterator(), false)
            .map(row -> alignRowToColumns(row, columnNames))
            .toList();
    store.put(name, materialized);
  }

  private Row alignRowToColumns(Row row, List<String> columnNames) {
    for (String columnName : columnNames) {
      if (!row.containsName(columnName)) {
        row.set(columnName, null);
      }
    }
    for (String columnName : row.getColumnNames()) {
      if (!columnNames.contains(columnName)) {
        row.clear(columnName);
      }
    }
    return row;
  }

  @Override
  public Iterable<Row> readTable(String name) {
    return store.getOrDefault(name, List.of());
  }

  @Override
  public void processTable(String name, RowProcessor processor) {
    processor.process(readTable(name).iterator(), this);
  }

  @Override
  public boolean containsTable(String name) {
    return store.containsKey(name);
  }

  @Override
  public Collection<String> getTableNames() {
    return store.keySet();
  }
}
