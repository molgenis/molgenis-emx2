package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.List;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvFile;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestRenameAndDelete {

  static Database db;
  static Schema schema;

  @BeforeAll
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestRenameAndDelete.class.getSimpleName());
  }

  @Test
  public void testRenameAndDrop() {

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
        List.of("tableName", "columnName", "oldName", "key"),
        List.of(
            new Row("tableName", "otherTable", "oldName", "myTable"),
            new Row("tableName", "otherTable", "columnName", "b", "oldName", "a", "key", "1")));
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

  @Test
  public void testRenameWhenRefs() throws IOException {
    schema = db.dropCreateSchema(TestRenameAndDelete.class.getSimpleName());

    new PetStoreLoader().load(schema, false);
    assertNotNull(schema.getTable("Category"));

    // now we gonna rename a table which has a ref from Category to Type
    SchemaMetadata newSchema =
        Emx2.fromRowList(
            new TableStoreForCsvFile(
                    Resource.newClassPathResource("testRenameWhenRefs.csv").getFile().toPath())
                .readTable("molgenis"));

    schema.migrate(newSchema);

    assertNull(schema.getTable("Category"));
    assertNotNull(schema.getTable("Type"));
  }
}
