package org.molgenis.emx2.io.stores;

import org.molgenis.emx2.Row;

import java.io.IOException;
import java.util.List;

public interface RowStore {

  void write(String name, List<Row> rows);

  List<Row> read(String name);

  boolean containsTable(String name);
}
