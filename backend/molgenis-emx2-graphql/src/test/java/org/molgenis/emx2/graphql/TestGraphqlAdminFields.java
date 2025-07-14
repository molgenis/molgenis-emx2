package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;

import com.fasterxml.jackson.core.JsonProcessingException;
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

public class TestGraphqlAdminFields {

  private static GraphqlSession session;
  private static final String schemaName = TestGraphqlAdminFields.class.getSimpleName();
  private static final String TEST_PERSOON = "testPersoon";
  private static final String ANOTHER_SCHEMA_NAME =
      TestGraphqlAdminFields.class.getSimpleName() + "2";

  @BeforeAll
  public static void setup() {
    // using session so we can test its full behavior
    session = new GraphqlSession(ANONYMOUS);
  }

  @Test
  void testUsers() throws IOException {
    // put in transaction so user count is not affected by other operations
    session.setSessionUser(ADMIN_USER);
    Database database = session.getDatabase();
    database.dropCreateSchema(schemaName);

    JsonNode result = execute(ADMIN_USER, "{_admin{users{email} userCount}}");
    assertTrue(result.at("/_admin/userCount").intValue() > 0);

    // test only admin can do this
    try {
      assertEquals(null, execute(ANONYMOUS, "{_admin{userCount}}").textValue());
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("FieldUndefined"));
    }
  }

  @Test
  void testUpdateUser() throws JsonProcessingException {
    session.setSessionUser(ADMIN_USER);
    Database testDatabase = session.getDatabase();
    GraphQL graphql = session.getGraphqlForDatabase();

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

  private JsonNode execute(String user, String query) throws IOException {
    GraphQL grapql = session.getGraphqlForDatabase();
    String result = convertExecutionResultToJson(grapql.execute(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return new ObjectMapper().readTree(result).get("data");
  }
}
