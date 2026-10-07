package org.molgenis.emx2.io.tablestore;

import java.util.function.Consumer;
import org.molgenis.emx2.Row;

@FunctionalInterface
public interface RowProducer {
  void produce(Consumer<Row> consumer);
}
