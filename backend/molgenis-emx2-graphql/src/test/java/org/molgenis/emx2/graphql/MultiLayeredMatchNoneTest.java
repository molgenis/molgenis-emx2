package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class MultiLayeredMatchNoneTest {

  private static final String SCHEMA_NAME = MultiLayeredMatchNoneTest.class.getSimpleName();

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static GraphQL graphql;

  /**
   * Initializes the test database schema and sample data before all tests.
   *
   * <p>Creates a three-table relational structure (A → B → C) where each table has:
   *
   * <p><b>Schema Structure:</b>
   *
   * <pre>
   * Table C: Base level, no outgoing references
   *   - name (STRING, PK)
   *
   * Table B: References C
   *   - name (STRING, PK)
   *   - rel (REF_ARRAY -> C)
   *
   * Table A: References B
   *   - name (STRING, PK)
   *   - rel (REF_ARRAY -> B)
   * </pre>
   *
   * <p><b>Test Data Graph:</b>
   *
   * <pre>
   * a1 -> b1 -> c1, c2
   * a2 -> b2 -> c1
   * a3 -> b1, b2 -> c1, c2
   * </pre>
   *
   * <p>This setup creates various relationship scenarios including:
   *
   * <ul>
   *   <li>Single references (a1 -> b1, a2 -> b2)
   *   <li>Multiple references (a3 -> b1, b2)
   *   <li>Shared references (both b1 and b2 reference c1)
   * </ul>
   */
  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);

    Table cTable =
        schema.create(
            TableMetadata.table("C", Column.column("name").setType(ColumnType.STRING).setPkey()));
    Table bTable =
        schema.create(
            TableMetadata.table(
                "B",
                Column.column("name").setType(ColumnType.STRING).setPkey(),
                Column.column("rel").setType(ColumnType.REF_ARRAY).setRefTable("C")));
    Table aTable =
        schema.create(
            TableMetadata.table(
                "A",
                Column.column("name").setType(ColumnType.STRING).setPkey(),
                Column.column("rel").setType(ColumnType.REF_ARRAY).setRefTable("B")));

    // a1 -> b1 -> c1, c2
    // a2 -> b2 -> c1
    // a3 -> b1, b2 -> c1, c2
    cTable.insert(Row.row("name", "c1"), Row.row("name", "c2"));
    bTable.insert(
        Row.row("name", "b1", "rel", List.of("c1", "c2")),
        Row.row("name", "b2", "rel", List.of("c1")));
    aTable.insert(
        Row.row("name", "a1", "rel", List.of("b1")),
        Row.row("name", "a2", "rel", List.of("b2")),
        Row.row("name", "a3", "rel", List.of("b1", "b2")));

    graphql = new GraphqlApiFactory().createGraphqlForSchema(schema);
  }

  @Test
  void shouldFilterOnFirstLayer() {
    String query1 =
        """
        {
          A (filter: { rel : { name: { match_none : ["b1"] } } }) {
            name
          }
        }
        """;
    assertQueryReturnsExpectedNames(query1, "a2");

    String query2 =
        """
            {
              A (filter: { rel : { name: { match_none : ["b1", "b2"] } } }) {
                name
              }
            }
            """;
    assertQueryReturnsExpectedNames(query2);
  }

  /**
   * { Pet( filter: { _and: [ { tags: { name: { match_none: ["red"] } } }, { status: { equals:
   * "available" } } ] } ) { name tags { name } status } }
   */
  @Test
  void shouldFilterOnSecondLayer() {
    String query1 =
        """
        {
          A (filter: { rel : { rel: { name: { match_none : ["c2"] } } } }) {
            name
            rel {
              name
              rel {
                name
              }
            }
          }
        }
        """;
    assertQueryReturnsExpectedNames(query1, "a2");

    String query2 =
        """
        {
          A (filter: { rel : { rel: { name: { match_none : ["c1", "c2"] } } } }) {
            name
          }
        }
        """;
    assertQueryReturnsExpectedNames(query2);

    String query3 =
        """
        {
          A (filter: { rel : { rel: { name: { match_none : ["c1"] } } } }) {
            name
          }
        }
        """;
    assertQueryReturnsExpectedNames(query3);
  }

  @Test
  void shouldFilterOnAttributeOfRelation() {
    String query1 =
        """
          {
            A {
              name
              rel(filter:  {
                  rel: {
                    name: {
                      match_none: ["c2"]
                    }
                  }
              }) {
                name
                rel {
                  name
                }
              }
            }
          }
          """;
    assertQueryReturnsExpectedNames(query1, "a2");
  }

  private void assertQueryReturnsExpectedNames(String query, String... expectedNames) {
    ExecutionResult execute = graphql.execute(query);
    JsonNode jsonNode = MAPPER.valueToTree(execute.toSpecification()).get("data").get("A");
    List<A> results;

    try {
      results = MAPPER.readerForListOf(A.class).readValue(jsonNode);
    } catch (IOException e) {
      throw new AssertionError("Unable to parse graphql response", e);
    }

    if (results == null) {
      if (expectedNames.length != 0) {
        throw new AssertionError(
            "No results found while expecting: " + String.join(", ", expectedNames));
      } else {
        // Expected no results
        return;
      }
    }

    assertEquals(Set.of(expectedNames), results.stream().map(A::name).collect(Collectors.toSet()));
  }

  record A(String name, List<B> rel) {}

  record B(String name, List<C> rel) {}

  record C(String name) {}
}
