package org.molgenis.emx2.fairmapper.tasks;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;

import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class DatabaseDataLoaderTest {

  private static final String SCHEMA_NAME = DatabaseDataLoaderTest.class.getSimpleName();
  private static final Database DATABASE = TestDatabaseFactory.getTestDatabase();

  private Schema schema;

  @BeforeEach
  void setupSchema() {
    schema = DATABASE.dropCreateSchema(SCHEMA_NAME);
    schema
        .getMetadata()
        .create(
            TableMetadata.table("Person").add(Column.column("name", ColumnType.STRING).setPkey()));
  }

  @Test
  void givenSchemaWithData_whenLoad_thenTableStoreStaysEmpty() {
    InMemoryTableStore tableStore = new InMemoryTableStore();
    tableStore.writeTable("Person", List.of("name"), List.of(Row.row("name", "Donald")));

    DatabaseDataLoader loader = new DatabaseDataLoader(schema, "Person");
    loader.load(tableStore);

    Iterator<Row> rows = tableStore.readTable("Person").iterator();
    CompareTools.assertEquals(
        rows.next(), Row.row("name", "Donald", MG_TABLECLASS, "DatabaseDataLoaderTest.Person"));
    assertFalse(rows.hasNext());
  }

  @Test
  void givenNoTablesRequested_whenLoad_thenCompletesWithoutError() {
    InMemoryTableStore tableStore = new InMemoryTableStore();
    DatabaseDataLoader loader = new DatabaseDataLoader(schema);

    assertDoesNotThrow(() -> loader.load(tableStore));
    assertTrue(tableStore.getTableNames().isEmpty());
  }
}
