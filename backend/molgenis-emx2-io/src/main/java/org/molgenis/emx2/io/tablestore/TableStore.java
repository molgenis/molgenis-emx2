package org.molgenis.emx2.io.tablestore;

import java.util.Collection;
import java.util.List;
import org.molgenis.emx2.NameMapper;
import org.molgenis.emx2.Row;

public interface TableStore {

  void writeTable(String name, List<String> columnNames, NameMapper mapper, Iterable<Row> rows);

  Iterable<Row> readTable(String name);

  Iterable<Row> readTable(String name, NameMapper mapper);

  void processTable(String name, NameMapper mapper, RowProcessor processor);

  boolean containsTable(String name);

  Collection<String> tableNames();
}
