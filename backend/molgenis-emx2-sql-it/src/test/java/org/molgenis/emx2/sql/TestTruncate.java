package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

public class TestTruncate {

  @Test
  public void testTruncate() {
    Database db = TestDatabaseFactory.getTestDatabase();
    Schema schema = db.dropCreateSchema(TestTruncate.class.getSimpleName());

    // create simple table, add data, and truncate
    Table table0 = schema.create(table("Table0", column("name").setPkey()));
    table0.insert(row("name", "a"));

    // native truncate fails on foreign key, so added test for that
    Table table1 =
        schema.create(
            table(
                "Table1",
                column("name").setPkey(),
                column("someFkey").setType(REF).setRefTable("Table0")));
    table1.insert(row("name", "a"));

    assertEquals(1, table1.retrieveRows().size());
    table1.truncate();
    assertEquals(0, table1.retrieveRows().size());

    // create with subclass
    Table table2 = schema.create(table("Table2").setInheritName("Table1").add(column("col1")));
    table1.insert(row("name", "a"));
    table2.insert(row("name", "b", "col1", "checkb"));
    assertEquals(2, table1.retrieveRows().size());
    assertEquals(1, table2.retrieveRows().size());

    table1.truncate();

    assertEquals(1, table1.retrieveRows().size());
    assertEquals(1, table2.retrieveRows().size());

    table2.truncate();

    assertEquals(0, table1.retrieveRows().size());
    assertEquals(0, table2.retrieveRows().size());

    // create with subclass of a subclass
    Table table3 = schema.create(table("Table3").setInheritName("Table2").add(column("col2")));
    table1.insert(row("name", "a"));
    table2.insert(row("name", "b", "col1", "checkb"));
    table3.insert(row("name", "c", "col1", "checkc", "col2", "checkc"));

    assertEquals(3, table1.retrieveRows().size());
    assertEquals(2, table2.retrieveRows().size());
    assertEquals(1, table3.retrieveRows().size());

    table2.truncate(); // leaves subclass?!!! is this expected behavior?

    assertEquals(2, table1.retrieveRows().size());
    assertEquals(1, table2.retrieveRows().size()); // !!!
    assertEquals(1, table3.retrieveRows().size());

    table3.truncate();

    assertEquals(1, table1.retrieveRows().size());
    assertEquals(0, table2.retrieveRows().size());
    assertEquals(0, table3.retrieveRows().size());

    table1.truncate();

    assertEquals(0, table1.retrieveRows().size());
    assertEquals(0, table2.retrieveRows().size());
    assertEquals(0, table3.retrieveRows().size());
  }
}
