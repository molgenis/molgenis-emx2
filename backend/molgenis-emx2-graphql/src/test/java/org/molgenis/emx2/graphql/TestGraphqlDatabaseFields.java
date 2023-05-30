package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_PW_DEFAULT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.GraphQL;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskService;
import org.molgenis.emx2.tasks.TaskServiceInMemory;
import org.molgenis.emx2.utils.EnvironmentProperty;

public class TestGraphqlDatabaseFields {

  private static GraphQL grapql;
  private static Database database;
  private static TaskService taskService;
  private static final String schemaName = "TestGraphqlDatabaseFields";

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    taskService = new TaskServiceInMemory();
    Schema schema = database.dropCreateSchema(schemaName);
    new PetStoreLoader().load(schema, false);
    grapql = new GraphqlApiFactory().createGraphqlForDatabase(database, taskService);
  }

  @Test
  public void testCreateAndDeleteSchema() throws IOException {
    // ensure schema doesn't exist
    if (database.getSchema(schemaName + "B") != null) {
      database.dropSchema(schemaName + "B");
    }

    assertNull(database.getSchema(schemaName + "B"));
    String result = execute("{_schemas{name}}").at("/data/_schemas").toString();
    assertFalse(result.contains(schemaName + "B"));

    execute("mutation{createSchema(name:\"" + schemaName + "B\"){message}}");
    assertNotNull(database.getSchema(schemaName + "B"));
    result = execute("{_schemas{name}}").at("/data/_schemas").toString();
    assertTrue(result.contains(schemaName + "B"));

    execute("mutation{deleteSchema(name:\"" + schemaName + "B\"){message}}");
    assertNull(database.getSchema(schemaName + "B"));
  }

  @Test
  public void testCreateDatabaseSetting() throws IOException {
    String createSettingQuery =
        """
                        mutation {
                          change(settings:{key: "db-key-1", value: "db-value-1" }){
                                message
                          }
                        }
                        """;

    var result = execute(createSettingQuery);

    // verify
    ObjectNode expected =
        new ObjectMapper()
            .readValue(
                "{\"data\":{\"change\":{\"message\":\"Changed setting 'db-key-1'.\"}}}",
                ObjectNode.class);
    assertEquals(expected, result);
  }

  @Test
  public void testDeleteDatabaseSetting() throws IOException {
    String createSettingQuery =
        """
                        mutation {
                          drop(settings:{key: "db-key-1"}){
                                message
                          }
                        }
                        """;

    var result = execute(createSettingQuery);

    // verify
    ObjectNode expected =
        new ObjectMapper()
            .readValue(
                "{\"data\":{\"drop\":{\"message\":\"Dropped setting 'db-key-1'.\"}}}",
                ObjectNode.class);
    assertEquals(expected, result);
  }

  @Test
  public void testRegisterAndLoginUsers() throws IOException {

    // todo: default user should be anonymous?
    assertTrue(database.isAdmin());

    // read admin password from environment if necessary
    String adminPass =
        (String)
            EnvironmentProperty.getParameter(
                org.molgenis.emx2.Constants.MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, STRING);
    execute(
        "mutation { signin(email: \""
            + database.getAdminUserName()
            + "\",password:\""
            + adminPass
            + "\") {message}}");
    assertTrue(database.isAdmin());

    if (database.hasUser("pietje")) database.removeUser("pietje");
    execute("mutation{signup(email:\"pietje\",password:\"blaat123\"){message}}");
    assertTrue(database.hasUser("pietje"));
    assertTrue(database.checkUserPassword("pietje", "blaat123"));

    assertTrue(
        execute("mutation{signin(email:\"pietje\",password:\"blaat12\"){message}}")
            .at("/data/signin/message")
            .textValue()
            .contains("failed"));
    // still admin
    assertTrue(database.isAdmin());

    assertTrue(
        execute("mutation{signin(email:\"pietje\",password:\"blaat123\"){message}}")
            .at("/data/signin/message")
            .textValue()
            .contains("Signed in"));
    assertEquals("pietje", database.getActiveUser());

    assertTrue(
        execute("mutation{changePassword(password:\"blaat124\"){message}}")
            .at("/data/changePassword/message")
            .textValue()
            .contains("Password changed"));
    assertTrue(database.checkUserPassword("pietje", "blaat124"));

    execute("mutation{signout{message}}");
    assertEquals("anonymous", database.getActiveUser());

    // back to superuser
    database.becomeAdmin();
  }

  @Test
  public void testUserSettings() throws IOException {
    try {
      execute(
          "mutation{change(users:{email:\"pietje\", settings: {key: \"mykey\", value:\"myvalue\"}}){message}}");

      // test we can retrieve the setting
      assertEquals(
          "myvalue",
          execute("{_admin{users(email:\"pietje\"){email,settings{key,value}}}}")
              .at("/data/_admin/users/0/settings/0/value")
              .textValue());

      // test that we can this only for 'ourselves'
      database.setActiveUser("pietje");
      execute(
          "mutation{change(users:{email:\"pietje\", settings: {key: \"mykey\", value:\"myvalue\"}}){message}}");
      assertEquals(
          "myvalue",
          execute("{_session{settings{key,value}}}")
              .at("/data/_session/settings/0/value")
              .textValue());

      try {
        execute(
            "mutation{change(users:{email:\"bofke\", settings: {key: \"mykey\", value:\"myvalue\"}}){message}}");
        fail("should have failed to update other user");
      } catch (Exception e) {
        // fails correctly
        assertTrue(e.getMessage().contains("permission denied"));
      }
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  public void testDatabaseVersion() throws IOException {
    String result =
        execute("{_manifest{DatabaseVersion}}").at("/data/_manifest/DatabaseVersion").textValue();
    // should be a number
    assertTrue(Integer.valueOf(result) > 0);
  }

  private JsonNode execute(String query) throws IOException {
    JsonNode result =
        new ObjectMapper().readTree(convertExecutionResultToJson(grapql.execute(query)));
    if (result.get("errors") != null) {
      throw new MolgenisException(result.get("errors").toString());
    }
    return result;
  }
}
