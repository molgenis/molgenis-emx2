package org.molgenis.emx2.sql;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.Assert;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

public class TestTruncate {

  @Test
  public void testTruncate() {
    Database db = TestDatabaseFactory.getTestDatabase();
    Schema schema = db.dropCreateSchema(TestTruncate.class.getSimpleName());

    // create simple table, add data, and truncate
    Table table1 = schema.create(table("Table1", column("name").setPkey()));
    table1.insert(row("name", "a"));

    Assert.assertEquals(1, table1.retrieveRows().size());
    table1.truncate();
    Assert.assertEquals(0, table1.retrieveRows().size());

    // create with subclass
    Table table2 = schema.create(table("Table2").setInherit("Table1").add(column("col1")));
    table1.insert(row("name", "a"));
    table2.insert(row("name", "b", "col1", "checkb"));
    Assert.assertEquals(2, table1.retrieveRows().size());
    Assert.assertEquals(1, table2.retrieveRows().size());

    table1.truncate();

    Assert.assertEquals(1, table1.retrieveRows().size());
    Assert.assertEquals(1, table2.retrieveRows().size());

    table2.truncate();

    Assert.assertEquals(0, table1.retrieveRows().size());
    Assert.assertEquals(0, table2.retrieveRows().size());

    // create with subclass of a subclass
    Table table3 = schema.create(table("Table3").setInherit("Table2").add(column("col2")));
    table1.insert(row("name", "a"));
    table2.insert(row("name", "b", "col1", "checkb"));
    table3.insert(row("name", "c", "col1", "checkc", "col2", "checkc"));

    Assert.assertEquals(3, table1.retrieveRows().size());
    Assert.assertEquals(2, table2.retrieveRows().size());
    Assert.assertEquals(1, table3.retrieveRows().size());

    table2.truncate(); // leaves subclass?!!! is this expected behavior?

    Assert.assertEquals(2, table1.retrieveRows().size());
    Assert.assertEquals(1, table2.retrieveRows().size()); // !!!
    Assert.assertEquals(1, table3.retrieveRows().size());

    table3.truncate();

    Assert.assertEquals(1, table1.retrieveRows().size());
    Assert.assertEquals(0, table2.retrieveRows().size());
    Assert.assertEquals(0, table3.retrieveRows().size());

    table1.truncate();

    Assert.assertEquals(0, table1.retrieveRows().size());
    Assert.assertEquals(0, table2.retrieveRows().size());
    Assert.assertEquals(0, table3.retrieveRows().size());
  }
}
