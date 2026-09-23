package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;
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
  void shouldListCustomRolesPerSchemaForAdmin() {
    database.tx(
        tdb -> {
          tdb.becomeAdmin();
          Schema schema = tdb.dropCreateSchema(SCHEMA_NAME);
          schema.create(
              table("Patient").add(column("id").setPkey()).add(column("name")),
              table("Doctor").add(column("id").setPkey()).add(column("name")));
          schema.createRole("PatientViewer");
          schema.grant(
              "PatientViewer", new TablePermission("Patient").select(true).rowLevel(false));
          graphql = new GraphqlExecutor(tdb, new TaskServiceInMemory());

          try {
            JsonNode schemaRoles =
                execute(
                        "{_admin{schemaRoles{schemaId roleName permissions{table select insert update delete isRowLevel}}}}")
                    .at("/_admin/schemaRoles");

            List<JsonNode> testSchemaRoles = new ArrayList<>();
            for (JsonNode entry : schemaRoles) {
              if (SCHEMA_NAME.equals(entry.get("schemaId").asText())) {
                testSchemaRoles.add(entry);
              }
            }
            assertEquals(1, testSchemaRoles.size());

            JsonNode schemaRole = testSchemaRoles.get(0);
            assertEquals("PatientViewer", schemaRole.get("roleName").asText());
            JsonNode permission = schemaRole.at("/permissions/0");
            assertEquals("Patient", permission.get("table").asText());
            assertTrue(permission.get("select").asBoolean());
            assertFalse(permission.path("insert").asBoolean());
            assertFalse(permission.path("isRowLevel").asBoolean());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }

          tdb.setActiveUser(ANONYMOUS);
          graphql = new GraphqlExecutor(tdb, new TaskServiceInMemory());
          MolgenisException exception =
              assertThrows(
                  MolgenisException.class, () -> execute("{_admin{schemaRoles{schemaId}}}"));
          assertTrue(exception.getMessage().contains("FieldUndefined"));
          tdb.becomeAdmin();
        });
  }

  @Test
  void shouldLimitAndOffsetSchemaRoles() {
    database.tx(
        tdb -> {
          tdb.becomeAdmin();
          Schema schema = tdb.dropCreateSchema(SCHEMA_NAME);
          schema.create(
              table("Patient").add(column("id").setPkey()).add(column("name")),
              table("Doctor").add(column("id").setPkey()).add(column("name")),
              table("Hospital").add(column("name").setPkey()).add(column("city")));
          for (String tableName : List.of("Patient", "Doctor", "Hospital")) {
            schema.createRole(tableName + "Viewer");
            schema.grant(
                tableName + "Viewer", new TablePermission(tableName).select(true).rowLevel(false));
          }
          schema.createRole("RoleWithoutPermissions");
          graphql = new GraphqlExecutor(tdb, new TaskServiceInMemory());

          try {
            JsonNode admin =
                execute("{_admin{schemaRoles(limit: 1000){schemaId roleName} schemaRoleCount}}")
                    .at("/_admin");
            JsonNode all = admin.get("schemaRoles");
            assertEquals(all.size(), admin.get("schemaRoleCount").intValue());

            for (JsonNode schemaRole : all) {
              assertNotEquals("RoleWithoutPermissions", schemaRole.get("roleName").asText());
            }

            JsonNode firstPage =
                execute("{_admin{schemaRoles(limit: 2, offset: 0){schemaId roleName}}}")
                    .at("/_admin/schemaRoles");
            assertEquals(2, firstPage.size());
            assertEquals(all.get(0), firstPage.get(0));
            assertEquals(all.get(1), firstPage.get(1));

            JsonNode secondPage =
                execute("{_admin{schemaRoles(limit: 2, offset: 2){schemaId roleName}}}")
                    .at("/_admin/schemaRoles");
            assertEquals(all.get(2), secondPage.get(0));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  void shouldSearchSchemaRoles() {
    database.tx(
        tdb -> {
          tdb.becomeAdmin();
          Schema schema = tdb.dropCreateSchema(SCHEMA_NAME);
          schema.create(
              table("Patient").add(column("id").setPkey()).add(column("name")),
              table("Hospital").add(column("name").setPkey()).add(column("city")));
          schema.createRole("PatientViewer");
          schema.grant("PatientViewer", new TablePermission("Patient").select(true));
          schema.createRole("HospitalEditor");
          schema.grant("HospitalEditor", new TablePermission("Hospital").insert(true));
          tdb.addUser(TEST_PERSOON);
          schema.addMember(TEST_PERSOON, "HospitalEditor");
          graphql = new GraphqlExecutor(tdb, new TaskServiceInMemory());

          try {
            assertEquals(
                List.of("PatientViewer"),
                roleNames(searchSchemaRoles("patientview")),
                "should match on role name, ignoring case");
            assertEquals(
                List.of("PatientViewer"),
                roleNames(searchSchemaRoles("Patient")),
                "should match on granted table");
            assertEquals(
                List.of("HospitalEditor"),
                roleNames(searchSchemaRoles(TEST_PERSOON)),
                "should match on member");
            assertEquals(
                List.of("HospitalEditor", "PatientViewer"),
                roleNames(searchSchemaRoles(SCHEMA_NAME)).stream().sorted().toList(),
                "should match on schema id");
            assertEquals(0, roleNames(searchSchemaRoles("noSuchRole")).size());

            JsonNode count =
                execute("{_admin{schemaRoleCount(search: \"patientview\")}}")
                    .at("/_admin/schemaRoleCount");
            assertEquals(1, count.intValue());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private JsonNode searchSchemaRoles(String search) throws IOException {
    return execute("{_admin{schemaRoles(search: \"" + search + "\"){schemaId roleName}}}")
        .at("/_admin/schemaRoles");
  }

  private List<String> roleNames(JsonNode schemaRoles) {
    List<String> result = new ArrayList<>();
    for (JsonNode schemaRole : schemaRoles) {
      result.add(schemaRole.get("roleName").asText());
    }
    return result;
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

          try {
            // setup
            testDatabase.addUser(TEST_PERSOON);
            testDatabase.setEnabledUser(TEST_PERSOON, true);
            testDatabase.getSchema(SCHEMA_NAME).addMember(TEST_PERSOON, "Owner");
            testDatabase.getSchema(ANOTHER_SCHEMA_NAME).addMember(TEST_PERSOON, "Viewer");

            // test
            String query =
                "mutation updateUser($updateUser:InputUpdateUser) {updateUser(updateUser:$updateUser){status, message}}";
            Map<String, Object> variables = createUpdateUserVar();
            String queryResult =
                convertExecutionResultToJson(graphql.executeWithoutSession(query, variables));
            JsonNode node = new ObjectMapper().readTree(queryResult);
            if (node.get("errors") != null) {
              throw new MolgenisException(node.get("errors").get(0).get("message").asText());
            }

            // assert results
            User user = testDatabase.getUser(TEST_PERSOON);
            assertEquals("testPersoon", user.getUsername());
            assertFalse(user.getEnabled());

            List<Member> members = testDatabase.getSchema(SCHEMA_NAME).getMembers();
            assertTrue(members.isEmpty());

            Member anotherSchemaMember =
                testDatabase.getSchema(ANOTHER_SCHEMA_NAME).getMembers().stream().findFirst().get();
            assertEquals("Owner", anotherSchemaMember.getRole());
            assertEquals(TEST_PERSOON, anotherSchemaMember.getUser());

            // clean up
            testDatabase.removeUser(TEST_PERSOON);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
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
