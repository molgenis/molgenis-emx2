package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.Query.Option.EXCLUDE_MG_COLUMNS;
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
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TestGraphqlMutationTransaction {

  private static final String SCHEMA_NAME = TestGraphqlMutationTransaction.class.getSimpleName();

  private static Table author;
  private static Table book;
  private static GraphqlExecutor graphqlExecutor;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);
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

    CompareTools.assertEquals(List.of(), author.retrieveRows(EXCLUDE_MG_COLUMNS));
    CompareTools.assertEquals(List.of(), book.retrieveRows(EXCLUDE_MG_COLUMNS));
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
        List.of(row("name", "tolkien", "country", "uk")), author.retrieveRows(EXCLUDE_MG_COLUMNS));
    CompareTools.assertEquals(List.of(), book.retrieveRows(EXCLUDE_MG_COLUMNS));
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
                        Author: { name: "tolkien", country: "new-zealand" }
                        Book: { title: "lord of the rings", author: { name: "does not exist" } }
                      ) { message }
                    }
                    """));
    assertFailedOnBook(exception);

    CompareTools.assertEquals(
        List.of(row("name", "tolkien", "country", "uk")), author.retrieveRows(EXCLUDE_MG_COLUMNS));
    CompareTools.assertEquals(
        List.of(row("title", "lord of the rings", "author", "tolkien")),
        book.retrieveRows(EXCLUDE_MG_COLUMNS));
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
        List.of(row("name", "tolkien", "country", "uk")), author.retrieveRows(EXCLUDE_MG_COLUMNS));
    CompareTools.assertEquals(
        List.of(row("title", "lord of the rings", "author", "tolkien")),
        book.retrieveRows(EXCLUDE_MG_COLUMNS));
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
        List.of(row("name", "tolkien", "country", "uk")), author.retrieveRows(EXCLUDE_MG_COLUMNS));
    CompareTools.assertEquals(
        List.of(row("title", "lord of the rings", "author", "tolkien")),
        book.retrieveRows(EXCLUDE_MG_COLUMNS));
  }

  @Test
  void mutationWithoutAnyTableFails() {
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> execute("mutation{insert{message}}"));
    assertEquals("None or invalid tables provided", exception.getMessage());
  }

  @Test
  void mutationFieldIsRolledBackWhenAnotherMutationFieldFails() {
    author.insert(row("name", "tolkien", "country", "uk"));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                execute(
                    """
                    mutation {
                      update(Author: { name: "tolkien", country: "new zealand" }) { message }
                      insert(Book: { title: "the hobbit" }) { message }
                      save(Book: { title: "lord of the rings", author: { name: "does not exist" } }) { message }
                    }
                    """));
    assertFailedOnBook(exception);

    CompareTools.assertEquals(
        List.of(row("name", "tolkien", "country", "uk")), author.retrieveRows(EXCLUDE_MG_COLUMNS));
    CompareTools.assertEquals(List.of(), book.retrieveRows(EXCLUDE_MG_COLUMNS));
  }

  @Test
  void multiFieldMutationIsCommittedWhenAllFieldsSucceed() throws IOException {
    author.insert(row("name", "tolkien", "country", "uk"));
    book.insert(row("title", "the hobbit", "author", "tolkien"));

    execute(
        """
        mutation {
          delete(Book: { title: "the hobbit" }) { message }
          update(Author: { name: "tolkien", country: "new zealand" }) { message }
          insert(Book: { title: "lord of the rings", author: { name: "tolkien" } }) { message }
        }
        """);

    CompareTools.assertEquals(
        List.of(row("name", "tolkien", "country", "new zealand")),
        author.retrieveRows(EXCLUDE_MG_COLUMNS));
    CompareTools.assertEquals(
        List.of(row("title", "lord of the rings", "author", "tolkien")),
        book.retrieveRows(EXCLUDE_MG_COLUMNS));
  }

  @Test
  void referencesAreCheckedAtEndOfTransaction() throws IOException {
    execute(
        """
        mutation {
          save(Book: { title: "Matilda", author: { name: "Roald Dahl" } }) { message }
          insert(Author: { name: "Roald Dahl", country: "nk" }) { message }
        }
        """);

    CompareTools.assertEquals(
        List.of(row("name", "Roald Dahl", "country", "nk")),
        author.retrieveRows(EXCLUDE_MG_COLUMNS));
    CompareTools.assertEquals(
        List.of(row("title", "Matilda", "author", "Roald Dahl")),
        book.retrieveRows(EXCLUDE_MG_COLUMNS));
  }

  @Test
  void dataMutationCombinedWithOtherMutationIsRejected() {
    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                execute(
                    """
                    mutation {
                      insert(Author: { name: "tolkien", country: "uk" }) { message }
                      signin(email: "nobody", password: "nothing") { message }
                    }
                    """));
    assertEquals(
        "Data mutations (truncate, insert, save, update, delete) cannot be combined with other mutations: signin",
        exception.getMessage());

    CompareTools.assertEquals(List.of(), author.retrieveRows(EXCLUDE_MG_COLUMNS));
  }

  @Test
  void dataMutationCombinedWithSchemaMutationsIsRejected() {
    schema.create(table("Temporary", column("name").setPkey()));
    try {
      MolgenisException exception =
          assertThrows(
              MolgenisException.class,
              () ->
                  execute(
                      """
                      mutation {
                        drop(tables: ["Temporary"]) { message }
                        change(settings: [{ key: "transaction.test", value: "rejected" }]) { message }
                        insert(Author: { name: "tolkien", country: "uk" }) { message }
                      }
                      """));
      assertEquals(
          "Data mutations (truncate, insert, save, update, delete) cannot be combined with other mutations: drop, change",
          exception.getMessage());

      assertTrue(reloadedSchema().getTableNames().contains("Temporary"));
      assertNull(reloadedSchema().getMetadata().getSetting("transaction.test"));
      CompareTools.assertEquals(List.of(), author.retrieveRows(EXCLUDE_MG_COLUMNS));
    } finally {
      schema.dropTable("Temporary");
    }
  }

  @Test
  void truncateAfterInsertIsCommittedTogether() throws IOException {
    book.insert(row("title", "the hobbit"));

    execute(
        """
        mutation {
          insert(Author: { name: "tolkien", country: "uk" }) { message }
          truncate(tables: ["Book"]) { message }
        }
        """);

    CompareTools.assertEquals(
        List.of(row("name", "tolkien", "country", "uk")), author.retrieveRows(EXCLUDE_MG_COLUMNS));
    CompareTools.assertEquals(List.of(), book.retrieveRows(EXCLUDE_MG_COLUMNS));
  }

  @Test
  void truncateAndInsertAreCommittedTogether() throws IOException {
    author.insert(row("name", "tolkien", "country", "uk"));
    book.insert(row("title", "the hobbit", "author", "tolkien"));

    execute(
        """
        mutation {
          truncate(tables: ["Book"]) { message }
          insert(Book: { title: "lord of the rings", author: { name: "tolkien" } }) { message }
        }
        """);

    CompareTools.assertEquals(
        List.of(row("title", "lord of the rings", "author", "tolkien")),
        book.retrieveRows(EXCLUDE_MG_COLUMNS));
  }

  @Test
  void truncateIsRolledBackWhenDataMutationFails() {
    book.insert(row("title", "the hobbit"));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                execute(
                    """
                    mutation {
                      truncate(tables: ["Book"]) { message }
                      insert(Book: { title: "lord of the rings", author: { name: "does not exist" } }) { message }
                    }
                    """));
    assertFailedOnBook(exception);

    CompareTools.assertEquals(
        List.of(row("title", "the hobbit", "author", null)), book.retrieveRows(EXCLUDE_MG_COLUMNS));
  }

  @Test
  void asyncTruncateCombinedWithOtherMutationIsRejected() {
    book.insert(row("title", "the hobbit"));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                execute(
                    """
                    mutation {
                      truncate(tables: ["Book"], async: true) { message }
                      insert(Author: { name: "tolkien", country: "uk" }) { message }
                    }
                    """));
    assertEquals("Async truncate cannot be combined with other mutations", exception.getMessage());

    CompareTools.assertEquals(List.of(), author.retrieveRows(EXCLUDE_MG_COLUMNS));
    CompareTools.assertEquals(
        List.of(row("title", "the hobbit", "author", null)), book.retrieveRows(EXCLUDE_MG_COLUMNS));
  }

  @Test
  void asyncTruncateCombinedWithOtherMutationInFragmentIsRejected() {
    book.insert(row("title", "the hobbit"));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                execute(
                    """
                    mutation {
                      truncate(tables: ["Book"], async: true) { message }
                      ...insertAuthor
                    }
                    fragment insertAuthor on Save {
                      insert(Author: { name: "tolkien", country: "uk" }) { message }
                    }
                    """));
    assertEquals("Async truncate cannot be combined with other mutations", exception.getMessage());

    CompareTools.assertEquals(List.of(), author.retrieveRows(EXCLUDE_MG_COLUMNS));
    CompareTools.assertEquals(
        List.of(row("title", "the hobbit", "author", null)), book.retrieveRows(EXCLUDE_MG_COLUMNS));
  }

  @Test
  void dataMutationCombinedWithOtherMutationInFragmentsIsRejected() {
    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                execute(
                    """
                    mutation {
                      insert(Author: { name: "tolkien", country: "uk" }) { message }
                      ...signin
                      ... on Save {
                        change(settings: [{ key: "transaction.test", value: "rejected" }]) { message }
                      }
                    }
                    fragment signin on Save {
                      signin(email: "nobody", password: "nothing") { message }
                    }
                    """));
    assertEquals(
        "Data mutations (truncate, insert, save, update, delete) cannot be combined with other mutations: signin, change",
        exception.getMessage());

    assertNull(reloadedSchema().getMetadata().getSetting("transaction.test"));
    CompareTools.assertEquals(List.of(), author.retrieveRows(EXCLUDE_MG_COLUMNS));
  }

  @Test
  void fieldsAfterFailingFieldAreSkipped() {
    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                execute(
                    """
                    mutation {
                      delete(strict: true, Book: { title: "unknown book" }) { message }
                      delete2: delete(strict: true, Author: { name: "unknown author" }) { message }
                    }
                    """));
    assertFailedOnBook(exception);
    assertEquals(0, exception.getSuppressed().length);
  }

  @Test
  void typenameDoesNotDisableTransaction() {
    author.insert(row("name", "tolkien", "country", "uk"));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                execute(
                    """
                    mutation {
                      __typename
                      update(Author: { name: "tolkien", country: "new zealand" }) { message }
                      save(Book: { title: "lord of the rings", author: { name: "does not exist" } }) { message }
                    }
                    """));
    assertFailedOnBook(exception);

    CompareTools.assertEquals(
        List.of(row("name", "tolkien", "country", "uk")), author.retrieveRows(EXCLUDE_MG_COLUMNS));
    CompareTools.assertEquals(List.of(), book.retrieveRows(EXCLUDE_MG_COLUMNS));
  }

  @Test
  void mutationWithoutDataMutationsIsExecuted() throws IOException {
    try {
      execute(
          """
          mutation {
            change(settings: [{ key: "transaction.test", value: "changed" }]) { message }
          }
          """);

      assertEquals("changed", reloadedSchema().getMetadata().getSetting("transaction.test"));
    } finally {
      schema.getMetadata().removeSetting("transaction.test");
    }
  }

  private void assertFailedOnBook(MolgenisException exception) {
    assertTrue(
        exception.getMessage().contains("Book"),
        "expected the failure to originate from table Book but got: " + exception.getMessage());
  }

  private Schema reloadedSchema() {
    Database database = TestDatabaseFactory.getTestDatabase();
    database.clearCache();
    return database.getSchema(SCHEMA_NAME);
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
