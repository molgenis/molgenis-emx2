package org.molgenis.emx2.fairmapper;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jetbrains.annotations.NotNull;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.processor.RowProcessor;

/**
 * A {@link TableStore} implementation that stores table data as lazy {@link Stream}s of {@link Row}
 * objects, avoiding materialising entire tables into memory at once.
 *
 * <p>Each table is stored as a single {@link Stream}, which means rows are only processed when the
 * stream is consumed. This makes {@code StreamingTableStore} well-suited for large datasets where
 * memory efficiency is a priority.
 *
 * <p><strong>Important — streams are read-once:</strong> Each table's stream can only be consumed a
 * single time. Calling {@link #readTable(String)} and iterating the result more than once, or
 * calling it multiple times for the same table, will throw an {@link IllegalStateException} on the
 * second traversal. Workflows must ensure each table is read exactly once, if you want to be able
 * to access the table again after the read, you have to rewrite again. If multiple passes over the
 * same data are required, consider a {@link TableStore} implementation that materialises rows (e.g.
 * backed by a {@link List}).
 */
public class StreamingTableStore implements TableStore {

  private final Map<String, Stream<Row>> store = new HashMap<>();

  @Override
  public void writeTable(String name, List<String> columnNames, Iterable<Row> rows) {
    writeTable(name, columnNames, StreamSupport.stream(rows.spliterator(), false));
  }

  public void writeTable(String name, List<String> columnNames, Stream<Row> rows) {
    store.put(name, rows.map(row -> validateRowWithColumnNames(row, columnNames)));
  }

  @NotNull
  private static Row validateRowWithColumnNames(Row row, List<String> columnNames) {
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
    return store.get(name)::iterator;
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
