package org.molgenis.emx2.io.tablestore.processor;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;

import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.Task;
import org.molgenis.emx2.tasks.TaskStatus;

class ValidatePkeyProcessorTest {

  private static final String SCHEMA_NAME = ValidatePkeyProcessorTest.class.getSimpleName();

  private static Database database;
  private TableMetadata metadata;
  private Schema schema;

  @BeforeAll
  static void setupDatabase() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @BeforeEach
  void setUp() {
    schema = database.dropCreateSchema(SCHEMA_NAME);
    metadata =
        TableMetadata.table(
            "Person",
            column("name").setType(ColumnType.STRING).setPkey(),
            column("age").setType(ColumnType.INT));
    schema.create(metadata);
  }

  @AfterAll
  static void tearDownDatabase() {
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void givenDataWithPrimaryKeys_thenValidate() {
    Task task = new Task().start();
    ValidatePkeyProcessor processor = new ValidatePkeyProcessor(metadata, task);
    Iterator<Row> rows = List.of(row("name", "Lewis"), row("name", "Marnie")).iterator();
    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory();

    assertDoesNotThrow(() -> processor.process(rows, store));
  }

  @Test
  void givenDataWithoutPrimaryKey_thenTaskFails() {
    Task task = new Task().start();
    ValidatePkeyProcessor processor = new ValidatePkeyProcessor(metadata, task);
    Iterator<Row> rows = List.of(row("age", 52)).iterator();
    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory();

    assertThrows(MolgenisException.class, () -> processor.process(rows, store));
    assertEquals(TaskStatus.ERROR, task.getStatus());
    assertTrue(
        task.getDescription()
            .startsWith(
                "Missing keys found in table 'Person': missing value for key column 'name'. Row: ROW(age='52')"));
  }

  @Test
  void givenDataWithDuplicatePrimaryKey_thenTaskFails() {
    Task task = new Task().start();
    ValidatePkeyProcessor processor = new ValidatePkeyProcessor(metadata, task);
    Iterator<Row> rows =
        List.of(row("name", "lewis", "age", 52), row("name", "lewis", "age", 51)).iterator();
    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory();

    assertThrows(MolgenisException.class, () -> processor.process(rows, store));
    assertEquals(TaskStatus.ERROR, task.getStatus());
    assertTrue(task.getDescription().startsWith("Duplicate keys found in table Person: [lewis]"));
  }

  @Test
  void givenCompoundKey_whenMatching_thenTaskFails() {
    TableMetadata petMetaData =
        TableMetadata.table(
            "Pet",
            column("name").setType(ColumnType.STRING).setPkey(),
            column("type").setType(ColumnType.STRING).setPkey(),
            column("age").setType(ColumnType.INT));
    schema.create(petMetaData);

    Task task = new Task().start();
    ValidatePkeyProcessor processor = new ValidatePkeyProcessor(petMetaData, task);
    Iterator<Row> rows =
        List.of(
                row("name", "lewis", "type", "dog", "age", 4),
                row("name", "lewis", "type", "dog", "age", 2))
            .iterator();
    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory();

    assertThrows(MolgenisException.class, () -> processor.process(rows, store));
    assertEquals(TaskStatus.ERROR, task.getStatus());
    assertTrue(task.getDescription().startsWith("Duplicate keys found in table Pet: [lewis,dog]"));
  }

  @Test
  void givenCompoundKey_whenUnique_thenValidate() {
    TableMetadata petMetaData =
        TableMetadata.table(
            "Pet",
            column("name").setType(ColumnType.STRING).setPkey(),
            column("type").setType(ColumnType.STRING).setPkey(),
            column("age").setType(ColumnType.INT));
    schema.create(petMetaData);

    Task task = new Task().start();
    ValidatePkeyProcessor processor = new ValidatePkeyProcessor(petMetaData, task);
    Iterator<Row> rows =
        List.of(
                row("name", "lewis", "type", "dog", "age", 4),
                row("name", "lewis", "type", "cat", "age", 2))
            .iterator();
    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory();

    assertDoesNotThrow(() -> processor.process(rows, store));
  }
}
