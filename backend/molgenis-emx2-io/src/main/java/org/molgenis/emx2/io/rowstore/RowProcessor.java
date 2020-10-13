package org.molgenis.emx2.io.rowstore;

import org.molgenis.emx2.Row;

import java.util.Iterator;

public interface RowProcessor {
  void process(Iterator<Row> iterator);
}
