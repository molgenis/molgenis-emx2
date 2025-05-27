package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestImportTableTask {

  private static Schema schema;

  @BeforeAll
  public static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestImportTableTask.class.getSimpleName());
  }

  @Test
  public void testWarningMissingColumns() {
    ClassLoader classLoader = getClass().getClassLoader();
    Path path = new File(classLoader.getResource("TestImportTableTask").getFile()).toPath();
    ImportDirectoryTask t = new ImportDirectoryTask(path, schema, false);
    t.run();

    assertTrue(
        t.getSubTasks()
            .get(1)
            .getSubTasks()
            .get(0)
            .getSubTasks()
            .get(0)
            .getDescription()
            .contains("colerror"));

    try {
      t = new ImportDirectoryTask(path, schema, true);
      t.run();
      fail("should have failed on colerror because strict");
    } catch (Exception e) {
      // should happen
      assertTrue(e.getMessage().contains("colerror"));
    }
  }

  @Test
  public void testWarningMissingKeys() {
    ClassLoader classLoader = getClass().getClassLoader();
    Path path = new File(classLoader.getResource("TestImportTableMissingKey").getFile()).toPath();
    try {
      new ImportDirectoryTask(path, schema, true).run();
      fail("should fail on missing keys");
    } catch (Exception e) {
      assertEquals(
          "Transaction failed: Missing keys found in table 'test': missing value for key column 'col1'. Row: ROW(col2='b')",
          e.getMessage());
    }
  }

  @Test
  public void testDeleteFromImportCSV() {
    ClassLoader classLoader = getClass().getClassLoader();
    Path path = new File(classLoader.getResource("TestImportTableDelete").getFile()).toPath();

    PET_STORE.getImportTask(schema, true).run();
    List<Row> rows = schema.retrieveSql("Select * from \"Pet\"");
    assertEquals(8, rows.size());

    // Insert one row
    Path insertPath = path.resolve("insert");
    ImportDirectoryTask insertTask = new ImportDirectoryTask(insertPath, schema, false);
    insertTask.run();

    rows = schema.retrieveSql("Select * from \"Pet\"");
    assertEquals(9, rows.size());

    // Delete one row
    Path deletePath = path.resolve("delete");
    ImportDirectoryTask deleteTask = new ImportDirectoryTask(deletePath, schema, false);
    deleteTask.run();

    rows = schema.retrieveSql("Select * from \"Pet\"");
    assertEquals(8, rows.size());
  }

  @Test
  public void testErrorDeletingFromImportCSV() {
    ClassLoader classLoader = getClass().getClassLoader();
    Path path =
        new File(classLoader.getResource("TestImportTableDelete/DeleteWithError").getFile())
            .toPath();
    PET_STORE.getImportTask(schema, true).run();
    ImportDirectoryTask t = new ImportDirectoryTask(path, schema, false);
    try {
      t.run();
      fail("should have failed on reference");
    } catch (Exception e) {
      assertEquals(
          "Transaction failed: update or delete on table \"Category\" violates foreign key constraint \"Pet.category REFERENCES Category\" on table \"Pet\". Details: Key (name)=(cat) is still referenced from table \"Pet\".",
          e.getMessage());
    }
  }
}
