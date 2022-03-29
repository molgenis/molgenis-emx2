package org.molgenis.emx2.graphql;

import static org.junit.Assert.*;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_PW_DEFAULT;
import static spark.Service.ignite;

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
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.Task;
import org.molgenis.emx2.tasks.TaskService;
import org.molgenis.emx2.tasks.TaskServiceInMemory;
import org.molgenis.emx2.tasks.TaskStatus;
import org.molgenis.emx2.utils.EnvironmentProperty;
import spark.Service;

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
    PetStoreExample.create(schema.getMetadata());
    grapql = new GraphqlApiFactory().createGraphqlForDatabase(database, taskService);
  }

  @Test
  public void testCreateAndDeleteSchema() throws IOException {
    // ensure schema doesn't exist
    if (database.getSchema(schemaName + "B") != null) {
      database.dropSchema(schemaName + "B");
    }
    if (database.getSchema(schemaName + "C") != null) {
      database.dropSchema(schemaName + "C");
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

    // create schema by sourcing example data from server
    Service http = ignite().port(8082);
    try {
      http.staticFiles.location("/dataexample");
      http.init();
      http.awaitInitialization(); // don't forget this one!
      execute(
          "mutation{createSchema(name:\""
              + schemaName
              + "C\", sourceURLs:\"http://localhost:8082/\"){message}}");
      assertNotNull(database.getSchema(schemaName + "C"));
      assertEquals(
          "a",
          database
              .getSchema(schemaName + "C")
              .getTable("test")
              .retrieveRows()
              .get(0)
              .getString("col1"));

      execute("mutation{deleteSchema(name:\"" + schemaName + "C\"){message}}");
    } finally {
      // close
      http.stop();
    }
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

  @Test
  public void testCreateField() throws IOException, InterruptedException {
    // duplicate test with test io
    String schema1 = TestGraphqlDatabaseFields.class.getSimpleName() + 1;
    String schema2 = TestGraphqlDatabaseFields.class.getSimpleName() + 2;
    database.dropSchemaIfExists(schema1);
    database.dropSchemaIfExists(schema2);

    Service http = ignite().port(8082);
    try {
      // metadata
      http.get(
          "/molgenis.csv",
          (req, res) -> "tableName,columnName,key\n" + "test,col1,1\n" + "test,col2,");
      // data
      http.get("/test.csv", (req, res) -> "col1,col2\ntest,some description");
      http.awaitInitialization();

      String graphql =
          String.format(
              "mutation{create(async:true, schemas:["
                  + "{name:\"%s\",sourceURLs:[\"%s\"]},"
                  + "{name:\"%s\",sourceURLs:[\"%s\"]}"
                  + "]){message,taskId}}",
              schema1, "http://localhost:8082", schema2, "http://localhost:8082");

      String taskId = execute(graphql).at("/data/create/taskId").textValue();

      // wait until complete
      Task task = taskService.getTask(taskId);
      int count = 0;
      while (!TaskStatus.COMPLETED.equals(task.getStatus())) {
        Thread.sleep(500);
        if (count > 10)
          throw new RuntimeException("Import took too long, something is wrong with this test");
      }

      // verification
      assertTrue(database.getSchemaNames().contains(schema1));
      assertTrue(database.getSchemaNames().contains(schema2));
      assertTrue(database.getSchema(schema2).getTableNames().contains("test"));

    } finally {
      http.stop();
    }
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
