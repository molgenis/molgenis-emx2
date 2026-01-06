package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TestGraphqlObjectFilters {

  private static final String SCHEMA_NAME = TestGraphqlObjectFilters.class.getSimpleName();

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static GraphQL graphql;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);
    schema.create(
        TableMetadata.table("Person", Column.column("name").setType(ColumnType.STRING).setPkey()));
    schema.getTable("Person").insert(Row.row("name", "John"), Row.row("name", "Steve"));
    graphql = new GraphqlApiFactory().createGraphqlForSchema(schema);
  }

  @Test
  void givenEquals_thenResultHasName() throws IOException {
    var query =
        """
          {
            Person (filter: { equals: { name: "John" } }) {
              name
            }
          }
        """;

    ExecutionResult execute = graphql.execute(query);
    JsonNode jsonNode = MAPPER.valueToTree(execute.toSpecification()).get("data").get("Person");

    List<Person> people = MAPPER.readerForListOf(Person.class).readValue(jsonNode);
    assertEquals(List.of(new Person("John")), people);
  }

  @Test
  void givenNotEquals_thenResultDoesntHaveName() throws IOException {
    var query =
        """
          {
            Person (filter: { not_equals: { name: "John" } }) {
              name
            }
          }
        """;

    ExecutionResult execute = graphql.execute(query);
    JsonNode jsonNode = MAPPER.valueToTree(execute.toSpecification()).get("data").get("Person");

    List<Person> people = MAPPER.readerForListOf(Person.class).readValue(jsonNode);
    assertEquals(List.of(new Person("Steve")), people);
  }

  record Person(String name) {}
}
