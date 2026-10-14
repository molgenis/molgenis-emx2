package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.ENUM;
import static org.molgenis.emx2.ColumnType.ENUM_ARRAY;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;

class TestEnumColumns {

  private static final String SCHEMA_NAME = TestEnumColumns.class.getSimpleName();
  private static final String SAMPLE = "Sample";
  private static final String ALTERABLE = "Alterable";
  private static final String ALTERABLE_KEEP = "AlterableKeep";
  private static final String ANSWER = "answer";
  private static final String COLORS = "colors";
  private static final String ID = "id";
  private static final List<String> ANSWER_VALUES = List.of("yes", "no", "unknown");

  private static Database db;
  private static Schema schema;
  private static Table sample;

  @BeforeAll
  static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(
        table(SAMPLE)
            .add(column(ID).setPkey())
            .add(column(ANSWER).setType(ENUM).setAllowedValues(ANSWER_VALUES))
            .add(column(COLORS).setType(ENUM_ARRAY).setAllowedValues("red", "green", "blue")));
    for (String name : List.of(ALTERABLE, ALTERABLE_KEEP)) {
      schema.create(
          table(name)
              .add(column(ID).setPkey())
              .add(column(ANSWER).setType(ENUM).setAllowedValues(ANSWER_VALUES)));
    }
    sample = schema.getTable(SAMPLE);
  }

  @Test
  void valuesSurviveReloadInOrderAndNonEnumColumnsHaveNone() {
    db.clearCache();
    TableMetadata reloaded = meta(SAMPLE);
    assertEquals(ANSWER_VALUES, reloaded.getColumn(ANSWER).getAllowedValues());
    assertEquals(List.of("red", "green", "blue"), reloaded.getColumn(COLORS).getAllowedValues());
    assertNull(reloaded.getColumn(ID).getAllowedValues());
  }

  @Test
  void writeAcceptsAllowedValuesAndNullAndReadsThemBack() {
    sample.insert(row(ID, "ok", ANSWER, "no", COLORS, "blue,red"), row(ID, "empty"));

    Row stored = rowWithId(sample, "ok");
    assertEquals("no", stored.getString(ANSWER));
    assertArrayEquals(new String[] {"blue", "red"}, stored.getStringArray(COLORS));
    Row empty = rowWithId(sample, "empty");
    assertNull(empty.getString(ANSWER));
    assertNull(empty.getStringArray(COLORS));
  }

  @Test
  void scalarWriteRejectsValueOutsideListNamingColumnAndValue() {
    MolgenisException insertError =
        assertThrows(MolgenisException.class, () -> sample.insert(row(ID, "s1", ANSWER, "maybe")));
    assertTrue(insertError.getMessage().contains(ANSWER), insertError.getMessage());
    assertTrue(insertError.getMessage().contains("maybe"), insertError.getMessage());

    sample.insert(row(ID, "s2", ANSWER, "yes"));
    assertThrows(MolgenisException.class, () -> sample.update(row(ID, "s2", ANSWER, "YES")));
    assertEquals("yes", rowWithId(sample, "s2").getString(ANSWER));
  }

  @Test
  void arrayWriteRejectsAnyElementOutsideList() {
    MolgenisException error =
        assertThrows(
            MolgenisException.class, () -> sample.insert(row(ID, "a1", COLORS, "red,purple")));
    assertTrue(error.getMessage().contains(COLORS), error.getMessage());
    assertTrue(error.getMessage().contains("purple"), error.getMessage());
  }

  @Test
  void tableCreateRejectsWrongValuesListNamingColumnAndSavesNothing() {
    Map<String, Column> wrongLists =
        Map.of(
            "NoValues", column("bad").setType(ENUM),
            "EmptyValues", column("bad").setType(ENUM_ARRAY).setAllowedValues(List.of()),
            "BlankValue", column("bad").setType(ENUM).setAllowedValues("a", " "),
            "DuplicateValue", column("bad").setType(ENUM).setAllowedValues("a", "b", "a"),
            "ValuesOnString", column("bad").setType(STRING).setAllowedValues("a", "b"));
    wrongLists.forEach(
        (tableName, badColumn) -> {
          MolgenisException error =
              assertThrows(
                  MolgenisException.class,
                  () -> schema.create(table(tableName).add(column(ID).setPkey()).add(badColumn)),
                  tableName);
          assertTrue(error.getMessage().contains("bad"), error.getMessage());
          db.clearCache();
          assertNull(db.getSchema(SCHEMA_NAME).getTable(tableName), tableName);
        });
  }

  @Test
  void columnAlterRejectsWrongValuesListAndKeepsStoredList() {
    List<Column> wrongLists =
        List.of(
            column(ANSWER).setType(ENUM),
            column(ANSWER).setType(ENUM).setAllowedValues("a", ""),
            column(ANSWER).setType(ENUM).setAllowedValues("a", "a"),
            column(ANSWER).setType(STRING).setAllowedValues("a"));
    for (Column wrong : wrongLists) {
      MolgenisException error =
          assertThrows(MolgenisException.class, () -> meta(ALTERABLE).alterColumn(wrong));
      assertTrue(error.getMessage().contains(ANSWER), error.getMessage());
    }
    db.clearCache();
    assertEquals(ANSWER_VALUES, meta(ALTERABLE).getColumn(ANSWER).getAllowedValues());
  }

  @Test
  void columnAlterAcceptsListThatKeepsAtLeastOneValue() {
    meta(ALTERABLE_KEEP).alterColumn(column(ANSWER).setType(ENUM).setAllowedValues("yes"));
    db.clearCache();
    assertEquals(List.of("yes"), meta(ALTERABLE_KEEP).getColumn(ANSWER).getAllowedValues());
  }

  private static Row rowWithId(Table table, String id) {
    return table.retrieveRows().stream()
        .filter(r -> id.equals(r.getString(ID)))
        .findFirst()
        .orElseThrow();
  }

  private static TableMetadata meta(String tableName) {
    return db.getSchema(SCHEMA_NAME).getTable(tableName).getMetadata();
  }
}
