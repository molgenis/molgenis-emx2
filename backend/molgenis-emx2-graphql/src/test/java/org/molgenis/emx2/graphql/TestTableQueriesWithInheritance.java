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
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskService;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

public class TestTableQueriesWithInheritance {
  private static final String schemaName = TestTableQueriesWithInheritance.class.getSimpleName();
  private static GraphqlExecutor grapql;
  private static Database database;
  private static TaskService taskService;
  private static Schema schema;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(schemaName);
    schema.create(table("Person", column("name").setPkey()));
    schema.create(
        table("Employee", column("salary").setType(ColumnType.INT)).setInheritName("Person"));
    schema.getTable("Employee").insert(row("name", "pooky", "salary", 1000));

    // base table with two subclasses; only one of them has a file column
    schema.create(table("Component", column("title").setPkey()));
    schema.create(
        table("ImageComponent", column("image").setType(ColumnType.FILE))
            .setInheritName("Component"));
    schema.create(table("TextComponent", column("text")).setInheritName("Component"));
    // insert a row of the sibling subclass that does NOT have the file column
    schema.getTable("TextComponent").insert(row("title", "hello", "text", "world"));

    taskService = new TaskServiceInMemory();
    grapql = new GraphqlExecutor(schema, taskService);
  }

  @Test
  public void testQueriesIncludingSubclassColumns() throws IOException {
    JsonNode result =
        execute(
            "{Person{name,salary}}"); // note, Person.salary doesn't exist, but Employee.salary does
    assertEquals(1000, result.at("/Person/0/salary").asInt());
  }

  @Test
  public void testFileColumnOnSiblingSubclassIsNotEmptyObject() throws IOException {
    JsonNode result = execute("{Component{title, text, image{url}}}");
    JsonNode componentRow = result.at("/Component/0");
    // sanity: the row's own columns are present
    assertEquals("hello", componentRow.get("title").asText());
    assertEquals("world", componentRow.get("text").asText());
    // the file column of the unrelated subclass must be absent, not an empty object
    assertFalse(
        componentRow.has("image"),
        "file field of unrelated subclass should be absent, got: " + componentRow);
    // queried directly on the subclass it is also absent when no file uploaded
    assertTrue(
        execute("{TextComponent{title}}").at("/TextComponent/0/title").asText().equals("hello"));
  }

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(grapql.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return new ObjectMapper().readTree(result).get("data");
  }
}
