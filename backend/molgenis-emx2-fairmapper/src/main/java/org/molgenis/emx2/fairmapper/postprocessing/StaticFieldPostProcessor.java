package org.molgenis.emx2.fairmapper.postprocessing;

import org.molgenis.emx2.io.tablestore.TableStore;

/** Adds the field and value to all rows of given table of provided TableStores */
public class StaticFieldPostProcessor implements PostProcessor {

  private final String table;
  private final String field;
  private final Object value;

  public StaticFieldPostProcessor(String table, String field, Object value) {
    this.table = table;
    this.field = field;
    this.value = value;
  }

  @Override
  public void process(TableStore tableStore) {
    tableStore.processTable(
        table, (iterator, source) -> iterator.forEachRemaining(row -> row.set(field, value)));
  }
}
