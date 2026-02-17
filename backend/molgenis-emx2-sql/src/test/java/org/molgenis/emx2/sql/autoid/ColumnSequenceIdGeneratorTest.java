package org.molgenis.emx2.sql.autoid;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class ColumnSequenceIdGeneratorTest {

  private static final String SCHEMA_NAME = ColumnSequenceIdGeneratorTest.class.getSimpleName();

  private static SqlDatabase database;
  private static DSLContext jooq;
  private Schema schema;

  @BeforeAll
  static void setup() {
    database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  }

  @BeforeEach
  void setupSchema() {
    schema = database.dropCreateSchema(SCHEMA_NAME);
    jooq = database.getJooq();
  }

  @Test
  void givenColumnWithoutComputed_thenThrow() {
    Column column = addColumnWithComputedToSchema(null);
    assertThrows(IllegalArgumentException.class, () -> new ColumnSequenceIdGenerator(column, jooq));
  }

  @Test
  void shouldHandleMultiple() {
    Column column =
        addColumnWithComputedToSchema(
            "FOO-${mg_autoid(length=3, format=numbers)}-${mg_autoid(length=5, format=numbers)}");
    ColumnSequenceIdGenerator generator = new ColumnSequenceIdGenerator(column, jooq);

    assertEquals("FOO-000-00000", generator.generateId());
    assertEquals("FOO-000-00001", generator.generateId());
    assertEquals("FOO-000-00002", generator.generateId());
    assertColumnHasSequenceWithLimit(column, 99999999);
  }

  private static void assertColumnHasSequenceWithLimit(Column column, long expectedLimit) {
    String name = SCHEMA_NAME + "-" + column.getName() + "-" + column.getComputed().hashCode();
    assertTrue(SqlSequence.exists(jooq, SCHEMA_NAME, name));
    SqlSequence sequence = new SqlSequence(jooq, SCHEMA_NAME, name);
    assertEquals(expectedLimit, sequence.limit());
  }

  private Column addColumnWithComputedToSchema(String computed) {
    return schema
        .create(
            TableMetadata.table(
                "Person", column("id", ColumnType.AUTO_ID).setPkey().setComputed(computed)))
        .getMetadata()
        .getColumn("id");
  }
}
