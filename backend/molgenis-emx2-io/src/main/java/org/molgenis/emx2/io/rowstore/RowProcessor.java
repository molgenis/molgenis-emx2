package org.molgenis.emx2.io.rowstore;

import java.util.Iterator;
import org.molgenis.emx2.Row;

public interface RowProcessor {
  void process(Iterator<Row> iterator);
}
