package org.molgenis.emx2.io.stores;

import org.molgenis.data.Row;

import java.io.IOException;
import java.util.List;

public interface RowStore {

  void write(String name, List<Row> rows) throws IOException;

  List<Row> read(String name) throws IOException;

  boolean contains(String name) throws IOException;
}
