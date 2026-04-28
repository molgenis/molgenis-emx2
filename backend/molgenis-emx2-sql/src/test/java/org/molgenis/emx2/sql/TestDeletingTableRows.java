package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
  void whenDeletingNonExistingRows_thenDontDelete() {
    table.insert(Row.row("id", 1, "name", "a"));
    table.insert(Row.row("id", 2, "name", "b"));

    int nrDeleted = table.delete(Row.row("id", 123));
    assertEquals(0, nrDeleted);

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
}
