package org.molgenis.emx2.io.tablestore.processor;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.*;

import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.sql.autoid.SqlSequence;
import org.molgenis.emx2.tasks.Task;

class ImportRowProcessorTest {

  private static final String SCHEMA_NAME = ImportRowProcessorTest.class.getSimpleName();
  private static SqlDatabase database;
  private Table table;
  private Schema schema;

  @BeforeAll
  static void setupDatabase() {
    database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  }

  @BeforeEach
  void setUp() {
    schema = database.dropCreateSchema(SCHEMA_NAME);
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
  void givenRowsWithAutoId_thenUpdateSequence() {
    Column column =
        column("id")
            .setType(ColumnType.AUTO_ID)
            .setPkey()
            .setComputed("${mg_autoid(format=numbers, length=4)}");

    Table autoIdTable =
        schema.create(
            TableMetadata.table("AutoId", column, column("name").setType(ColumnType.STRING)));

    List<Map<String, Object>> imported =
        importRows(autoIdTable, row("id", "1234", "name", "first-name"));
    SqlSequence sequence =
        new SqlSequence(database.getJooq(), SCHEMA_NAME, getSequenceName(autoIdTable, column));
    assertEquals(imported, List.of(Map.of("id", "1234", "name", "first-name")));
    assertEquals(7569, sequence.getCurrentValue());

    imported = importRows(autoIdTable, row("id", "1234", "name", "second-name"));
    assertEquals(imported, List.of(Map.of("id", "1234", "name", "second-name")));
    assertEquals(7569, sequence.getCurrentValue());
  }

  private static String getSequenceName(Table table, Column column) {
    return String.join(
        "-",
        SCHEMA_NAME,
        table.getName(),
        column.getName(),
        HexFormat.of().toHexDigits(column.getComputed().hashCode()));
  }

  @Test
  void givenRows_thenImportToSpecifiedTable() {
    List<Map<String, Object>> actual =
        importRows(table, row("name", "Lewis"), row("name", "Marnie"));
    List<Map<String, String>> expected = List.of(Map.of("name", "Lewis"), Map.of("name", "Marnie"));
    assertEquals(expected, actual);
  }

  @Test
  void givenEmptyRows_thenSkip() {
    List<Map<String, Object>> actual =
        importRows(table, row("name", "Lewis"), row(), row("name", null), row("name", "Marnie"));
    List<Map<String, String>> expected = List.of(Map.of("name", "Lewis"), Map.of("name", "Marnie"));
    assertEquals(expected, actual);
  }

  private List<Map<String, Object>> importRows(Table table, Row... rows) {
    Task task = new Task();
    ImportRowProcessor processor = new ImportRowProcessor(table, task);
    processor.process(List.of(rows).iterator(), new TableStoreForCsvInMemory());
    return table.retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS).stream()
        .map(Row::getValueMap)
        .toList();
  }
}
