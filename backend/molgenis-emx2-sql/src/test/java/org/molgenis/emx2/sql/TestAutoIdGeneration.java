package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestAutoIdGeneration {

  private static Schema schema;

  @BeforeAll
  static void setUp() {
    Database db = TestDatabaseFactory.getTestDatabase();
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

  @Test
  void givenTableInheritance_whenInserting_thenGenerateAutoId() {
    schema.create(
        table(
            "test_inheritance_A",
            new Column("a_id")
                .setType(ColumnType.AUTO_ID)
                .setComputed("${mg_autoid(length=1, format=numbers)}")
                .setPkey(),
            new Column("a_label").setType(ColumnType.INT)),
        table(
                "test_inheritance_B",
                new Column("b_id")
                    .setType(ColumnType.AUTO_ID)
                    .setComputed("${mg_autoid(length=2, format=numbers)}")
                    .setPkey(),
                new Column("b_label").setType(ColumnType.INT))
            .setInheritName("test_inheritance_A"));

    Table tableC =
        schema.create(
            table(
                    "test_inheritance_C",
                    new Column("c_id")
                        .setType(ColumnType.AUTO_ID)
                        .setComputed("${mg_autoid(length=3, format=numbers)}")
                        .setPkey(),
                    new Column("c_label").setType(ColumnType.INT))
                .setInheritName("test_inheritance_B"));

    int nrInserted = tableC.insert(Row.row());
    assertEquals(1, nrInserted);

    List<Row> rows = tableC.retrieveRows();
    assertEquals(1, rows.size());
    Row row = rows.getFirst();
    assertRowValueMatchesPattern(row, "a_id", "\\d");
    assertRowValueMatchesPattern(row, "b_id", "\\d{2}");
    assertRowValueMatchesPattern(row, "c_id", "\\d{3}");
  }

  private void assertRowValueMatchesPattern(Row row, String value, String pattern) {
    assertTrue(Pattern.compile(pattern).matcher(row.getString(value)).matches());
  }
}
