package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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

public class TestTableQueriesWithInheritance {
  private static final String schemaName = TestTableQueriesWithInheritance.class.getSimpleName();
  private static final String multiParentSchemaName = "MultiParentInheritanceTest";
  private static GraphqlExecutor grapql;
  private static GraphqlExecutor multiParentGrapql;
  private static Database database;
  private static TaskService taskService;
  private static Schema schema;
  private static Schema multiParentSchema;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(schemaName);
    schema.create(table("Person", column("name").setPkey()));
    schema.create(
        table("Employee", column("salary").setType(ColumnType.INT)).setInheritNames("Person"));
    schema.getTable("Employee").insert(row("name", "pooky", "salary", 1000));
    taskService = new TaskServiceInMemory();
    grapql = new GraphqlExecutor(schema, taskService);

    multiParentSchema = database.dropCreateSchema(multiParentSchemaName);
    multiParentSchema.create(
        table("Experiments")
            .add(column("id").setPkey())
            .add(column("name"))
            .add(column("experiment_type").setType(ColumnType.PROFILE).setRequired(true)));
    multiParentSchema.create(
        table("Sampling")
            .setTableType(TableType.BLOCK)
            .setInheritNames("Experiments")
            .add(column("sample_type")));
    multiParentSchema.create(
        table("Sequencing")
            .setTableType(TableType.BLOCK)
            .setInheritNames("Experiments")
            .add(column("library_strategy")));
    multiParentSchema.create(
        table("WGS")
            .setInheritNames("Sampling", "Sequencing")
            .add(column("coverage").setType(ColumnType.INT)));
    multiParentSchema
        .getTable("Experiments")
        .insert(
            row(
                "id", "wgs1",
                "name", "WGS experiment 1",
                "experiment_type", "WGS",
                "sample_type", "blood",
                "library_strategy", "WGS",
                "coverage", 30));
    multiParentGrapql = new GraphqlExecutor(multiParentSchema, taskService);
  }

  @Test
  public void testQueriesIncludingSubclassColumns() throws IOException {
    JsonNode result =
        execute(
            grapql,
            "{Person{name,salary}}"); // note, Person.salary doesn't exist, but Employee.salary does
    assertEquals(1000, result.at("/Person/0/salary").asInt());
  }

  @Test
  public void testMultiParentInheritanceSchemaMetadata() throws IOException {
    JsonNode result = execute(multiParentGrapql, "{_schema{tables{name,inheritNames}}}");
    JsonNode tables = result.at("/_schema/tables");
    assertFalse(tables.isMissingNode(), "tables should be present in schema");

    JsonNode wgsTable = findTableByName(tables, "WGS");
    assertTrue(wgsTable != null, "WGS table should be present");
    JsonNode inheritNames = wgsTable.get("inheritNames");
    assertTrue(inheritNames != null && inheritNames.isArray(), "inheritNames should be an array");
    assertEquals(2, inheritNames.size(), "WGS should have 2 parents");

    boolean hasSampling = false;
    boolean hasSequencing = false;
    for (JsonNode name : inheritNames) {
      if ("Sampling".equals(name.asText())) hasSampling = true;
      if ("Sequencing".equals(name.asText())) hasSequencing = true;
    }
    assertTrue(hasSampling, "WGS should inherit from Sampling");
    assertTrue(hasSequencing, "WGS should inherit from Sequencing");
  }

  @Test
  public void testMultiParentInheritanceDataQuery() throws IOException {
    JsonNode result =
        execute(multiParentGrapql, "{WGS{id,name,sample_type,library_strategy,coverage}}");
    JsonNode wgsRows = result.at("/WGS");
    assertFalse(wgsRows.isMissingNode(), "WGS query result should be present");
    assertEquals(1, wgsRows.size(), "WGS should have 1 row");

    JsonNode row = wgsRows.get(0);
    assertEquals("wgs1", row.at("/id").asText());
    assertEquals("WGS experiment 1", row.at("/name").asText());
    assertEquals("blood", row.at("/sample_type").asText());
    assertEquals("WGS", row.at("/library_strategy").asText());
    assertEquals(30, row.at("/coverage").asInt());
  }

  @Test
  public void testMultiParentInheritanceVisibleViaRootTable() throws IOException {
    JsonNode result = execute(multiParentGrapql, "{Experiments{id,name}}");
    JsonNode rows = result.at("/Experiments");
    assertFalse(rows.isMissingNode(), "Experiments query result should be present");
    assertEquals(1, rows.size(), "Experiments should contain the WGS row via inheritance");
    assertEquals("wgs1", rows.get(0).at("/id").asText());
  }

  private JsonNode findTableByName(JsonNode tables, String name) {
    for (JsonNode table : tables) {
      if (name.equals(table.at("/name").asText())) {
        return table;
      }
    }
    return null;
  }

  private JsonNode execute(GraphqlExecutor executor, String query) throws IOException {
    String result = convertExecutionResultToJson(executor.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return new ObjectMapper().readTree(result).get("data");
  }
}
