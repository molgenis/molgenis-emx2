package org.molgenis.emx2.io.rowstore;

import org.molgenis.emx2.Row;

import java.util.List;

public interface TableStore {

  void writeTable(String name, List<Row> rows);

  List<Row> readTable(String name);

  boolean containsTable(String name);
}
