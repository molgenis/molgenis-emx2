package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskService;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestModuleScalarGraphql {

  private static final String SCHEMA_NAME = TestModuleScalarGraphql.class.getSimpleName();

  private static GraphqlExecutor graphql;
  private static Schema schema;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(SCHEMA_NAME);

    schema.create(
        table("SRoot")
            .add(column("id").setType(STRING).setPkey())
            .add(column("rootCol").setType(STRING)));
    schema.create(
        table("SMod")
            .setTableType(TableType.MODULE)
            .setInheritNames("SRoot")
            .add(column("modCol").setType(STRING)));
    schema
        .getTable("SRoot")
        .getMetadata()
        .add(column("modType").setType(ColumnType.MODULE).setValues("SMod"));

    schema
        .getTable("SRoot")
        .insert(row("id", "activating", "rootCol", "rv", "modType", "SMod", "modCol", "modValue"));
    schema.getTable("SRoot").insert(row("id", "nonActivating", "rootCol", "rv2"));

    TaskService taskService = new TaskServiceInMemory();
    graphql = new GraphqlExecutor(schema, taskService);
  }

  @Test
  void graphqlQueryReturnsModuleColumnValueForActivatingRow() throws IOException {
    JsonNode result = execute("{SRoot{id,modCol}}");
    JsonNode rows = result.get("SRoot");
    assertNotNull(rows);

    boolean found = false;
    for (JsonNode rowNode : rows) {
      if ("activating".equals(rowNode.get("id").asText())) {
        assertEquals(
            "modValue",
            rowNode.get("modCol").asText(),
            "Activating row must expose module column value");
        found = true;
      }
    }
    assertTrue(found, "activating row must be present in result");
  }

  @Test
  void graphqlQueryReturnsNullModuleColumnForNonActivatingRow() throws IOException {
    JsonNode result = execute("{SRoot{id,modCol}}");
    JsonNode rows = result.get("SRoot");
    assertNotNull(rows);

    for (JsonNode rowNode : rows) {
      if ("nonActivating".equals(rowNode.get("id").asText())) {
        assertTrue(
            rowNode.get("modCol") == null || rowNode.get("modCol").isNull(),
            "Non-activating row must have null modCol");
        return;
      }
    }
    fail("nonActivating row must be present in result");
  }

  @Test
  void graphqlMutationInsertWithScalarModuleColumn() throws IOException {
    executeMutation(
        "mutation ins($rows:[SRootInput]){insert(SRoot:$rows){message}}",
        Map.of(
            "rows",
            List.of(
                Map.of(
                    "id", "mut1",
                    "rootCol", "rv3",
                    "modType", "SMod",
                    "modCol", "mutVal"))));

    String modColValue = findModColById(execute("{SRoot{id,modCol}}"), "mut1");
    assertEquals("mutVal", modColValue, "Scalar MODULE mutation must write and be readable");
  }

  @Test
  void graphqlMutationDeselectingScalarModuleCausesNullProjection() throws IOException {
    executeMutation(
        "mutation ins($rows:[SRootInput]){insert(SRoot:$rows){message}}",
        Map.of(
            "rows",
            List.of(
                Map.of(
                    "id", "desel2",
                    "rootCol", "rv4",
                    "modType", "SMod",
                    "modCol", "willGone"))));

    Map<String, Object> clearRow = new HashMap<>();
    clearRow.put("id", "desel2");
    clearRow.put("modType", null);
    executeMutation(
        "mutation upd($rows:[SRootInput]){update(SRoot:$rows){message}}",
        Map.of("rows", List.of(clearRow)));

    JsonNode modColNode = findModColNodeById(execute("{SRoot{id,modCol}}"), "desel2");
    assertTrue(
        modColNode == null || modColNode.isNull(),
        "After clearing scalar MODULE, module column must be null, got: " + modColNode);
  }

  @Test
  void schemaModuleColumnValuesReflectsDerivedSet() throws IOException {
    Database database = TestDatabaseFactory.getTestDatabase();
    String derivedSchemaName = SCHEMA_NAME + "Derived";
    Schema derivedSchema = database.dropCreateSchema(derivedSchemaName);

    derivedSchema.create(table("DHost").add(column("id").setType(STRING).setPkey()));
    derivedSchema.create(
        table("DMod1")
            .setTableType(TableType.MODULE)
            .setInheritNames("DHost")
            .add(column("dCol1").setType(STRING)));
    derivedSchema.create(
        table("DMod2")
            .setTableType(TableType.MODULE)
            .setInheritNames("DHost")
            .add(column("dCol2").setType(STRING)));
    derivedSchema.getTable("DHost").getMetadata().add(column("modType").setType(ColumnType.MODULE));

    TaskService taskService = new TaskServiceInMemory();
    GraphqlExecutor derivedGraphql = new GraphqlExecutor(derivedSchema, taskService);

    String query = "{_schema{tables{name,columns{name,columnType,values}}}}";
    String result = convertExecutionResultToJson(derivedGraphql.executeWithoutSession(query));
    JsonNode data = new ObjectMapper().readTree(result).get("data");
    assertNotNull(data, "data must not be null");

    JsonNode tables = data.at("/_schema/tables");
    JsonNode dHostTable = null;
    for (JsonNode tableNode : tables) {
      if ("DHost".equals(tableNode.get("name").asText())) {
        dHostTable = tableNode;
        break;
      }
    }
    assertNotNull(dHostTable, "DHost table must be present in _schema");

    JsonNode columns = dHostTable.get("columns");
    JsonNode modTypeCol = null;
    for (JsonNode col : columns) {
      if ("modType".equals(col.get("name").asText())) {
        modTypeCol = col;
        break;
      }
    }
    assertNotNull(modTypeCol, "modType column must be present in _schema");
    assertEquals(
        "MODULE", modTypeCol.get("columnType").asText(), "column type must be MODULE in _schema");

    JsonNode valuesNode = modTypeCol.get("values");
    assertNotNull(valuesNode, "_schema MODULE column values must be non-null (derived set)");
    assertTrue(valuesNode.isArray(), "_schema MODULE column values must be an array");

    List<String> actualValues = new ArrayList<>();
    for (JsonNode valueNode : valuesNode) {
      actualValues.add(valueNode.asText());
    }
    assertTrue(
        actualValues.contains("DMod1"),
        "_schema derived values must contain DMod1, got: " + actualValues);
    assertTrue(
        actualValues.contains("DMod2"),
        "_schema derived values must contain DMod2, got: " + actualValues);

    database.dropSchemaIfExists(derivedSchemaName);
  }

  private String findModColById(JsonNode data, String id) {
    for (JsonNode rowNode : data.get("SRoot")) {
      if (id.equals(rowNode.at("/id").asText())) {
        return rowNode.at("/modCol").asText();
      }
    }
    fail("Row with id=" + id + " not found");
    return null;
  }

  private JsonNode findModColNodeById(JsonNode data, String id) {
    for (JsonNode rowNode : data.get("SRoot")) {
      if (id.equals(rowNode.at("/id").asText())) {
        return rowNode.get("modCol");
      }
    }
    fail("Row with id=" + id + " not found");
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
