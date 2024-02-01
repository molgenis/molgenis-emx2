package org.molgenis.emx2.sql;

import static graphql.Assert.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestSumQuery {
  private static final String TEST_SUM_QUERY = "TestSumQuery";
  static Database database;
  static Schema schema;
  private static final String SAMPLES = "Samples";
  private static final String TYPE = "Type";

  private static final String TYPE_ARRAY = "TypeArray";
  private static final String NAME = "Name";
  private static final String GENDER = "Gender";
  private static final String N = "N";

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();

    // createColumn a schema to test with
    schema = database.dropCreateSchema(TEST_SUM_QUERY);

    // createColumn some tables with contents
    final Table typeTable = schema.create(table(TYPE).add(column(NAME).setPkey()));
    typeTable.insert(row(NAME, "Type a"), row(NAME, "Type b"), row(NAME, "Type c"));
    final Table genderTable = schema.create(table(GENDER).add(column(NAME).setPkey()));
    genderTable.insert(row(NAME, "M"), row(NAME, "F"));

    final Table samplesTable =
        schema.create(
            table(SAMPLES)
                .add(column("N").setType(INT))
                .add(column(TYPE).setType(REF).setRefTable(TYPE).setPkey())
                .add(column(TYPE_ARRAY).setType(REF_ARRAY).setRefTable(TYPE))
                .add(column(GENDER).setType(REF).setRefTable(GENDER).setPkey()));

    samplesTable.insert(
        row(N, 23, TYPE, "Type a", GENDER, "M", TYPE_ARRAY, List.of("Type a", "Type b")),
        row(N, 5, TYPE, "Type a", GENDER, "F", TYPE_ARRAY, List.of("Type a")),
        row(N, 2, TYPE, "Type b", GENDER, "M", TYPE_ARRAY, List.of("Type b")),
        row(N, 9, TYPE, "Type b", GENDER, "F", TYPE_ARRAY, List.of("Type a", "Type b")));
  }

  @Test
  public void testSumQueryGroupByRef() {
    Table table = schema.getTable(SAMPLES).getMetadata().getTable();
    Query query1 = table.groupBy();
    query1.select(s("sum", s(N)), s(TYPE, s(NAME)));
    final String json = query1.retrieveJSON();
    assertTrue(json.contains("28")); // for Type A
    assertTrue(json.contains("11")); // for Type B
    System.out.println(json);

    // todo filter example
  }

  @Test
  public void testSumQueryGroupByRefArray() {
    Table table = schema.getTable(SAMPLES).getMetadata().getTable();
    Query query1 = table.groupBy();
    query1.select(s("sum", s(N)), s(TYPE_ARRAY, s(NAME)));
    final String json = query1.retrieveJSON();
    assertTrue(json.contains("37")); // for Type A
    assertTrue(json.contains("34")); // for Type B
    System.out.println(json);

    // todo filter example
  }

  @Test
  public void testCombination() {
    Table table = schema.getTable(SAMPLES).getMetadata().getTable();
    Query query1 = table.groupBy();
    query1.select(s("sum", s(N)), s(TYPE_ARRAY, s(NAME)), s(TYPE, s(NAME)));
    final String json = query1.retrieveJSON();
    assertTrue(json.contains("28")); // for Type b, Type b
    assertTrue(json.contains("11")); // for Type b, Type b
    assertTrue(json.contains("23")); // for Type a, Type b
    assertTrue(json.contains("9")); // for Type b, Type a
    System.out.println(json);
  }
}
