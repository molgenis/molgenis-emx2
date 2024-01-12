package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

public class TestAddColumnUpdateSearchTrigger {
  private static Database db;
  private static Table table;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    Schema schema = db.dropCreateSchema(TestAddColumnUpdateSearchTrigger.class.getSimpleName());
    table =
        schema.create(
            table("TestAddColUpdateSearchTrigger")
                .add(column("col1").setPkey())
                .add(column("col2").setType(STRING)));

    table.insert(new Row().setString("col1", "key1").setString("col2", "aaa"));
  }

  @Test
  public void testAddedColumnGetsSearchIndexed() {

    assertEquals(1, table.query().search("aaa").retrieveRows().size());

    // add new col
    table.getMetadata().add(column("col3").setType(STRING));

    // add new row with data in new col
    table.insert(new Row().setString("col1", "key2").setString("col3", "bbb"));

    // search for data in new col, expect the new row to be found
    List<Row> searchResult = table.query().search("bbb").retrieveRows();
    assertEquals(1, searchResult.size());
    assertEquals("key2", searchResult.get(0).getString("col1"));
  }
}
