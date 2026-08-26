package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TestGraphqlMutationTransaction {

  private static final String schemaName = TestGraphqlMutationTransaction.class.getSimpleName();

  private static Schema schema;
  private static GraphqlExecutor graphqlExecutor;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(schemaName);
    schema.create(
        table("Author", column("name").setPkey(), column("country")),
        table("Book", column("title").setPkey(), column("author", REF).setRefTable("Author")));
    graphqlExecutor = new GraphqlExecutor(schema);
  }

  @BeforeEach
  void emptyTables() {
    schema.getTable("Book").truncate();
    schema.getTable("Author").truncate();
  }

  @Test
  void insertIsRolledBackWhenAnotherTableFails() throws IOException {
    // the Book row fails because it refers to an author that does not exist
    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                execute(
                    """
                    mutation {
                      insert(
                        Author: { name: "tolkien", country: "uk" }
                        Book: { title: "lord of the rings", author: { name: "does not exist" } }
                      ) { message }
                    }
                    """));
    assertFailedOnBook(exception);

    // the valid Author row must have been rolled back along with it
    assertEquals(0, count("Author"));
    assertEquals(0, count("Book"));
  }

  @Test
  void saveIsRolledBackWhenAnotherTableFails() throws IOException {
    schema.getTable("Author").insert(row("name", "tolkien", "country", "uk"));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                execute(
                    """
                    mutation {
                      save(
                        Author: { name: "tolkien", country: "new zealand" }
                        Book: { title: "lord of the rings", author: { name: "does not exist" } }
                      ) { message }
                    }
                    """));
    assertFailedOnBook(exception);

    assertEquals("uk", country("tolkien"));
    assertEquals(0, count("Book"));
  }

  @Test
  void updateIsRolledBackWhenAnotherTableFails() throws IOException {
    schema.getTable("Author").insert(row("name", "tolkien", "country", "uk"));
    schema.getTable("Book").insert(row("title", "lord of the rings"));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                execute(
                    """
                    mutation {
                      update(
                        Author: { name: "tolkien", country: "new zealand" }
                        Book: { title: "lord of the rings", author: { name: "does not exist" } }
                      ) { message }
                    }
                    """));
    assertFailedOnBook(exception);

    assertEquals("uk", country("tolkien"));
  }

  @Test
  void deleteIsRolledBackWhenAnotherTableFails() throws IOException {
    schema.getTable("Author").insert(row("name", "tolkien", "country", "uk"));
    schema.getTable("Book").insert(row("title", "lord of the rings"));

    // a strict delete of a book that does not exist fails
    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                execute(
                    """
                    mutation {
                      delete(
                        strict: true
                        Author: { name: "tolkien" }
                        Book: { title: "unknown book" }
                      ) { message }
                    }
                    """));
    assertFailedOnBook(exception);

    // the author must not have been deleted
    assertEquals(1, count("Author"));
    assertEquals(1, count("Book"));
  }

  @Test
  void multiTableMutationIsCommittedWhenAllTablesSucceed() throws IOException {
    String message =
        execute(
                """
                mutation {
                  insert(
                    Author: { name: "tolkien", country: "uk" }
                    Book: { title: "lord of the rings", author: { name: "tolkien" } }
                  ) { message }
                }
                """)
            .at("/insert/message")
            .asText();

    assertEquals("inserted 1 records to Author\ninserted 1 records to Book\n", message);
    assertEquals(1, count("Author"));
    assertEquals(1, count("Book"));
  }

  @Test
  void mutationWithoutAnyTableFails() {
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> execute("mutation{insert{message}}"));
    assertEquals("None or invalid tables provided", exception.getMessage());
  }

  private void assertFailedOnBook(MolgenisException exception) {
    assertTrue(
        exception.getMessage().contains("Book"),
        "expected the failure to originate from table Book but got: " + exception.getMessage());
  }

  private int count(String tableName) throws IOException {
    return execute("{" + tableName + "_agg{count}}").at("/" + tableName + "_agg/count").intValue();
  }

  private String country(String authorName) throws IOException {
    return execute("{Author(filter:{name:{equals:\"" + authorName + "\"}}){country}}")
        .at("/Author/0/country")
        .asText();
  }

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(graphqlExecutor.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return node.get("data");
  }
}
