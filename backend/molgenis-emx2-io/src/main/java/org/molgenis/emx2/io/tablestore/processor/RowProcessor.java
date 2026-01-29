package org.molgenis.emx2.io.tablestore.processor;

import java.util.Iterator;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.tablestore.TableStore;

public interface RowProcessor {

  void process(Iterator<Row> iterator, TableStore source);
}
