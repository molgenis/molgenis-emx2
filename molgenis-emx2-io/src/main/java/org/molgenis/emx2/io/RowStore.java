package org.molgenis.emx2.io;

import org.molgenis.Row;

import java.io.IOException;
import java.util.List;

public interface RowStore {

  static final String CSV_EXTENSION = ".csv";

  void write(String name, List<Row> rows) throws IOException;

  List<Row> read(String name) throws IOException;
}
