package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.datamodels.util.CompareTools;

class StreamingTableStoreTest {

  StreamingTableStore tableStore;

  @BeforeEach
  void setUp() {
    tableStore = new StreamingTableStore();
  }

  @Test
  void givenRows_whenWritingTable_thenWriteColumnNames() {
    tableStore.writeTable(
        "Product",
        List.of("name", "price"),
        List.of(Row.row("name", "Bread", "price", 1), Row.row("name", "Butter", "price", 3)));

    Iterator<Row> rows = tableStore.readTable("Product").iterator();
    CompareTools.assertEquals(rows.next(), Row.row("name", "Bread", "price", 1));
    CompareTools.assertEquals(rows.next(), Row.row("name", "Butter", "price", 3));
    assertFalse(rows.hasNext());
  }

  @Test
  void givenRowsWithoutSpecifiedColumn_whenWritingTable_thenAddNullValue() {
    tableStore.writeTable(
        "Product",
        List.of("name", "price", "description"),
        List.of(Row.row("name", "Bread", "price", 1)));

    Iterator<Row> rows = tableStore.readTable("Product").iterator();
    assertEquals(
        rows.next().getValueMap(),
        Row.row("name", "Bread", "price", 1, "description", null).getValueMap());
    assertFalse(rows.hasNext());
  }

  @Test
  void givenRowsWithExtraColumns_whenWritingTable_thenDropExtraColumns() {
    tableStore.writeTable(
        "Product", List.of("name"), List.of(Row.row("name", "Bread", "price", 1)));

    Iterator<Row> rows = tableStore.readTable("Product").iterator();
    CompareTools.assertEquals(rows.next(), Row.row("name", "Bread"));
    assertFalse(rows.hasNext());
  }

  @Test
  void givenRows_whenWritingAlreadyExistingTable_thenOverride() {
    tableStore.writeTable(
        "Product", List.of("name"), List.of(Row.row("name", "Bread", "price", 1)));

    tableStore.writeTable(
        "Product",
        List.of("name", "price"),
        List.of(Row.row("name", "Yoghurt", "price", 2), Row.row("name", "Butter", "price", 3)));

    Iterator<Row> rows = tableStore.readTable("Product").iterator();
    CompareTools.assertEquals(rows.next(), Row.row("name", "Yoghurt", "price", 2));
    CompareTools.assertEquals(rows.next(), Row.row("name", "Butter", "price", 3));
    assertFalse(rows.hasNext());
  }

  @Test
  void givenExistingTable_thenContainsTable() {
    tableStore.writeTable("Product", List.of("name"), List.of(Row.row("name", "Bread")));
    assertTrue(tableStore.containsTable("Product"));
  }

  @Test
  void givenNonExistingTable_thenDoesNotContainTable() {
    tableStore.writeTable("Product", List.of("name"), List.of(Row.row("name", "Bread")));
    assertFalse(tableStore.containsTable("non-existent"));
  }

  @Test
  void givenTables_thenReturnTableNames() {
    tableStore.writeTable("Product", List.of("name"), List.of(Row.row("name", "Bread")));
    tableStore.writeTable("Order", List.of("orderId"), List.of(Row.row("orderId", 123)));
    assertEquals(tableStore.getTableNames(), Set.of("Product", "Order"));
  }

  @Test
  void shouldBeAbleToReadTableTwice() {
    tableStore.writeTable("Product", List.of("name"), List.of(Row.row("name", "Bread")));

    List<Row> firstRead =
        StreamSupport.stream(tableStore.readTable("Product").spliterator(), false)
            .collect(Collectors.toCollection(ArrayList::new));
    CompareTools.assertEquals(firstRead, List.of(Row.row("name", "Bread")));

    assertThrows(IllegalStateException.class, () -> tableStore.readTable("Product").spliterator());

    firstRead.add(Row.row("name", "Pie"));
    tableStore.writeTable(
        "Product", new ArrayList<>(firstRead.getFirst().getColumnNames()), firstRead.stream());

    List<Row> secondRead =
        StreamSupport.stream(tableStore.readTable("Product").spliterator(), false).toList();
    CompareTools.assertEquals(
        secondRead, List.of(Row.row("name", "Bread"), Row.row("name", "Pie")));
  }
}
