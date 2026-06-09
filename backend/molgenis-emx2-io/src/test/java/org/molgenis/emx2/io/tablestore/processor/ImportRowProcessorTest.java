package org.molgenis.emx2.io.tablestore.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.MG_DELETE;
import static org.molgenis.emx2.Query.Option.INCLUDE_FILE_CONTENTS;
import static org.molgenis.emx2.Row.row;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.RawInMemoryTableAndFileStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.Task;

class ImportRowProcessorTest {

  private static final String SCHEMA_NAME = ImportRowProcessorTest.class.getSimpleName();
  private static Database database;
  private Table table;

  @BeforeAll
  static void setupDatabase() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @BeforeEach
  void setUp() {
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);
    TableMetadata metadata =
        TableMetadata.table("Person", column("name").setType(ColumnType.STRING).setPkey());
    schema.create(metadata);
    table = schema.getTable("Person");
  }

  @AfterAll
  static void tearDownDatabase() {
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void givenRows_thenImportToSpecifiedTable() {
    List<Map<String, Object>> actual = importRows(row("name", "Lewis"), row("name", "Marnie"));
    List<Map<String, String>> expected = List.of(Map.of("name", "Lewis"), Map.of("name", "Marnie"));
    assertEquals(expected, actual);
  }

  @Test
  void givenEmptyRows_thenSkip() {
    List<Map<String, Object>> actual =
        importRows(row("name", "Lewis"), row(), row("name", null), row("name", "Marnie"));
    List<Map<String, String>> expected = List.of(Map.of("name", "Lewis"), Map.of("name", "Marnie"));
    assertEquals(expected, actual);
  }

  @Test
  void givenRow_whenNotDropping_thenAddFileAttachmentsToRow() {
    table.getMetadata().add(column("passport").setType(ColumnType.FILE));
    List<Row> rows = List.of(row("name", "Lewis", "passport", "passport_example.txt"));

    Task task = new Task();

    RawInMemoryTableAndFileStore store = new RawInMemoryTableAndFileStore();
    store.writeFile("passport_example.txt", "Hello world".getBytes());
    ImportRowProcessor processor = new ImportRowProcessor(table, task);
    processor.process(rows.iterator(), store);

    List<Map<String, Object>> list =
        table.retrieveRows(INCLUDE_FILE_CONTENTS).stream().map(Row::getValueMap).toList();
    assertEquals(1, list.size());
    Map<String, Object> uploadedRow = list.getFirst();

    assertEquals("Lewis", uploadedRow.get("name"));
    assertEquals("passport_example.txt", uploadedRow.get("passport_filename"));
    assertEquals("txt", uploadedRow.get("passport_extension"));
    assertEquals("bytes", uploadedRow.get("passport_mimetype"));
    assertEquals(11, uploadedRow.get("passport_size"));
    assertEquals("Hello world", new String((byte[]) uploadedRow.get("passport_contents")));
    Pattern hexPattern = Pattern.compile("\\p{XDigit}{32}");
    assertTrue(hexPattern.matcher(uploadedRow.get("passport").toString()).find());
  }

  @Test
  void givenRow_whenDropping_skipAddFileAttachmentsToRow() {
    table.getMetadata().add(column("passport").setType(ColumnType.FILE));
    List<Row> insertRows = List.of(row("name", "Lewis", "passport", "passport_example.txt"));

    Task task = new Task();

    RawInMemoryTableAndFileStore store = new RawInMemoryTableAndFileStore();
    store.writeFile("passport_example.txt", "Hello world".getBytes());
    ImportRowProcessor processor = new ImportRowProcessor(table, task);
    processor.process(insertRows.iterator(), store);

    List<Map<String, Object>> list = table.retrieveRows().stream().map(Row::getValueMap).toList();
    assertEquals(1, list.size());

    List<Row> deleteRows =
        List.of(row("name", "Lewis", "passport", "passport_example.txt", MG_DELETE, "true"));
    task = new Task();
    // test that row with file is deleted without supplying the files in a store
    store = new RawInMemoryTableAndFileStore();
    processor = new ImportRowProcessor(table, task);
    processor.process(deleteRows.iterator(), store);

    list = table.retrieveRows().stream().map(Row::getValueMap).toList();
    assertTrue(list.isEmpty());
  }

  private List<Map<String, Object>> importRows(Row... rows) {
    Task task = new Task();
    ImportRowProcessor processor = new ImportRowProcessor(table, task);
    processor.process(List.of(rows).iterator(), new TableStoreForCsvInMemory());
    return table.retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS).stream()
        .map(Row::getValueMap)
        .toList();
  }
}
