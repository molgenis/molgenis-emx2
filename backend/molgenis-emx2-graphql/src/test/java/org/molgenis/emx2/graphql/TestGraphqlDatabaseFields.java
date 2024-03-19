package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_PW_DEFAULT;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.tasks.TaskServiceInMemory;
import org.molgenis.emx2.utils.EnvironmentProperty;

public class TestGraphqlDatabaseFields {

  private static MolgenisSession session;
  private static final String schemaName = "TestGraphqlDatabaseFields";

  @BeforeAll
  public static void setup() {
    Schema schema = new SqlDatabase(ADMIN_USER).dropCreateSchema(schemaName);
    new PetStoreLoader().load(schema, false);
    session = new MolgenisSession(new TaskServiceInMemory()).setSessionUser(ADMIN_USER);
  }

  @Test
  public void testCreateAndDeleteSchema() throws IOException {
    Database database = new SqlDatabase(ADMIN_USER);
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

    execute("mutation{deleteSchema(id:\"" + schemaName + "B\"){message}}");
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

    // read admin password from environment if necessary
    String adminPass =
        (String)
            EnvironmentProperty.getParameter(
                org.molgenis.emx2.Constants.MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, STRING);
    execute(
        "mutation { signin(email: \""
            + session.getDatabase().getAdminUserName()
            + "\",password:\""
            + adminPass
            + "\") {message}}");
    assertTrue(session.getDatabase().isAdmin());

    if (session.getDatabase().hasUser("pietje")) session.getDatabase().removeUser("pietje");
    execute("mutation{signup(email:\"pietje\",password:\"blaat123\"){message}}");
    assertTrue(session.getDatabase().hasUser("pietje"));
    assertTrue(session.getDatabase().checkUserPassword("pietje", "blaat123"));

    assertTrue(
        execute("mutation{signin(email:\"pietje\",password:\"blaat12\"){message}}")
            .at("/data/signin/message")
            .textValue()
            .contains("failed"));
    // still admin
    assertTrue(session.getDatabase().isAdmin());

    assertTrue(
        execute("mutation{signin(email:\"pietje\",password:\"blaat123\"){message}}")
            .at("/data/signin/message")
            .textValue()
            .contains("Signed in"));
    assertEquals("pietje", session.getDatabase().getActiveUser());

    assertTrue(
        execute("mutation{changePassword(password:\"blaat124\"){message}}")
            .at("/data/changePassword/message")
            .textValue()
            .contains("Password changed"));
    assertTrue(session.getDatabase().checkUserPassword("pietje", "blaat124"));

    execute("mutation{signout{message}}");
    assertEquals("anonymous", session.getDatabase().getActiveUser());
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
      session.setSessionUser("pietje");
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
      session.setSessionUser(ADMIN_USER);
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
        new ObjectMapper()
            .readTree(
                convertExecutionResultToJson(
                    session.getGraphqlForSchema(schemaName).execute(query)));
    if (result.get("errors") != null) {
      throw new MolgenisException(result.get("errors").toString());
    }
    return result;
  }
}
