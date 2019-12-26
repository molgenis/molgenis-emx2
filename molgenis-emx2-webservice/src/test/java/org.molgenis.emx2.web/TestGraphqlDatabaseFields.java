package org.molgenis.emx2.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.TestDatabaseFactory;

import java.io.IOException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.molgenis.emx2.web.GraphqlApi.convertExecutionResultToJson;

public class TestGraphqlDatabaseFields {

  private static GraphQL grapql;
  private static Database database;
  private static final String schemaName = "TestGraphqlDatabaseFields";

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.createSchema(schemaName);
    PetStoreExample.create(schema.getMetadata());
    grapql = GraphqlApi.createGraphqlForDatabase(database);
  }

  @Test
  public void testCreateAndDeleteSchema() throws IOException {
    int length = execute("{Schemas{name}}").at("/data/Schemas").size();
    execute("mutation{createSchema(name:\"" + schemaName + "B\"){message}}");

    assertEquals(length + 1, database.getSchemaNames().size());

    execute("mutation{deleteSchema(name:\"" + schemaName + "B\"){message}}");
    assertEquals(length, database.getSchemaNames().size());
  }

  @Test
  public void testRegisterAndLoginUsers() throws IOException {

    // todo: default user should be anonymous?
    assertNull(database.getActiveUser());

    // login is admin, todo is to elevate privileges to enable registration
    execute("mutation { login(username: \"admin\",password:\"admin\") {message}}");
    assertEquals("admin", database.getActiveUser());

    execute("mutation{register(username:\"pietje\",password:\"blaat123\"){message}}");
    Assert.assertTrue(database.hasUser("pietje"));

    assertTrue(
        execute("mutation{login(username:\"pietje\",password:\"blaat12\"){message}}")
            .at("/data/login/message")
            .textValue()
            .contains("failed"));
    assertEquals("admin", database.getActiveUser());

    assertTrue(
        execute("mutation{login(username:\"pietje\",password:\"blaat123\"){message}}")
            .at("/data/login/message")
            .textValue()
            .contains("Logged in"));
    assertEquals("pietje", database.getActiveUser());

    execute("mutation{logout{message}}");
    assertEquals("anonymous", database.getActiveUser());

    // back to superuser
    database.clearActiveUser();
  }

  private JsonNode execute(String query) throws IOException {
    return new ObjectMapper().readTree(convertExecutionResultToJson(grapql.execute(query)));
  }
}
