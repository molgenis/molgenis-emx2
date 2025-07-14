package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.SqlDatabase;

public class TestTableQueriesWithInheritance {
  private static final String schemaName = TestTableQueriesWithInheritance.class.getSimpleName();
  private static GraphQL grapql;
  private static Database database;
  private static Schema schema;

  @BeforeAll
  public static void setup() {
    database = new SqlDatabase(ADMIN_USER);
    schema = database.dropCreateSchema(schemaName);
    schema.create(table("Person", column("name").setPkey()));
    schema.create(
        table("Employee", column("salary").setType(ColumnType.INT)).setInheritName("Person"));
    schema.getTable("Employee").insert(row("name", "pooky", "salary", 1000));
    grapql = new GraphqlApiFactory().createGraphqlForSchema(schema, new GraphqlSession(ADMIN_USER));
  }

  @Test
  public void testQueriesIncludingSubclassColumns() throws IOException {
    JsonNode result =
        execute(
            "{Person{name,salary}}"); // note, Person.salary doesn't exist, but Employee.salary does
    assertEquals(1000, result.at("/Person/0/salary").asInt());
  }

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(grapql.execute(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return new ObjectMapper().readTree(result).get("data");
  }
}
