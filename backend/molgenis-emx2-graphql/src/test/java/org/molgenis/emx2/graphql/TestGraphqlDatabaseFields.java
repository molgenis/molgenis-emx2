package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_PW_DEFAULT;
import static org.molgenis.emx2.tasks.TaskStatus.COMPLETED;
import static org.molgenis.emx2.tasks.TaskStatus.ERROR;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.ExecutionInput;
import graphql.GraphQL;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.Task;
import org.molgenis.emx2.tasks.TaskService;
import org.molgenis.emx2.tasks.TaskServiceInMemory;
import org.molgenis.emx2.tasks.TaskStatus;
import org.molgenis.emx2.utils.EnvironmentProperty;

public class TestGraphqlDatabaseFields {

  private static GraphQL graphql;
  private static Database database;
  private static TaskService taskService;
  private static final String SCHEMA_NAME = TestGraphqlDatabaseFields.class.getSimpleName();

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    taskService = new TaskServiceInMemory();
    graphql = new GraphqlApiFactory().createGraphqlForDatabase(database, taskService);
  }

  @Test
  public void testCreateAndDeleteSchema() throws IOException {
    // ensure schema doesn't exist
    database.dropSchemaIfExists(SCHEMA_NAME + "B");

    assertNull(database.getSchema(SCHEMA_NAME + "B"));
    String result = execute("{_schemas{name}}").at("/data/_schemas").toString();
    assertFalse(result.contains(SCHEMA_NAME + "B"));

    execute("mutation{createSchema(name:\"" + SCHEMA_NAME + "B\"){message}}");
    assertNotNull(database.getSchema(SCHEMA_NAME + "B"));
    result = execute("{_schemas{name}}").at("/data/_schemas").toString();
    assertTrue(result.contains(SCHEMA_NAME + "B"));

    execute("mutation{deleteSchema(id:\"" + SCHEMA_NAME + "B\"){message}}");
    assertNull(database.getSchema(SCHEMA_NAME + "B"));
  }

  @Test
  public void testCreateSchemaFromTemplate() throws IOException, InterruptedException {
    database.dropSchemaIfExists(SCHEMA_NAME + "B");
    assertNull(database.getSchema(SCHEMA_NAME + "B"));
    JsonNode executeResult =
        execute(
            "mutation{createSchema(name:\""
                + SCHEMA_NAME
                + "B\", description: \"test\", template: \"ERN_DASHBOARD\", includeDemoData: false){message taskId}}");
    String taskId = executeResult.get("data").get("createSchema").get("taskId").asText();
    Task mutationTask = taskService.getTask(taskId);
    TaskStatus mutationTaskStatus = mutationTask.getStatus();
    while (mutationTaskStatus != COMPLETED && mutationTaskStatus != ERROR) {
      Thread.sleep(50);
      mutationTaskStatus = mutationTask.getStatus();
    }
    String result = execute("{_schemas{name}}").at("/data/_schemas").toString();
    assertTrue(result.contains(SCHEMA_NAME + "B"));
    assertEquals(9, database.getSchema(SCHEMA_NAME + "B").getTableNames().size());

    execute("mutation{deleteSchema(id:\"" + SCHEMA_NAME + "B\"){message}}");
    assertNull(database.getSchema(SCHEMA_NAME + "B"));
  }

  @Test
  public void testUpdateSchema() throws IOException {
    database.dropSchemaIfExists(SCHEMA_NAME + "B");

    assertNull(database.getSchema(SCHEMA_NAME + "B"));
    String result = execute("{_schemas{name}}").at("/data/_schemas").toString();
    assertFalse(result.contains(SCHEMA_NAME + "B"));

    execute("mutation{createSchema(name:\"" + SCHEMA_NAME + "B\"){message}}");
    assertNotNull(database.getSchema(SCHEMA_NAME + "B"));
    result = execute("{_schemas{name}}").at("/data/_schemas").toString();
    assertTrue(result.contains(SCHEMA_NAME + "B"));

    execute(
        "mutation{updateSchema(name:\""
            + SCHEMA_NAME
            + "B\", description: \"updated description\"){message}}");
    String description = database.getSchemaInfo(SCHEMA_NAME + "B").description();
    assertEquals("updated description", description);

    execute("mutation{deleteSchema(id:\"" + SCHEMA_NAME + "B\"){message}}");
    assertNull(database.getSchema(SCHEMA_NAME + "B"));
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
    GraphqlSessionHandlerInterface sessionManager =
        new GraphqlSessionHandlerInterface() {
          private String user;

          @Override
          public void createSession(String username) {
            this.user = username;
          }

          @Override
          public void destroySession() {
            this.user = null;
          }

          @Override
          public String getCurrentUser() {
            return user;
          }
        };
    execute(
        "mutation { signin(email: \""
            + database.getAdminUserName()
            + "\",password:\""
            + adminPass
            + "\") {message}}",
        sessionManager);
    assertEquals(sessionManager.getCurrentUser(), database.getAdminUserName());

    if (database.hasUser("pietje")) database.removeUser("pietje");
    execute("mutation{signup(email:\"pietje\",password:\"blaat123\"){message}}");
    assertTrue(database.hasUser("pietje"));
    assertTrue(database.checkUserPassword("pietje", "blaat123"));

    assertTrue(
        execute("mutation{signin(email:\"pietje\",password:\"blaat12\"){message}}", sessionManager)
            .at("/data/signin/message")
            .textValue()
            .contains("failed"));
    // still admin
    assertEquals(sessionManager.getCurrentUser(), database.getAdminUserName());

    assertTrue(
        execute("mutation{signin(email:\"pietje\",password:\"blaat123\"){message}}", sessionManager)
            .at("/data/signin/message")
            .textValue()
            .contains("Signed in"));
    assertEquals("pietje", sessionManager.getCurrentUser());

    database.setActiveUser("pietje");
    assertTrue(
        execute("mutation{changePassword(password:\"blaat124\"){message}}")
            .at("/data/changePassword/message")
            .textValue()
            .contains("Password changed"));
    assertTrue(database.checkUserPassword("pietje", "blaat124"));

    execute("mutation{signout{message}}", sessionManager);
    assertNull(sessionManager.getCurrentUser());

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
    assertTrue(Integer.parseInt(result) > 0);
  }

  private JsonNode execute(String query) throws IOException {
    return execute(query, null);
  }

  private JsonNode execute(String query, GraphqlSessionHandlerInterface sessionManager)
      throws IOException {
    Map graphQLContext =
        sessionManager != null
            ? Map.of(GraphqlSessionHandlerInterface.class, sessionManager)
            : Map.of();
    JsonNode result =
        new ObjectMapper()
            .readTree(
                convertExecutionResultToJson(
                    graphql.execute(
                        ExecutionInput.newExecutionInput(query)
                            .graphQLContext(graphQLContext)
                            .build())));
    if (result.get("errors") != null) {
      throw new MolgenisException(result.get("errors").toString());
    }
    return result;
  }
}
