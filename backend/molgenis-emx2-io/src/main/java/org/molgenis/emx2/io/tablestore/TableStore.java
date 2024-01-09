package org.molgenis.emx2.io.tablestore;

import java.util.Collection;
import java.util.List;
import org.molgenis.emx2.Row;

public interface TableStore {

  void writeTable(String name, List<String> columnNames, Iterable<Row> rows);

  Iterable<Row> readTable(String name);

  void processTable(String name, RowProcessor processor);

  boolean containsTable(String name);

  Collection<String> tableNames();
}
