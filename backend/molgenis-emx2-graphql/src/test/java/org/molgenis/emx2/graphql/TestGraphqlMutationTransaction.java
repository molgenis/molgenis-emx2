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
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TestGraphqlMutationTransaction {

  private static final String SCHEMA_NAME = TestGraphqlMutationTransaction.class.getSimpleName();

  private static Schema schema;
  private static Table author;
  private static Table book;
  private static GraphqlExecutor graphqlExecutor;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(SCHEMA_NAME);
    schema.create(
        table("Author", column("name").setPkey(), column("country")),
        table("Book", column("title").setPkey(), column("author", REF).setRefTable("Author")));
    author = schema.getTable("Author");
    book = schema.getTable("Book");
    graphqlExecutor = new GraphqlExecutor(schema);
  }

  @BeforeEach
  void emptyTables() {
    book.truncate();
    author.truncate();
  }

  @Test
  void insertIsRolledBackWhenAnotherTableFails() {
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

    CompareTools.assertEquals(List.of(), retrieveRows(author));
    CompareTools.assertEquals(List.of(), retrieveRows(book));
  }

  @Test
  void saveIsRolledBackWhenAnotherTableFails() {
    author.insert(row("name", "tolkien", "country", "uk"));

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

    CompareTools.assertEquals(
        List.of(row("name", "tolkien", "country", "uk")), retrieveRows(author));
    CompareTools.assertEquals(List.of(), retrieveRows(book));
  }

  @Test
  void updateIsRolledBackWhenAnotherTableFails() {
    author.insert(row("name", "tolkien", "country", "uk"));
    book.insert(row("title", "lord of the rings", "author", "tolkien"));

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

    CompareTools.assertEquals(
        List.of(row("name", "tolkien", "country", "uk")), retrieveRows(author));
    CompareTools.assertEquals(
        List.of(row("title", "lord of the rings", "author", "tolkien")), retrieveRows(book));
  }

  @Test
  void deleteIsRolledBackWhenAnotherTableFails() {
    author.insert(row("name", "tolkien", "country", "uk"));
    book.insert(row("title", "lord of the rings", "author", "tolkien"));

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

    CompareTools.assertEquals(
        List.of(row("name", "tolkien", "country", "uk")), retrieveRows(author));
    CompareTools.assertEquals(
        List.of(row("title", "lord of the rings", "author", "tolkien")), retrieveRows(book));
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
    CompareTools.assertEquals(
        List.of(row("name", "tolkien", "country", "uk")), retrieveRows(author));
    CompareTools.assertEquals(
        List.of(row("title", "lord of the rings", "author", "tolkien")), retrieveRows(book));
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

  private List<Row> retrieveRows(Table table) {
    return table.retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS);
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
