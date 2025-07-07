package org.molgenis.emx2.graphql;

import static graphql.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.sql.SqlQuery.SUM_FIELD;

import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestSumQuery {

  public static final String TAG = "Tag";
  public static final String COLORS = "colors";

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
            table(
                SAMPLES,
                column("N").setType(INT),
                column(TYPE).setType(REF).setRefTable(TYPE).setPkey(),
                column(TYPE_ARRAY).setType(REF_ARRAY).setRefTable(TYPE),
                column(GENDER).setType(REF).setRefTable(GENDER).setPkey(),
                column(COLORS).setType(STRING_ARRAY),
                column(TAG).setType(STRING)));

    samplesTable.insert(
        row(
            N,
            23,
            TYPE,
            "Type a",
            GENDER,
            "M",
            TYPE_ARRAY,
            List.of("Type a", "Type b"),
            COLORS,
            "green,red",
            TAG,
            "a"),
        row(
            N,
            5,
            TYPE,
            "Type a",
            GENDER,
            "F",
            TYPE_ARRAY,
            List.of("Type a"),
            COLORS,
            "green",
            TAG,
            "a"),
        row(
            N,
            2,
            TYPE,
            "Type b",
            GENDER,
            "M",
            TYPE_ARRAY,
            List.of("Type b"),
            COLORS,
            "red",
            TAG,
            "b"),
        row(
            N,
            9,
            TYPE,
            "Type b",
            GENDER,
            "F",
            TYPE_ARRAY,
            List.of("Type a", "Type b"),
            COLORS,
            "red",
            TAG,
            "b"));
  }

  @Test
  public void testSumQueryGroupByRef() {
    Table table = schema.getTable(SAMPLES).getMetadata().getTable();
    Query query1 = table.groupBy();
    query1.select(s(SUM_FIELD, s(N)), s(TYPE, s(NAME)));
    final String json = query1.retrieveJSON();
    assertTrue(json.contains("n\": 28")); // for Type A
    assertTrue(json.contains("11")); // for Type B
  }

  @Test
  public void testSumQueryGroupByRefArray() {
    Table table = schema.getTable(SAMPLES).getMetadata().getTable();
    Query query1 = table.groupBy();
    query1.select(s(SUM_FIELD, s(N)), s(TYPE_ARRAY, s(NAME)));
    final String json = query1.retrieveJSON();
    // note, should return identifier not name of column as key!
    assertTrue(json.contains("n\": 37")); // for Type A
    assertTrue(json.contains("34")); // for Type B
  }

  @Test
  public void testSumQueryGroupByString() {
    Table table = schema.getTable(SAMPLES).getMetadata().getTable();
    Query query1 = table.groupBy();
    query1.select(s(SUM_FIELD, s(N)), s(TAG));
    final String json = query1.retrieveJSON();
    assertEquals(
        "{\"Samples_groupBy\": [{\"tag\": \"a\", \"_sum\": {\"n\": 28}}, {\"tag\": \"b\", \"_sum\": {\"n\": 11}}]}",
        json);

    // todo, we need a permission test to ensure this cannot be done unless you have view permssion
    // on the table
  }

  @Test
  public void testSumQueryGroupByStringArray() {
    Table table = schema.getTable(SAMPLES).getMetadata().getTable();
    Query query1 = table.groupBy();
    query1.select(s(SUM_FIELD, s(N)), s(COLORS));
    final String json = query1.retrieveJSON();
    assertEquals(
        "{\"Samples_groupBy\": [{\"_sum\": {\"n\": 28}, \"colors\": \"green\"}, {\"_sum\": {\"n\": 34}, \"colors\": \"red\"}]}",
        json);

    // todo, we need a permission test to ensure this cannot be done unless you have view permssion
    // on the table
  }

  @Test
  public void testCombination() throws JsonProcessingException {
    Table table = schema.getTable(SAMPLES).getMetadata().getTable();
    Query query1 = table.groupBy();
    query1.select(s(SUM_FIELD, s(N)), s(TYPE_ARRAY, s(NAME)), s(TYPE, s(NAME)));
    String json = query1.retrieveJSON();
    assertTrue(json.contains("n\": 28")); // for Type b, Type b
    assertTrue(json.contains("11")); // for Type b, Type b
    assertTrue(json.contains("23")); // for Type a, Type b
    assertTrue(json.contains("9")); // for Type b, Type a

    // test that the graphql also works
    GraphQL graphql = new GraphqlApiFactory().createGraphqlForSchema(schema, new UserSession());
    ExecutionResult result =
        graphql.execute(
            """
            {Samples_groupBy {
              count
              _sum {
                n
              }
              gender {
                name
              }
            }}
            """);
    json = JsonUtil.getWriter().writeValueAsString(result.toSpecification().get("data"));
    assertEquals(
        """
{
  "Samples_groupBy" : [
    {
      "count" : 2,
      "_sum" : {
        "n" : 14
      },
      "gender" : {
        "name" : "F"
      }
    },
    {
      "count" : 2,
      "_sum" : {
        "n" : 25
      },
      "gender" : {
        "name" : "M"
      }
    }
  ]
}""",
        json);
  }
}
