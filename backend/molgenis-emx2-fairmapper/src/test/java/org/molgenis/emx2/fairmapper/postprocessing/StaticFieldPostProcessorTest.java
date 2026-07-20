package org.molgenis.emx2.fairmapper.postprocessing;

import static org.molgenis.emx2.datamodels.util.CompareTools.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.io.tablestore.TableStore;

class StaticFieldPostProcessorTest {

  private static final String TABLE_NAME = "order";

  @Test
  void givenField_whenExisting_thenOverride() {
    TableStore tableStore =
        getTableStoreWithRows(new Row("id", "temp", "name", "keyboard", "barcode", 1234));

    StaticFieldPostProcessor processor =
        new StaticFieldPostProcessor(TABLE_NAME, "id", "static-id");
    processor.process(tableStore);

    assertTableStoreHasRows(
        tableStore,
        new Row(
            "id", "static-id",
            "name", "keyboard",
            "barcode", 1234));
  }

  @Test
  void givenField_whenNotExisting_thenCreate() {
    TableStore tableStore = getTableStoreWithRows(new Row("name", "keyboard", "barcode", 1234));

    StaticFieldPostProcessor processor =
        new StaticFieldPostProcessor(TABLE_NAME, "id", "static-id");
    processor.process(tableStore);

    assertTableStoreHasRows(
        tableStore, new Row("id", "static-id", "name", "keyboard", "barcode", 1234));
  }

  @Test
  void givenField_whenTableEmpty_thenNoOp() {
    TableStore tableStore = getTableStoreWithRows();

    StaticFieldPostProcessor processor = new StaticFieldPostProcessor(TABLE_NAME, "id", "name");
    processor.process(tableStore);

    assertTableStoreHasRows(tableStore);
  }

  @Test
  void givenFields_whenTableHasMultipleRows_thenHandleIndependentlyPerRow() {
    TableStore tableStore =
        getTableStoreWithRows(
            new Row("name", "keyboard", "barcode", 1234),
            new Row("name", "mouse", "barcode", 6789));

    StaticFieldPostProcessor processor =
        new StaticFieldPostProcessor(TABLE_NAME, "id", "static-id");
    processor.process(tableStore);

    assertTableStoreHasRows(
        tableStore,
        new Row("id", "static-id", "name", "keyboard", "barcode", 1234),
        new Row("id", "static-id", "name", "mouse", "barcode", 6789));
  }

  private TableStore getTableStoreWithRows(Row... rows) {
    InMemoryTableStore store = new InMemoryTableStore();
    if (rows.length == 0) {
      store.writeTable(TABLE_NAME, List.of(), List.of());
    } else {
      store.writeTable(TABLE_NAME, new ArrayList<>(rows[0].getColumnNames()), Arrays.asList(rows));
    }

    return store;
  }

  private void assertTableStoreHasRows(TableStore tableStore, Row... rows) {
    List<Row> actual =
        StreamSupport.stream(tableStore.readTable(TABLE_NAME).spliterator(), false).toList();
    List<Row> expected = Arrays.stream(rows).toList();
    assertEquals(actual, expected);
  }
}
