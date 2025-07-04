package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.GraphQL;
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

public class TestGraphqlAdminFields {

  private static GraphQL grapql;
  private static Database database;
  private static final String schemaName = TestGraphqlAdminFields.class.getSimpleName();
  private static final String TEST_PERSOON = "testPersoon";
  private static final String ANOTHER_SCHEMA_NAME =
      TestGraphqlAdminFields.class.getSimpleName() + "2";

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  void testUsers() {
    // put in transaction so user count is not affected by other operations
    database.tx(
        tdb -> {
          tdb.becomeAdmin();
          Schema schema = tdb.dropCreateSchema(schemaName);
          grapql = new GraphqlApiFactory().createGraphqlForDatabase(new GraphqlSession(tdb));

          try {
            JsonNode result = execute("{_admin{users{email} userCount}}");
            assertTrue(result.at("/_admin/userCount").intValue() > 0);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          // test that only admin can do this
          tdb.setActiveUser(ANONYMOUS);
          grapql = new GraphqlApiFactory().createGraphqlForDatabase(new GraphqlSession(tdb));

          try {
            assertEquals(null, execute("{_admin{userCount}}").textValue());
          } catch (Exception e) {
            assertTrue(e.getMessage().contains("FieldUndefined"));
          }
          tdb.becomeAdmin();
        });
  }

  @Test
  void testUpdateUser() {
    database.tx(
        testDatabase -> {
          testDatabase.becomeAdmin();
          GraphQL graphql =
              new GraphqlApiFactory().createGraphqlForDatabase(new GraphqlSession(testDatabase));

          try {
            // setup
            testDatabase.dropCreateSchema(schemaName);
            testDatabase.dropCreateSchema(ANOTHER_SCHEMA_NAME);
            testDatabase.addUser(TEST_PERSOON);
            testDatabase.setEnabledUser(TEST_PERSOON, true);
            testDatabase.getSchema(schemaName).addMember(TEST_PERSOON, "Owner");
            testDatabase.getSchema(ANOTHER_SCHEMA_NAME).addMember(TEST_PERSOON, "Viewer");

            // test
            String query =
                "mutation updateUser($updateUser:InputUpdateUser) {updateUser(updateUser:$updateUser){status, message}}";
            Map<String, Object> variables = createUpdateUserVar();
            ExecutionInput build =
                ExecutionInput.newExecutionInput().query(query).variables(variables).build();
            String queryResult = convertExecutionResultToJson(graphql.execute(build));
            JsonNode node = new ObjectMapper().readTree(queryResult);
            if (node.get("errors") != null) {
              throw new MolgenisException(node.get("errors").get(0).get("message").asText());
            }

            // assert results
            User user = testDatabase.getUser(TEST_PERSOON);
            assertEquals("testPersoon", user.getUsername());
            assertFalse(user.getEnabled());

            List<Member> members = testDatabase.getSchema(schemaName).getMembers();
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
    revokedRole.put("schemaId", schemaName);
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
    String result = convertExecutionResultToJson(grapql.execute(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return new ObjectMapper().readTree(result).get("data");
  }
}
