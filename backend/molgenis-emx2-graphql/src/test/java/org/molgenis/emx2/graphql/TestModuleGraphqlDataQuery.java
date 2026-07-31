package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.MODULE_ARRAY;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.TableType.MODULE;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskService;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestModuleGraphqlDataQuery {

  private static final String SCHEMA_NAME = TestModuleGraphqlDataQuery.class.getSimpleName();

  private static GraphqlExecutor graphql;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);

    schema.create(
        table("Root")
            .add(column("id").setType(STRING).setPkey())
            .add(column("rootCol").setType(STRING)));
    schema.create(
        table("Mod")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("modCol").setType(STRING)));
    schema
        .getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("Mod"));

    schema
        .getTable("Root")
        .insert(row("id", "activating", "rootCol", "rv", "panels", "Mod", "modCol", "modValue"));
    schema.getTable("Root").insert(row("id", "nonActivating", "rootCol", "rv2"));

    TaskService taskService = new TaskServiceInMemory();
    graphql = new GraphqlExecutor(schema, taskService);
  }

  @Test
  void graphqlQueryReturnsModuleColumnValueForActivatingRow() throws IOException {
    JsonNode result = execute("{Root{id,modCol}}");

    JsonNode rows = result.get("Root");
    assertNotNull(rows, "Root data must be present in GraphQL result");

    boolean foundModValue = false;
    for (JsonNode rowNode : rows) {
      if ("activating".equals(rowNode.get("id").asText())) {
        assertEquals(
            "modValue",
            rowNode.get("modCol").asText(),
            "Activating row must expose module column value");
        foundModValue = true;
      }
    }
    assertTrue(foundModValue, "activating row must be present in result");
  }

  @Test
  void graphqlQueryReturnsNullModuleColumnForNonActivatingRow() throws IOException {
    JsonNode result = execute("{Root{id,modCol}}");

    JsonNode rows = result.get("Root");
    assertNotNull(rows, "Root data must be present in GraphQL result");

    for (JsonNode rowNode : rows) {
      if ("nonActivating".equals(rowNode.get("id").asText())) {
        assertTrue(
            rowNode.get("modCol") == null || rowNode.get("modCol").isNull(),
            "Non-activating row must have null modCol, got: " + rowNode.get("modCol"));
        return;
      }
    }
    fail("nonActivating row must be present in result");
  }

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(graphql.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return new ObjectMapper().readTree(result).get("data");
  }
}
