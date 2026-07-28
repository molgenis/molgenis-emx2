package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;
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

public class TestGraphqlSchemaTables {

  private static final String PARENT_SCHEMA =
      TestGraphqlSchemaTables.class.getSimpleName() + "Parent";
  private static final String CHILD_SCHEMA =
      TestGraphqlSchemaTables.class.getSimpleName() + "Child";
  private static final String PARENT_TABLE = "Shape";
  private static final String CHILD_TABLE = "MyShape";
  private static final String INHERIT_SCHEMA_NAME = "inheritSchemaName";
  private static final String TABLES_QUERY =
      "{_schema{tables{name,inheritName,inheritSchemaName}}}";

  private static GraphqlExecutor graphql;

  @BeforeAll
  public static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
    database.dropSchemaIfExists(CHILD_SCHEMA);
    database.dropSchemaIfExists(PARENT_SCHEMA);
    Schema parent = database.createSchema(PARENT_SCHEMA);
    parent.create(table(PARENT_TABLE).add(column("name").setPkey()));
    Schema child = database.createSchema(CHILD_SCHEMA);
    child.create(
        table(CHILD_TABLE)
            .setInheritName(PARENT_TABLE)
            .setImportSchema(PARENT_SCHEMA)
            .add(column("surface")));
    child.create(table("Note").add(column("id").setPkey()));
    graphql = new GraphqlExecutor(child);
  }

  @Test
  public void schemaReturnsInheritSchemaName() throws IOException {
    JsonNode child = tableByName(execute(TABLES_QUERY), CHILD_TABLE);
    assertEquals(PARENT_TABLE, child.get("inheritName").asText());
    assertEquals(PARENT_SCHEMA, child.get(INHERIT_SCHEMA_NAME).asText());
  }

  @Test
  public void changeMutationPreservesInheritSchemaName() throws IOException {
    assertEquals(
        PARENT_SCHEMA,
        tableByName(execute(TABLES_QUERY), CHILD_TABLE).get(INHERIT_SCHEMA_NAME).asText());

    execute(
        """
        mutation{change(tables:[
          {name:"MyShape",inheritName:"Shape",inheritSchemaName:"%s",columns:[{name:"surface"}]},
          {name:"Note"}
        ]){message}}"""
            .formatted(PARENT_SCHEMA));

    assertEquals(
        PARENT_SCHEMA,
        tableByName(execute(TABLES_QUERY), CHILD_TABLE).get(INHERIT_SCHEMA_NAME).asText());
  }

  private static JsonNode tableByName(JsonNode data, String tableName) {
    for (JsonNode table : data.at("/_schema/tables")) {
      if (tableName.equals(table.path("name").asText())) {
        return table;
      }
    }
    return fail("table '" + tableName + "' not found in _schema");
  }

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(graphql.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return node.get("data");
  }
}
