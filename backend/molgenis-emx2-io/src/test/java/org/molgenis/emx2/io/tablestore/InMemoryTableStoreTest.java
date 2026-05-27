package org.molgenis.emx2.io.tablestore;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;

class InMemoryTableStoreTest {

  private InMemoryTableStore store;

  @BeforeEach
  void setUp() {
    store = new InMemoryTableStore();
  }

  @Test
  void givenRows_whenReadingTable_thenReturnRows() {
    store.writeTable(
        "Product",
        List.of("name", "price"),
        List.of(Row.row("name", "Bread", "price", 1), Row.row("name", "Butter", "price", 3)));

    Iterator<Row> rows = store.readTable("Product").iterator();
    Row bread = rows.next();
    Row butter = rows.next();

    assertEquals("Bread", bread.getString("name"));
    assertEquals("Butter", butter.getString("name"));
    assertFalse(rows.hasNext());
  }

  @Test
  void givenTable_whenReadingMultipleTimes_thenSucceeds() {
    store.writeTable("Product", List.of("name"), List.of(Row.row("name", "Bread")));

    String firstRead = store.readTable("Product").iterator().next().getString("name");
    String secondRead = store.readTable("Product").iterator().next().getString("name");

    assertEquals("Bread", firstRead);
    assertEquals("Bread", secondRead);
  }

  @Test
  void givenRowsMissingColumn_whenWritingTable_thenFillWithNull() {
    store.writeTable(
        "Product",
        List.of("name", "price", "description"),
        List.of(Row.row("name", "Bread", "price", 1)));

    Row row = store.readTable("Product").iterator().next();

    assertEquals("Bread", row.getString("name"));
    assertEquals(1, row.getInteger("price"));
    assertNull(row.getString("description"));
  }

  @Test
  void givenRowsWithExtraColumns_whenWritingTable_thenDropExtraColumns() {
    store.writeTable("Product", List.of("name"), List.of(Row.row("name", "Bread", "price", 1)));

    Row row = store.readTable("Product").iterator().next();

    assertEquals("Bread", row.getString("name"));
    assertFalse(row.getColumnNames().contains("price"));
  }

  @Test
  void givenExistingTable_whenWritingAgain_thenReplaceRows() {
    store.writeTable("Product", List.of("name"), List.of(Row.row("name", "Bread")));
    store.writeTable(
        "Product", List.of("name"), List.of(Row.row("name", "Butter"), Row.row("name", "Milk")));

    Iterator<Row> rows = store.readTable("Product").iterator();
    Row butter = rows.next();
    Row milk = rows.next();

    assertEquals("Butter", butter.getString("name"));
    assertEquals("Milk", milk.getString("name"));
    assertFalse(rows.hasNext());
  }

  @Test
  void givenExistingTable_thenContainsTable() {
    store.writeTable("Product", List.of("name"), List.of(Row.row("name", "Bread")));
    assertTrue(store.containsTable("Product"));
  }

  @Test
  void givenNonExistingTable_thenDoesNotContainTable() {
    assertFalse(store.containsTable("NonExistent"));
  }

  @Test
  void givenMultipleTables_whenGettingTableNames_thenReturnAll() {
    store.writeTable("Product", List.of("name"), List.of(Row.row("name", "Bread")));
    store.writeTable("Order", List.of("id"), List.of(Row.row("id", 1)));
    assertEquals(Set.of("Product", "Order"), Set.copyOf(store.getTableNames()));
  }

  @Test
  void givenIntegerValue_whenWritingTable_thenPreserveType() {
    store.writeTable("Product", List.of("price"), List.of(Row.row("price", 42)));

    Object stored = store.readTable("Product").iterator().next().getValueMap().get("price");

    assertInstanceOf(Integer.class, stored);
    assertEquals(42, stored);
  }

  @Test
  void givenBooleanValue_whenWritingTable_thenPreserveType() {
    store.writeTable("Product", List.of("available"), List.of(Row.row("available", true)));

    Object stored = store.readTable("Product").iterator().next().getValueMap().get("available");

    assertInstanceOf(Boolean.class, stored);
    assertEquals(true, stored);
  }
}
