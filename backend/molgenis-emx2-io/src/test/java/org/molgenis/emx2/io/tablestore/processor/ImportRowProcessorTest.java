package org.molgenis.emx2.io.tablestore.processor;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.*;

import java.util.List;
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
    List<String> actual = importRows(row("name", "Lewis"), row("name", "Marnie"));
    assertEquals(List.of("Lewis", "Marnie"), actual);
  }

  @Test
  void givenEmptyRow_thenSkip() {
    List<String> actual = importRows(row("name", "Lewis"), row(), row("name", "Marnie"));
    assertEquals(List.of("Lewis", "Marnie"), actual);
  }

  private List<String> importRows(Row... rows) {
    Task task = new Task();
    ImportRowProcessor processor = new ImportRowProcessor(table, task);
    processor.process(List.of(rows).iterator(), new TableStoreForCsvInMemory());
    return table.retrieveRows().stream()
        .map(Row::getValueMap)
        .map(map -> map.get("name"))
        .map(String::valueOf)
        .toList();
  }
}
