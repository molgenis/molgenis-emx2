package org.molgenis.emx2.sql.autoid;

import static org.junit.jupiter.api.Assertions.*;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class RetryingIdGeneratorTest {

  private static final String SCHEMA_NAME = RetryingIdGeneratorTest.class.getSimpleName();

  private static SqlDatabase database;
  private Column column;
  private DSLContext jooq;

  @BeforeAll
  static void setup() {
    database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  }

  @BeforeEach
  void setupSchema() {
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);
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
  }

  @Test
  void givenColumnWithAutoIdFormat_thenCallGenerateFunction() {
    jooq.execute("SELECT setseed(0.5);");
    String generated = new RetryingIdGenerator(column).generateId().toString();
    String id = jooq.resultQuery(generated).fetchOneInto(String.class);

    // Expected is based on the provided seed
    assertEquals("9zIKo2IJfve8chubuk30CWP60", id);
  }
}
