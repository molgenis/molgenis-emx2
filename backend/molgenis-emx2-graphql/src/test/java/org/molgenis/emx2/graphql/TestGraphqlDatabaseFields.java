package org.molgenis.emx2.graphql;

import static org.junit.Assert.*;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_PW_DEFAULT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.GraphQL;
import java.io.IOException;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
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

  @BeforeClass
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
    String result = execute("{Schemas{name}}").at("/data/Schemas").toString();
    assertFalse(result.contains(schemaName + "B"));

    execute(
        "mutation{createSchema(name:\"" + schemaName + "B\", isChangelogEnabled: false){message}}");
    assertNotNull(database.getSchema(schemaName + "B"));
    result = execute("{Schemas{name}}").at("/data/Schemas").toString();
    assertTrue(result.contains(schemaName + "B"));

    execute("mutation{deleteSchema(name:\"" + schemaName + "B\"){message}}");
    assertNull(database.getSchema(schemaName + "B"));
  }

  @Test
  public void testCreateDatabaseSetting() throws IOException {
    String createSettingQuery =
        """
            mutation {
              createSetting(key: "db-key-1", value: "db-value-1" ){
                    message
              }
            }
            """;

    var result = execute(createSettingQuery);

    // verify
    ObjectNode expected =
        new ObjectMapper()
            .readValue(
                "{\"data\":{\"createSetting\":{\"message\":\"Database setting db-key-1 created\"}}}",
                ObjectNode.class);
    assertEquals(expected, result);
  }

  @Test
  public void testDeleteDatabaseSetting() throws IOException {
    String createSettingQuery =
        """
                mutation {
                  deleteSetting(key: "db-key-1"){
                        message
                  }
                }
                """;

    var result = execute(createSettingQuery);

    // verify
    ObjectNode expected =
        new ObjectMapper()
            .readValue(
                "{\"data\":{\"deleteSetting\":{\"message\":\"Database setting db-key-1 deleted\"}}}",
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
    Assert.assertTrue(database.isAdmin());

    if (database.hasUser("pietje")) database.removeUser("pietje");
    execute("mutation{signup(email:\"pietje\",password:\"blaat123\"){message}}");
    assertTrue(database.hasUser("pietje"));
    assertTrue(database.checkUserPassword("pietje", "blaat123"));

    TestCase.assertTrue(
        execute("mutation{signin(email:\"pietje\",password:\"blaat12\"){message}}")
            .at("/data/signin/message")
            .textValue()
            .contains("failed"));
    // still admin
    Assert.assertTrue(database.isAdmin());

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
    database.becomeAdmin();
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
      throw new RuntimeException(result.get("errors").toString());
    }
    return result;
  }
}
