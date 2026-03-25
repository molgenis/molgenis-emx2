package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestAutoIdGeneration {

  private static Database db;
  private static Schema schema;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestAutoIdGeneration.class.getSimpleName());
  }

  @Test
  void givenInvalidArgumentForAutoIdComputed_thenThrowException() {
    Table table =
        schema.create(
            table(
                "test_invalid_autoid_argument",
                new Column("id").setType(ColumnType.AUTO_ID).setComputed("${mg_autoid(invalid)}")));

    Row emptyRow = new Row("id", null);
    assertThrows(MolgenisException.class, () -> table.insert(emptyRow));
  }

  @Test
  void givenArgumentForAutoIdComputed_thenGenerateAccordingly() {
    Table table =
        schema.create(
            table(
                "test_autoid",
                new Column("id")
                    .setType(ColumnType.AUTO_ID)
                    .setComputed("${mg_autoid(length=1, format=NUMBERS)}")
                    .setPkey()));
    table.insert(new Row("id", null));
    Row row = table.retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS).getFirst();
    String generatedId = row.get("id", String.class);
    Matcher matcher = Pattern.compile("\\d").matcher(generatedId);
    assertTrue(matcher.find());
  }
}
