package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestGraphqlAdminFields {

  private static GraphqlExecutor graphql;
  private static Database database;
  private static GraphqlSessionHandlerInterface sessionManager;
  private static final String SCHEMA_NAME = TestGraphqlAdminFields.class.getSimpleName();
  private static final String TEST_PERSOON = "testPersoon";
  private static final String ANOTHER_SCHEMA_NAME =
      TestGraphqlAdminFields.class.getSimpleName() + "2";

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropCreateSchema(SCHEMA_NAME);
    database.dropCreateSchema(ANOTHER_SCHEMA_NAME);

    graphql = new GraphqlExecutor(database, new TaskServiceInMemory());

    sessionManager =
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
  }

  @Test
  void testUsers() {
    // put in transaction so user count is not affected by other operations
    database.tx(
        tdb -> {
          tdb.becomeAdmin();
          tdb.dropCreateSchema(SCHEMA_NAME);

          try {
            JsonNode result = execute("{_admin{users{email} userCount}}");
            assertTrue(result.at("/_admin/userCount").intValue() > 0);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          // test that only admin can do this
          tdb.setActiveUser(ANONYMOUS);
          graphql = new GraphqlExecutor(tdb, new TaskServiceInMemory());

          try {
            assertNull(execute("{_admin{userCount}}").textValue());
          } catch (Exception e) {
            assertTrue(e.getMessage().contains("FieldUndefined"));
          }
          tdb.becomeAdmin();
        });
  }

  @Test
  void testSetUserAdmin() throws JsonProcessingException {
    database.becomeAdmin();
    graphql = new GraphqlExecutor(database, new TaskServiceInMemory());

    // create and sign in user testAdmin
    executeDb("mutation{signup(email:\"testAdmin\",password:\"test123456\"){message}}");
    executeDb("mutation{signin(email:\"testAdmin\",password:\"test123456\"){message}}");

    // give testAdmin user admin privileges
    executeDb(
        """
      mutation {
        updateUser(updateUser:  {
           email: "testAdmin",
           admin: true
        }) {
          message
        }
      }

      """);

    executeDb("mutation{signout{message}}");
    executeDb("mutation{signin(email:\"testAdmin\",password:\"test123456\"){message}}");
    database.setActiveUser("testAdmin");

    // Do an admin only query
    String lastUpdateResult =
        executeDb(
            """
      query {
        _lastUpdate {
          operation
          stamp
          userId
          tableName
          schemaName
        }
      }
      """);
    assertTrue(lastUpdateResult.contains("lastUpdate"));
  }

  private String executeDb(String query) throws JsonProcessingException {
    return convertExecutionResultToJson(graphql.execute(query, null, sessionManager));
  }

  @Test
  void shouldOnlyListTasksAsAdmin() throws JsonProcessingException {
    String query =
        """
        {
          _tasks {
            id
          }
        }
        """;

    sessionManager.createSession(ANONYMOUS);
    MolgenisException exception = assertThrows(MolgenisException.class, () -> executeDb(query));
    assertTrue(
        exception.getMessage().contains("Listing all tasks is only allowed for admin users"));

    sessionManager.createSession(ADMIN_USER);
    assertTrue(executeDb(query).contains("\"_tasks\" : [ ]"));
  }

  @Test
  void testUpdateUser() {
    database.tx(
        testDatabase -> {
          testDatabase.becomeAdmin();
          graphql = new GraphqlExecutor(testDatabase, new TaskServiceInMemory());
          GraphqlExecutor schemaExecutor =
              new GraphqlExecutor(testDatabase.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
          GraphqlExecutor anotherSchemaExecutor =
              new GraphqlExecutor(
                  testDatabase.getSchema(ANOTHER_SCHEMA_NAME), new TaskServiceInMemory());

          try {
            // setup
            testDatabase.addUser(TEST_PERSOON);
            testDatabase.setEnabledUser(TEST_PERSOON, true);
            testDatabase.getSchema(SCHEMA_NAME).addMember(TEST_PERSOON, "Owner");
            testDatabase.getSchema(ANOTHER_SCHEMA_NAME).addMember(TEST_PERSOON, "Viewer");

            // pre-check: user enabled, member of SCHEMA_NAME with Owner, member of
            // ANOTHER_SCHEMA_NAME with Viewer
            JsonNode preUser =
                executeOn(
                    graphql, "{_admin{users(email:\"" + TEST_PERSOON + "\"){email,enabled}}}");
            assertEquals(TEST_PERSOON, preUser.at("/_admin/users/0/email").asText());
            assertTrue(preUser.at("/_admin/users/0/enabled").asBoolean());
            assertTrue(
                memberEmailsOn(schemaExecutor).contains(TEST_PERSOON),
                "Pre-condition: testPersoon must be a member of SCHEMA_NAME");
            assertTrue(
                memberEmailsOn(anotherSchemaExecutor).contains(TEST_PERSOON),
                "Pre-condition: testPersoon must be a member of ANOTHER_SCHEMA_NAME");

            // mutate
            String query =
                "mutation updateUser($updateUser:InputUpdateUser) {updateUser(updateUser:$updateUser){status, message}}";
            Map<String, Object> variables = createUpdateUserVar();
            String queryResult =
                convertExecutionResultToJson(graphql.executeWithoutSession(query, variables));
            JsonNode node = new ObjectMapper().readTree(queryResult);
            if (node.get("errors") != null) {
              throw new MolgenisException(node.get("errors").get(0).get("message").asText());
            }

            // post-check: user disabled
            JsonNode postUser =
                executeOn(
                    graphql, "{_admin{users(email:\"" + TEST_PERSOON + "\"){email,enabled}}}");
            assertEquals(TEST_PERSOON, postUser.at("/_admin/users/0/email").asText());
            assertFalse(postUser.at("/_admin/users/0/enabled").asBoolean());

            // post-check: no longer member of SCHEMA_NAME
            assertTrue(
                memberEmailsOn(schemaExecutor).stream().noneMatch(TEST_PERSOON::equals),
                "testPersoon must not be a member of SCHEMA_NAME after revoke");

            // post-check: now Owner of ANOTHER_SCHEMA_NAME (was Viewer, role updated to Owner)
            JsonNode anotherMembers =
                executeOn(anotherSchemaExecutor, "{_schema{members{email,role}}}");
            boolean hasOwner = false;
            for (JsonNode m : anotherMembers.at("/_schema/members")) {
              if (TEST_PERSOON.equals(m.path("email").asText())
                  && "Owner".equals(m.path("role").asText())) {
                hasOwner = true;
              }
            }
            assertTrue(hasOwner, "testPersoon must be Owner of ANOTHER_SCHEMA_NAME after update");

            // clean up
            testDatabase.removeUser(TEST_PERSOON);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  private JsonNode executeOn(GraphqlExecutor executor, String query) throws IOException {
    String result = convertExecutionResultToJson(executor.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return new ObjectMapper().readTree(result).get("data");
  }

  private List<String> memberEmailsOn(GraphqlExecutor executor) throws IOException {
    JsonNode members = executeOn(executor, "{_schema{members{email}}}").at("/_schema/members");
    List<String> emails = new ArrayList<>();
    for (JsonNode m : members) {
      emails.add(m.path("email").asText());
    }
    return emails;
  }

  @NotNull
  private static Map<String, Object> createUpdateUserVar() {
    Map<String, Object> variables = new HashMap<>();
    Map<String, Object> updateUser = new HashMap<>();
    updateUser.put("email", TEST_PERSOON);
    updateUser.put("password", "12345678");
    updateUser.put("enabled", "false");

    ArrayList<Map<String, String>> revokedRoles = new ArrayList<>();
    Map<String, String> revokedRole = new HashMap<>();
    revokedRole.put("schemaId", SCHEMA_NAME);
    revokedRole.put("role", "Owner");
    revokedRoles.add(revokedRole);
    updateUser.put("revokedRoles", revokedRoles);

    ArrayList<Map<String, String>> roles = new ArrayList<>();
    Map<String, String> role = new HashMap<>();
    role.put("schemaId", ANOTHER_SCHEMA_NAME);
    role.put("role", "Owner");
    roles.add(role);
    updateUser.put("roles", roles);

    variables.put("updateUser", updateUser);
    return variables;
  }

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(graphql.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return new ObjectMapper().readTree(result).get("data");
  }
}
