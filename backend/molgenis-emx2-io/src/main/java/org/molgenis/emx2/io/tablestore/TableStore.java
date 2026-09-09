package org.molgenis.emx2.io.tablestore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.tablestore.processor.RowProcessor;

public interface TableStore {

  void writeTable(String name, List<String> columnNames, Iterable<Row> rows);

  default void writeTableStreaming(String name, List<String> columnNames, RowProducer rows) {
    List<Row> collected = new ArrayList<>();
    rows.produce(collected::add);
    writeTable(name, columnNames, collected);
  }

  Iterable<Row> readTable(String name);

  void processTable(String name, RowProcessor processor);

  boolean containsTable(String name);

  Collection<String> getTableNames();
}
