package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestImportTableTask {

  private static Database database;
  private static Schema schema;
  private static final String schemaName = "TestImportTableTask";

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema("TestImportTableTask");
  }

  @Test
  void testWarningMissingColumns() {
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
  void testWarningMissingKeys() {
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
  void testDeleteFromImportCSV() {
    ClassLoader classLoader = getClass().getClassLoader();
    Path path = new File(classLoader.getResource("TestImportTableDelete").getFile()).toPath();

    database.dropSchemaIfExists(schemaName);
    PET_STORE.getImportTask(database, schemaName, "", true).run();
    schema = database.getSchema(schemaName);
    List<Row> rows = schema.getTable("Pet").retrieveRows();
    assertEquals(9, rows.size());

    // Insert one row
    Path insertPath = path.resolve("insert");
    ImportDirectoryTask insertTask = new ImportDirectoryTask(insertPath, schema, false);
    insertTask.run();

    rows = schema.getTable("Pet").retrieveRows();
    assertEquals(10, rows.size());

    // Delete one row
    Path deletePath = path.resolve("delete");
    ImportDirectoryTask deleteTask = new ImportDirectoryTask(deletePath, schema, false);
    deleteTask.run();

    rows = schema.getTable("Pet").retrieveRows();
    assertEquals(9, rows.size());
  }

  @Test
  void testErrorDeletingFromImportCSV() {
    ClassLoader classLoader = getClass().getClassLoader();
    Path path =
        new File(classLoader.getResource("TestImportTableDelete/DeleteWithError").getFile())
            .toPath();
    database.dropSchemaIfExists(schemaName);
    PET_STORE.getImportTask(database, schemaName, "", true).run();
    ImportDirectoryTask t = new ImportDirectoryTask(path, schema, false);
    assertThrows(MolgenisException.class, t::run, "should have failed on reference deletion");
  }
}
