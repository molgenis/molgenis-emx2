package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.DATE_ARRAY;
import static org.molgenis.emx2.ColumnType.INT_ARRAY;
import static org.molgenis.emx2.ColumnType.STRING_ARRAY;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.BETWEEN;
import static org.molgenis.emx2.Operator.LIKE;
import static org.molgenis.emx2.Operator.NOT_BETWEEN;
import static org.molgenis.emx2.TableMetadata.table;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Operator;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;

public class TestArrayFilters {

  private static final String INTS = "ints";
  private static final String DATES = "dates";
  private static final String TAGS = "tags";
  private static Table table;

  @BeforeAll
  public static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    table =
        database
            .dropCreateSchema(TestArrayFilters.class.getSimpleName())
            .create(
                table(
                    "T",
                    column("id").setPkey(),
                    column(INTS).setType(INT_ARRAY),
                    column(DATES).setType(DATE_ARRAY),
                    column(TAGS).setType(STRING_ARRAY)));
    table.insert(
        new Row()
            .set("id", "a")
            .set(INTS, new Integer[] {1, 5})
            .set(DATES, new LocalDate[] {LocalDate.of(2020, 1, 1), LocalDate.of(2020, 6, 1)})
            .set(TAGS, new String[] {"Apple pie", "banana"}),
        new Row()
            .set("id", "b")
            .set(INTS, new Integer[] {20, 30})
            .set(DATES, new LocalDate[] {LocalDate.of(2021, 1, 1)})
            .set(TAGS, new String[] {"cherry", "apple"}),
        new Row()
            .set("id", "c")
            .set(INTS, new Integer[] {3, 50})
            .set(DATES, new LocalDate[] {LocalDate.of(2022, 1, 1)})
            .set(TAGS, new String[] {"date palm", "fig tree"}));
  }

  @Test
  public void betweenOnArrayMatchesWhenAnyElementInAnyRange() {
    assertEquals(List.of("a", "c"), ids(INTS, BETWEEN, 2, 10));
    assertEquals(List.of("a", "b"), ids(INTS, BETWEEN, 0, 1, 25, 30));
    assertEquals(List.of("b", "c"), ids(INTS, BETWEEN, 25, null));
    assertEquals(List.of("a"), ids(INTS, BETWEEN, null, 2));
    assertEquals(List.of("b"), ids(INTS, NOT_BETWEEN, 2, 10));

    LocalDate d2020 = LocalDate.of(2020, 3, 1);
    LocalDate d2020end = LocalDate.of(2020, 12, 31);
    assertEquals(List.of("a"), ids(DATES, BETWEEN, d2020, d2020end));
    assertEquals(
        List.of("a", "c"),
        ids(DATES, BETWEEN, d2020, d2020end, LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)));
    assertEquals(List.of("b", "c"), ids(DATES, BETWEEN, LocalDate.of(2020, 12, 31), null));
  }

  @Test
  public void likeOnStringArrayMatchesAnyElementAnyTerm() {
    assertEquals(List.of("a"), ids(TAGS, LIKE, "BANAN"));
    assertEquals(List.of("a", "b"), ids(TAGS, LIKE, "apple"));
    assertEquals(List.of("b", "c"), ids(TAGS, LIKE, "cherry", "fig"));
    assertEquals(List.of("a"), ids(TAGS, LIKE, "apple pie"));
    assertEquals(List.of(), ids(TAGS, LIKE, "pie apple"));
    assertEquals(List.of(), ids(TAGS, LIKE, "zzz"));
  }

  private static List<String> ids(String column, Operator op, Object... range) {
    return table.query().where(f(column, op, range)).orderBy("id").retrieveRows().stream()
        .map(r -> r.getString("id"))
        .toList();
  }
}
