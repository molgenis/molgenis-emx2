package org.molgenis.emx2.web.graphql;

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
import org.molgenis.emx2.web.graphql.GraphqlApi;

import java.io.IOException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.molgenis.emx2.web.graphql.GraphqlApi.convertExecutionResultToJson;

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

    int realLength = database.getSchemaNames().size();
    int length = execute("{Schemas{name}}").at("/data/Schemas").size();
    assertEquals(realLength, length);
    execute("mutation{createSchema(name:\"" + schemaName + "B\"){message}}");

    assertEquals(length + 1, database.getSchemaNames().size());

    execute("mutation{deleteSchema(name:\"" + schemaName + "B\"){message}}");
    assertEquals(length, database.getSchemaNames().size());
  }

  @Test
  public void testRegisterAndLoginUsers() throws IOException {

    // todo: default user should be anonymous?
    assertNull(database.getActiveUser());

    // login is admin
    execute("mutation { signin(email: \"admin\",password:\"admin\") {message}}");
    assertEquals("admin", database.getActiveUser());

    execute("mutation{signup(email:\"pietje\",password:\"blaat123\"){message}}");
    Assert.assertTrue(database.hasUser("pietje"));

    assertTrue(
        execute("mutation{signin(email:\"pietje\",password:\"blaat12\"){message}}")
            .at("/data/signin/message")
            .textValue()
            .contains("failed"));
    assertEquals("admin", database.getActiveUser());

    assertTrue(
        execute("mutation{signin(email:\"pietje\",password:\"blaat123\"){message}}")
            .at("/data/signin/message")
            .textValue()
            .contains("Signed in"));
    assertEquals("pietje", database.getActiveUser());

    execute("mutation{signout{message}}");
    assertEquals("anonymous", database.getActiveUser());

    // back to superuser
    database.clearActiveUser();
  }

  private JsonNode execute(String query) throws IOException {
    return new ObjectMapper().readTree(convertExecutionResultToJson(grapql.execute(query)));
  }
}
