package org.molgenis.emx2.io;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestRenameAndDelete {

  static Database db;
  static Schema schema;

  @BeforeClass
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestRenameAndDelete.class.getSimpleName());
  }

  @Test
  public void testColumnRename() {

    // import simple model
    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory();
    store.writeTable(
        "molgenis",
        List.of("tableName", "columnName", "key"),
        List.of(
            new Row("tableName", "myTable"),
            new Row("tableName", "myTable", "columnName", "a", "key", "1")));
    ImportMetadataTask imt = new ImportMetadataTask(schema, store, true);
    imt.run();

    assertNotNull(schema.getTable("myTable"));
    assertNotNull(schema.getTable("myTable").getMetadata().getColumn("a"));

    // rename some tables and columns
    store = new TableStoreForCsvInMemory();
    store.writeTable(
        "molgenis",
        List.of("tableName", "columnName", "oldName"),
        List.of(
            new Row("tableName", "otherTable", "oldName", "myTable"),
            new Row("tableName", "otherTable", "columnName", "b", "oldName", "a")));
    imt = new ImportMetadataTask(schema, store, true);
    imt.run();

    // check schema for the changes
    schema = db.getSchema(TestRenameAndDelete.class.getSimpleName());
    assertNull(schema.getTable("myTable"));
    assertNotNull(schema.getTable("otherTable"));
    assertNull(schema.getTable("otherTable").getMetadata().getColumn("a"));
    assertNotNull(schema.getTable("otherTable").getMetadata().getColumn("b"));

    // drop column
    store = new TableStoreForCsvInMemory();
    store.writeTable(
        "molgenis",
        List.of("tableName", "columnName", "drop"),
        List.of(new Row("tableName", "otherTable", "columnName", "b", "drop", "true")));
    imt = new ImportMetadataTask(schema, store, true);
    imt.run();

    assertNull(schema.getTable("otherTable").getMetadata().getColumn("b"));

    // drop table
    store = new TableStoreForCsvInMemory();
    store.writeTable(
        "molgenis",
        List.of("tableName", "drop"),
        List.of(new Row("tableName", "otherTable", "drop", "true")));
    imt = new ImportMetadataTask(schema, store, true);
    imt.run();

    assertNull(schema.getTable("otherTable"));
  }
}
