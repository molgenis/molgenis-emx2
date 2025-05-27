package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
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
  public void testWarningDelete() {
    ClassLoader classLoader = getClass().getClassLoader();
    Path path = new File(classLoader.getResource("TestImportTableDelete").getFile()).toPath();
    ImportDirectoryTask t = new ImportDirectoryTask(path, schema, false);
    t.run();
  }
}
