package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TestImportTableTask {

  private static Database database;
  private static Schema schema;
  private static final String SCHEMA_NAME = TestImportTableTask.class.getSimpleName();

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(SCHEMA_NAME);
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

    database.dropSchemaIfExists(SCHEMA_NAME);
    PET_STORE.getImportTask(database, SCHEMA_NAME, "", true).run();
    schema = database.getSchema(SCHEMA_NAME);
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
    database.dropSchemaIfExists(SCHEMA_NAME);
    PET_STORE.getImportTask(database, SCHEMA_NAME, "", true).run();
    ImportDirectoryTask t = new ImportDirectoryTask(path, schema, false);
    assertThrows(MolgenisException.class, t::run, "should have failed on reference deletion");
  }

  @Test
  void givenStoreWithEmptyRows_whenNotStrict_thenSkip() throws IOException {
    Path path =
        Path.of(
            Objects.requireNonNull(getClass().getClassLoader().getResource("TestImportTableTask"))
                .getPath());
    String csv = Files.readString(path.resolve("empty-rows.csv"));
    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory(',');
    store.setCsvString("Person", csv);

    database.dropCreateSchema(SCHEMA_NAME);
    schema = database.getSchema(SCHEMA_NAME);
    Table table =
        schema.create(
            TableMetadata.table(
                "Person",
                Column.column("name", ColumnType.STRING).setPkey(),
                Column.column("age", ColumnType.INT)));

    new ImportTableTask(store, table, false).run();

    List<Map<String, Object>> values =
        table.retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS).stream().map(Row::getValueMap).toList();
    List<Map<String, Object>> expected =
        List.of(Map.of("name", "john", "age", 42), Map.of("name", "doe", "age", 24));
    assertEquals(expected, values);
  }
}
