package org.molgenis.emx2.sql.autoid;

import static org.junit.jupiter.api.Assertions.*;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class RetryingIdGeneratorTest {

  private static final String SCHEMA_NAME = RetryingIdGeneratorTest.class.getSimpleName();

  private static SqlDatabase database;
  private Column column;
  private DSLContext jooq;
  private Schema schema;

  @BeforeAll
  static void setup() {
    database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  }

  @BeforeEach
  void setupSchema() {
    schema = database.dropCreateSchema(SCHEMA_NAME);
    jooq = database.getJooq();
    TableMetadata table =
        schema
            .create(TableMetadata.table("Person"))
            .getMetadata()
            .add(
                Column.column("id")
                    .setType(ColumnType.AUTO_ID)
                    .setComputed("${mg_autoid(format=mixed, length=25)}")
                    .setPkey());
    column = table.getColumn("id");
    setSeed();
  }

  private void setSeed() {
    jooq.execute("SELECT setseed(0.5);");
  }

  @Test
  void givenColumnWithAutoIdFormat_thenCallGenerateFunction() {
    Field<String> generated = new RetryingIdGenerator(column).generateId();
    String id = jooq.select(generated).fetchOneInto(String.class);

    // Expected is based on the provided seed
    assertEquals("9zIKo2IJfve8chubuk30CWP60", id);
  }

  @Test
  void givenSqlInjection_thenEscape() {
    Table table = schema.getTable("Person");
    table.insert(Row.row("id", "9zIKo2IJfve8chubuk30CWP60"));

    assertFalse(table.retrieveRows().isEmpty());
    column.setComputed("; DROP table Person; ${mg_autoid(format=mixed, length=25)}");
    new RetryingIdGenerator(column).generateId();
    assertFalse(table.retrieveRows().isEmpty());
  }

  @Test
  void givenComputed_whenFormatHasPrefix_thenAddToGeneratedId() {
    column.setComputed("PREFIX-${mg_autoid(format=numbers, length=4)}");
    Field<String> generated = new RetryingIdGenerator(column).generateId();
    String id = jooq.select(generated).fetchOneInto(String.class);

    // Expected is based on the provided seed
    assertEquals("PREFIX-9811", id);
  }

  @Test
  void givenComputed_whenFormatHasSuffix_thenAddToGeneratedId() {
    column.setComputed("${mg_autoid(format=numbers, length=4)}-SUFFIX");
    Field<String> generated = new RetryingIdGenerator(column).generateId();
    String id = jooq.select(generated).fetchOneInto(String.class);

    // Expected is based on the provided seed
    assertEquals("9811-SUFFIX", id);
  }
}
