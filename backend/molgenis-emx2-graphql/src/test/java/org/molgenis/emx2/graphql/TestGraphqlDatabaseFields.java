package org.molgenis.emx2.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import junit.framework.TestCase;
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
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.createGraphqlForDatabase;

public class TestGraphqlDatabaseFields {

  private static GraphQL grapql;
  private static Database database;
  private static final String schemaName = "TestGraphqlDatabaseFields";

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.createSchema(schemaName);
    PetStoreExample.create(schema.getMetadata());
    grapql = createGraphqlForDatabase(database);
  }

  @Test
  public void testCreateAndDeleteSchema() throws IOException {

    int realLength = database.getSchemaNames().size();
    int length = execute("{Schemas{name}}").at("/data/Schemas").size();
    assertEquals(realLength, length);
    execute("mutation{createSchema(name:\"" + schemaName + "B\"){message}}");

    Assert.assertEquals(length + 1, database.getSchemaNames().size());

    execute("mutation{deleteSchema(name:\"" + schemaName + "B\"){message}}");
    Assert.assertEquals(length, database.getSchemaNames().size());
  }

  @Test
  public void testRegisterAndLoginUsers() throws IOException {

    // todo: default user should be anonymous?
    assertNull(database.getActiveUser());

    // login is admin
    execute("mutation { signin(email: \"admin\",password:\"admin\") {message}}");
    Assert.assertEquals("admin", database.getActiveUser());

    execute("mutation{signup(email:\"pietje\",password:\"blaat123\"){message}}");
    Assert.assertTrue(database.hasUser("pietje"));

    TestCase.assertTrue(
        execute("mutation{signin(email:\"pietje\",password:\"blaat12\"){message}}")
            .at("/data/signin/message")
            .textValue()
            .contains("failed"));
    Assert.assertEquals("admin", database.getActiveUser());

    TestCase.assertTrue(
        execute("mutation{signin(email:\"pietje\",password:\"blaat123\"){message}}")
            .at("/data/signin/message")
            .textValue()
            .contains("Signed in"));
    Assert.assertEquals("pietje", database.getActiveUser());

    execute("mutation{signout{message}}");
    Assert.assertEquals("anonymous", database.getActiveUser());

    // back to superuser
    database.clearActiveUser();
  }

  private JsonNode execute(String query) throws IOException {
    return new ObjectMapper().readTree(convertExecutionResultToJson(grapql.execute(query)));
  }
}
