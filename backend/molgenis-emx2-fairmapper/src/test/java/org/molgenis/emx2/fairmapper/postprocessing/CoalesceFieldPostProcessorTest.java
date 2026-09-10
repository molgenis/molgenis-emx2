package org.molgenis.emx2.fairmapper.postprocessing;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.molgenis.emx2.datamodels.util.CompareTools.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;

class CoalesceFieldPostProcessorTest {

  private static final String TABLE_NAME = "order";

  @Test
  void givenSingleField_whenMatching_thenDeriveFromSingleField() {
    InMemoryTableStore tableStore =
        getTableStoreWithRows(new Row("name", "keyboard", "barcode", 1234));

    CoalesceFieldPostProcessor processor =
        new CoalesceFieldPostProcessor(TABLE_NAME, "id", false, "name");
    processor.process(tableStore);

    assertTableStoreHasRows(
        tableStore,
        new Row(
            "id", "keyboard",
            "name", "keyboard",
            "barcode", 1234));
  }

  @Test
  void givenSingleField_whenNotMatchingAndStrict_thenThrowException() {
    InMemoryTableStore tableStore =
        getTableStoreWithRows(new Row("name", "keyboard", "barcode", 1234));

    CoalesceFieldPostProcessor processor =
        new CoalesceFieldPostProcessor(TABLE_NAME, "id", true, "non-existent");

    assertThrows(MolgenisException.class, () -> processor.process(tableStore));
  }

  @Test
  void givenSingleField_whenNotMatchingAndNotStrict_thenUseNull() {
    InMemoryTableStore tableStore =
        getTableStoreWithRows(new Row("name", "keyboard", "barcode", 1234));

    CoalesceFieldPostProcessor processor =
        new CoalesceFieldPostProcessor(TABLE_NAME, "id", false, "non-existent");

    processor.process(tableStore);
    assertTableStoreHasRows(tableStore, new Row("id", null, "name", "keyboard", "barcode", 1234));
  }

  @Test
  void givenFields_whenMultipleMatching_thenDeriveFromFirstOnly() {
    InMemoryTableStore tableStore =
        getTableStoreWithRows(new Row("name", "keyboard", "barcode", 1234));

    CoalesceFieldPostProcessor processor =
        new CoalesceFieldPostProcessor(TABLE_NAME, "id", false, "name", "barcode");
    processor.process(tableStore);

    assertTableStoreHasRows(
        tableStore,
        new Row(
            "id", "keyboard",
            "name", "keyboard",
            "barcode", 1234));
  }

  @Test
  void givenFields_whenLastMatching_thenDeriveFromLast() {
    InMemoryTableStore tableStore =
        getTableStoreWithRows(new Row("name", "keyboard", "barcode", 1234));

    CoalesceFieldPostProcessor processor =
        new CoalesceFieldPostProcessor(TABLE_NAME, "id", false, "non-existent", "name");
    processor.process(tableStore);

    assertTableStoreHasRows(
        tableStore,
        new Row(
            "id", "keyboard",
            "name", "keyboard",
            "barcode", 1234));
  }

  @Test
  void givenNoDeriveFields_whenProcessed_thenThrowException() {
    assertThrows(
        MolgenisException.class, () -> new CoalesceFieldPostProcessor(TABLE_NAME, "id", true));
  }

  @Test
  void givenFields_whenTableHasMultipleRows_thenDeriveIndependentlyPerRow() {
    InMemoryTableStore tableStore =
        getTableStoreWithRows(
            new Row("name", "keyboard", "barcode", 1234),
            new Row("name", "mouse", "barcode", 6789));

    CoalesceFieldPostProcessor processor =
        new CoalesceFieldPostProcessor(TABLE_NAME, "id", false, "name");
    processor.process(tableStore);

    assertTableStoreHasRows(
        tableStore,
        new Row(
            "id", "keyboard",
            "name", "keyboard",
            "barcode", 1234),
        new Row(
            "id", "mouse",
            "name", "mouse",
            "barcode", 6789));
  }

  @Test
  void givenFields_whenTableEmpty_thenNoOp() {
    InMemoryTableStore tableStore = getTableStoreWithRows();

    CoalesceFieldPostProcessor processor =
        new CoalesceFieldPostProcessor(TABLE_NAME, "id", false, "name");
    processor.process(tableStore);

    assertTableStoreHasRows(tableStore);
  }

  @Test
  void givenFields_whenTargetFieldAlreadyHasValue_thenOverwriteWithDerivedValue() {
    InMemoryTableStore tableStore =
        getTableStoreWithRows(new Row("id", "some-id", "name", "keyboard", "barcode", 1234));

    CoalesceFieldPostProcessor processor =
        new CoalesceFieldPostProcessor(TABLE_NAME, "id", false, "name", "barcode");
    processor.process(tableStore);

    assertTableStoreHasRows(
        tableStore,
        new Row(
            "id", "keyboard",
            "name", "keyboard",
            "barcode", 1234));
  }

  private InMemoryTableStore getTableStoreWithRows(Row... rows) {
    InMemoryTableStore store = new InMemoryTableStore();
    if (rows.length == 0) {
      store.writeTable(TABLE_NAME, List.of(), List.of());
    } else {
      store.writeTable(TABLE_NAME, new ArrayList<>(rows[0].getColumnNames()), Arrays.asList(rows));
    }

    return store;
  }

  private void assertTableStoreHasRows(InMemoryTableStore tableStore, Row... rows) {
    List<Row> actual =
        StreamSupport.stream(tableStore.readTable(TABLE_NAME).spliterator(), false).toList();
    List<Row> expected = Arrays.stream(rows).toList();
    assertEquals(actual, expected);
  }
}
