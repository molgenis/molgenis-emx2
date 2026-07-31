package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.MODULE_ARRAY;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.TableType.MODULE;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskService;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestModuleGraphqlDataMutation {

  private static final String SCHEMA_NAME = TestModuleGraphqlDataMutation.class.getSimpleName();

  private static GraphqlExecutor graphql;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);

    schema.create(
        table("MutRoot")
            .add(column("id").setType(STRING).setPkey())
            .add(column("rootCol").setType(STRING)));
    schema.create(
        table("MutMod")
            .setTableType(MODULE)
            .setInheritNames("MutRoot")
            .add(column("modCol").setType(STRING)));
    schema
        .getTable("MutRoot")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("MutMod"));

    TaskService taskService = new TaskServiceInMemory();
    graphql = new GraphqlExecutor(schema, taskService);
  }

  @Test
  void insertWithModuleColumnSucceedsAndIsQueryable() throws IOException {
    JsonNode beforeInsert = execute("{MutRoot{id,modCol}}");
    JsonNode beforeRows = beforeInsert.get("MutRoot");
    boolean rowAbsentBefore = true;
    if (beforeRows != null && !beforeRows.isNull()) {
      for (JsonNode row : beforeRows) {
        if ("ins1".equals(row.at("/id").asText())) {
          rowAbsentBefore = false;
          break;
        }
      }
    }
    assertTrue(rowAbsentBefore, "Row ins1 must not exist before insert");

    executeMutation(
        "mutation ins($rows:[MutRootInput]){insert(MutRoot:$rows){message}}",
        Map.of(
            "rows",
            List.of(
                Map.of(
                    "id",
                    "ins1",
                    "rootCol",
                    "rv1",
                    "panels",
                    List.of("MutMod"),
                    "modCol",
                    "hello"))));

    String modColValue = findModColById(execute("{MutRoot{id,modCol}}"), "ins1");
    assertEquals("hello", modColValue, "Module column value must be readable after insert");
  }

  @Test
  void updateModuleColumnValueRoundTrips() throws IOException {
    executeMutation(
        "mutation ins($rows:[MutRootInput]){insert(MutRoot:$rows){message}}",
        Map.of(
            "rows",
            List.of(
                Map.of(
                    "id",
                    "upd1",
                    "rootCol",
                    "rv2",
                    "panels",
                    List.of("MutMod"),
                    "modCol",
                    "first"))));

    assertEquals(
        "first",
        findModColById(execute("{MutRoot{id,modCol}}"), "upd1"),
        "Initial module column value must be readable");

    executeMutation(
        "mutation upd($rows:[MutRootInput]){update(MutRoot:$rows){message}}",
        Map.of(
            "rows",
            List.of(Map.of("id", "upd1", "panels", List.of("MutMod"), "modCol", "second"))));

    assertEquals(
        "second",
        findModColById(execute("{MutRoot{id,modCol}}"), "upd1"),
        "Updated module column value must be readable after update");
  }

  @Test
  void deselectingModuleCausesModuleColumnToReadNull() throws IOException {
    executeMutation(
        "mutation ins($rows:[MutRootInput]){insert(MutRoot:$rows){message}}",
        Map.of(
            "rows",
            List.of(
                Map.of(
                    "id",
                    "desel1",
                    "rootCol",
                    "rv3",
                    "panels",
                    List.of("MutMod"),
                    "modCol",
                    "willBeGone"))));

    assertEquals(
        "willBeGone",
        findModColById(execute("{MutRoot{id,modCol}}"), "desel1"),
        "Module column value must be present before deselect");

    executeMutation(
        "mutation upd($rows:[MutRootInput]){update(MutRoot:$rows){message}}",
        Map.of("rows", List.of(Map.of("id", "desel1", "panels", List.of()))));

    JsonNode modColNode = findModColNodeById(execute("{MutRoot{id,modCol}}"), "desel1");
    assertTrue(
        modColNode == null || modColNode.isNull(),
        "Module column must be null/absent after module deselection, got: " + modColNode);
  }

  private String findModColById(JsonNode data, String id) {
    for (JsonNode row : data.get("MutRoot")) {
      if (id.equals(row.at("/id").asText())) {
        return row.at("/modCol").asText();
      }
    }
    fail("Row with id=" + id + " not found in result");
    return null;
  }

  private JsonNode findModColNodeById(JsonNode data, String id) {
    for (JsonNode row : data.get("MutRoot")) {
      if (id.equals(row.at("/id").asText())) {
        return row.get("modCol");
      }
    }
    fail("Row with id=" + id + " not found in result");
    return null;
  }

  private void executeMutation(String mutation, Map<String, Object> variables) throws IOException {
    String result =
        convertExecutionResultToJson(graphql.executeWithoutSession(mutation, variables));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
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
