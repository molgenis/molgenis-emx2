package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Query.Option;
import org.molgenis.emx2.datamodels.util.CompareTools;

class TestDeletingTableRows {

  private static Table table;

  @BeforeEach
  void setup() {
    Database db = TestDatabaseFactory.getTestDatabase();
    Schema schema = db.dropCreateSchema(TestDeletingTableRows.class.getSimpleName());
    table =
        schema.create(
            TableMetadata.table(
                "to_delete_rows",
                Column.column("id", ColumnType.INT).setPkey(),
                Column.column("name", ColumnType.STRING)));
  }

  @Test
  void whenDeletingRowsWithPrimaryKey_thenDeleteGivenRows() {
    table.insert(Row.row("id", 1, "name", "foo"));
    table.insert(Row.row("id", 2, "name", "bar"));
    table.insert(Row.row("id", 3, "name", "baz"));

    int nrDeleted = table.delete(Row.row("id", 1), Row.row("id", 2));
    assertEquals(2, nrDeleted);

    CompareTools.assertEquals(
        table.retrieveRows(Option.EXCLUDE_MG_COLUMNS), List.of(Row.row("id", 3, "name", "baz")));
  }

  @Test
  void givenNonExistingRows_whenDeletingWithStrict_thenThrowException() {
    table.insert(Row.row("id", 1, "name", "a"));
    table.insert(Row.row("id", 2, "name", "b"));

    Row nonExistentRow = Row.row("id", 123);
    assertThrows(
        MolgenisException.class,
        () -> table.delete(List.of(nonExistentRow), true),
        "Should throw exception when deleting a row that doesn't exist in the table");

    // Verify transaction is rolled back
    CompareTools.assertEquals(
        table.retrieveRows(Option.EXCLUDE_MG_COLUMNS),
        List.of(Row.row("id", 1, "name", "a"), Row.row("id", 2, "name", "b")));
  }

  @Test
  void givenNonExistingRows_whenDeletingWithoutStrict_thenDoNothing() {
    table.insert(Row.row("id", 1, "name", "a"));
    table.insert(Row.row("id", 2, "name", "b"));

    Row nonExistentRow = Row.row("id", 123);
    assertEquals(
        0,
        table.delete(nonExistentRow),
        "Should ignore when deleting a row that doesn't exist in the table");

    // Verify transaction is rolled back
    CompareTools.assertEquals(
        table.retrieveRows(Option.EXCLUDE_MG_COLUMNS),
        List.of(Row.row("id", 1, "name", "a"), Row.row("id", 2, "name", "b")));
  }

  @Test
  void whenDeletingRowsWithAllFields_thenDelete() {
    table.insert(Row.row("id", 1, "name", "foo"));
    table.insert(Row.row("id", 2, "name", "bar"));

    int nrDeleted = table.delete(Row.row("id", 2, "name", "bar"));
    assertEquals(1, nrDeleted);

    CompareTools.assertEquals(
        table.retrieveRows(Option.EXCLUDE_MG_COLUMNS), List.of(Row.row("id", 1, "name", "foo")));
  }

  @Test
  void whenDeletingMatchingPrimaryKeyWithUnknownColumn_thenDeleteRow() {
    table.insert(Row.row("id", 1, "name", "foo"));
    table.insert(Row.row("id", 2, "name", "bar"));

    int nrDeleted = table.delete(Row.row("id", 1, "unknown-field", "foo"));
    assertEquals(1, nrDeleted);

    CompareTools.assertEquals(
        table.retrieveRows(Option.EXCLUDE_MG_COLUMNS), List.of(Row.row("id", 2, "name", "bar")));
  }

  @Test
  void whenDeletingRowNotInTable_thenThrowException() {
    table.insert(Row.row("id", 1, "name", "test"));

    Row nonExistentRow = Row.row("id", 999, "name", "non-existent");

    assertThrows(
        MolgenisException.class,
        () -> table.delete(List.of(nonExistentRow), true),
        "Should throw exception when deleting a row that doesn't exist in the table");
  }

  @Test
  void givenRowsWithMixedValidAndInvalidRows_whenStrict_thenThrowException() {
    table.insert(Row.row("id", 1, "name", "test1"));
    table.insert(Row.row("id", 2, "name", "test2"));

    Row validRow = Row.row("id", 1, "name", "test1");
    Row invalidRow = Row.row("id", 999, "name", "non-existent");

    assertEquals(
        1,
        table.delete(validRow, invalidRow),
        "Should ignore when deleting a row that doesn't exist in the table");

    // Verify transaction is rolled back
    CompareTools.assertEquals(
        table.retrieveRows(Option.EXCLUDE_MG_COLUMNS), List.of(Row.row("id", 2, "name", "test2")));
  }

  @Test
  void givenRowsWithMixedValidAndInvalidRows_whenNotStrict_thenDeleteExisting() {
    table.insert(Row.row("id", 1, "name", "test1"));
    table.insert(Row.row("id", 2, "name", "test2"));

    Row validRow = Row.row("id", 1, "name", "test1");
    Row invalidRow = Row.row("id", 999, "name", "non-existent");

    assertThrows(
        MolgenisException.class,
        () -> table.delete(List.of(validRow, invalidRow), true),
        "Should throw exception when any row in the batch doesn't exist in the table");

    // Verify transaction is rolled back
    CompareTools.assertEquals(
        table.retrieveRows(Option.EXCLUDE_MG_COLUMNS),
        List.of(Row.row("id", 1, "name", "test1"), Row.row("id", 2, "name", "test2")));
  }
}
