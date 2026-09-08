package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
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

/**
 * Rows referenced by a mutation may be supplied by that same mutation, in any table order. See <a
 * href="https://github.com/molgenis/molgenis-emx2/issues/6210">issue 6210</a>.
 */
class TestGraphqlMutationRefResolution {

  private static final String SCHEMA_NAME = TestGraphqlMutationRefResolution.class.getSimpleName();

  private static Table pet;
  private static Table category;
  private static Table tag;
  private static GraphqlExecutor graphqlExecutor;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);
    schema.create(
        table("Category", column("name").setPkey()),
        table("Tag", column("name").setPkey()),
        table(
            "Pet",
            column("name").setPkey(),
            column("category", REF).setRefTable("Category"),
            column("tags", REF_ARRAY).setRefTable("Tag")));
    pet = schema.getTable("Pet");
    category = schema.getTable("Category");
    tag = schema.getTable("Tag");
    graphqlExecutor = new GraphqlExecutor(schema);
  }

  @BeforeEach
  void emptyTables() {
    pet.truncate();
    category.truncate();
    tag.truncate();
  }

  @Test
  void refAndRefArrayAreResolvedAgainstRowsFromTheSameMutation() throws IOException {
    // the tables of a mutation are walked in an arbitrary order, so the referring table may well be
    // inserted before the tables holding the rows it refers to; the mutation runs as one
    // transaction with deferred constraints, so the refs are checked only once all rows are in
    execute(
        """
        mutation {
          insert(
            Pet: { name: "Melman", category: { name: "giraffe" }, tags: [{ name: "cartoon" }] }
            Category: { name: "giraffe" }
            Tag: { name: "cartoon" }
          ) { message }
        }
        """);

    CompareTools.assertEquals(
        List.of(row("name", "giraffe")), category.retrieveRows(EXCLUDE_MG_COLUMNS));
    CompareTools.assertEquals(
        List.of(row("name", "cartoon")), tag.retrieveRows(EXCLUDE_MG_COLUMNS));
    CompareTools.assertEquals(
        List.of(row("name", "Melman", "category", "giraffe", "tags", "cartoon")),
        pet.retrieveRows(EXCLUDE_MG_COLUMNS));
  }

  @Test
  void unresolvableRefArrayRollsBackTheWholeMutation() {
    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                execute(
                    """
                    mutation {
                      insert(
                        Pet: { name: "Melman", category: { name: "giraffe" }, tags: [{ name: "does not exist" }] }
                        Category: { name: "giraffe" }
                      ) { message }
                    }
                    """));
    assertTrue(
        exception.getMessage().contains("does not exist"),
        "expected the failure to name the missing tag but got: " + exception.getMessage());

    CompareTools.assertEquals(List.of(), category.retrieveRows(EXCLUDE_MG_COLUMNS));
    CompareTools.assertEquals(List.of(), pet.retrieveRows(EXCLUDE_MG_COLUMNS));
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
