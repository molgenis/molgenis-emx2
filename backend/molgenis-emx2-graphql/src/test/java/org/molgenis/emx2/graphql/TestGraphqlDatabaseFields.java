package org.molgenis.emx2.graphql;

import static org.junit.Assert.*;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import java.io.IOException;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestGraphqlDatabaseFields {

  private static GraphQL grapql;
  private static Database database;
  private static final String schemaName = "TestGraphqlDatabaseFields";

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(schemaName);
    PetStoreExample.create(schema.getMetadata());
    grapql = new GraphqlApiFactory().createGraphqlForDatabase(database);
  }

  @Test
  public void testCreateAndDeleteSchema() throws IOException {
    // ensure schema doesn't exist
    if (database.getSchema(schemaName + "B") != null) {
      database.dropSchema(schemaName + "B");
    }

    assertNull(database.getSchema(schemaName + "B"));
    String result = execute("{Schemas{name}}").at("/data/Schemas").toString();
    assertFalse(result.contains(schemaName + "B"));

    execute("mutation{createSchema(name:\"" + schemaName + "B\"){message}}");
    assertNotNull(database.getSchema(schemaName + "B"));
    result = execute("{Schemas{name}}").at("/data/Schemas").toString();
    assertTrue(result.contains(schemaName + "B"));

    execute("mutation{deleteSchema(name:\"" + schemaName + "B\"){message}}");
    assertNull(database.getSchema(schemaName + "B"));
  }

  @Test
  public void testRegisterAndLoginUsers() throws IOException {

    // todo: default user should be anonymous?
    assertNull(database.getActiveUser());
    database.setUserPassword("admin", "admin");

    // login is admin
    execute("mutation { signin(email: \"admin\",password:\"admin\") {message}}");
    Assert.assertEquals("admin", database.getActiveUser());

    if (database.hasUser("pietje")) database.removeUser("pietje");
    execute("mutation{signup(email:\"pietje\",password:\"blaat123\"){message}}");
    assertTrue(database.hasUser("pietje"));
    assertTrue(database.checkUserPassword("pietje", "blaat123"));

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

    TestCase.assertTrue(
        execute("mutation{changePassword(password:\"blaat124\"){message}}")
            .at("/data/changePassword/message")
            .textValue()
            .contains("Password changed"));
    assertTrue(database.checkUserPassword("pietje", "blaat124"));

    execute("mutation{signout{message}}");
    Assert.assertEquals("anonymous", database.getActiveUser());

    // back to superuser
    database.clearActiveUser();
  }

  private JsonNode execute(String query) throws IOException {
    return new ObjectMapper().readTree(convertExecutionResultToJson(grapql.execute(query)));
  }
}
