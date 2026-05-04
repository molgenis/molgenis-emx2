package org.molgenis.emx2.io.tablestore.processor;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
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

  private List<Map<String, Object>> importRows(Row... rows) {
    Task task = new Task();
    ImportRowProcessor processor = new ImportRowProcessor(table, task);
    processor.process(List.of(rows).iterator(), new TableStoreForCsvInMemory());
    return table.retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS).stream()
        .map(Row::getValueMap)
        .toList();
  }
}
