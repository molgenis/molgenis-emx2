package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlRoleManager;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestGraphqlSchemaGroups {

  private static final String SCHEMA_NAME = "TGraphqlSchemaGroups";
  private static final String USER_MANAGER = "grp_manager";
  private static final String USER_EDITOR = "grp_editor";
  private static final String QUERY_GROUPS = "{ _schema { groups { name users { name role } } } }";

  private static final String USER_DUAL_ROLE = "grp_dual_role";
  private static final String GROUP_DUAL = "gamma_dual";
  private static final String ROLE_ONE = "roleOne";
  private static final String ROLE_TWO = "roleTwo";

  private static Database database;
  private static Schema schema;
  private static GraphqlExecutor adminExecutor;
  private static SqlRoleManager roleManager;
  private static DSLContext jooq;

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    schema = database.createSchema(SCHEMA_NAME);

    database.setUserPassword(USER_MANAGER, USER_MANAGER);
    database.setUserPassword(USER_EDITOR, USER_EDITOR);
    database.setUserPassword(USER_DUAL_ROLE, USER_DUAL_ROLE);

    schema.addMember(USER_MANAGER, "Manager");
    schema.addMember(USER_EDITOR, "Editor");

    roleManager = ((SqlDatabase) database).getRoleManager();

    adminExecutor = new GraphqlExecutor(schema, new TaskServiceInMemory());
    jooq = ((SqlDatabase) database).getJooq();
  }

  @AfterAll
  static void tearDown() {
    jooq.execute("DELETE FROM \"MOLGENIS\".groups_metadata WHERE schema = ?", SCHEMA_NAME);
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void createGroup_idempotent_noError() throws IOException {
    assertFalse(
        queryGroupNames().contains("beta"), "Pre-condition: beta must not exist before test");
    executeAdmin("mutation { change(groups: [{name: \"beta\"}]) { message } }");
    assertDoesNotThrow(
        () -> executeAdmin("mutation { change(groups: [{name: \"beta\"}]) { message } }"),
        "Calling change(groups) twice with same name must be idempotent");

    List<String> names = queryGroupNames();
    assertEquals(1, names.stream().filter("beta"::equals).count(), "beta must appear exactly once");
  }

  @Test
  void deleteGroup_nonExistent_returnsError() {
    assertFalse(
        queryGroupNames().contains("no_such_group_xyz"),
        "Pre-condition: no_such_group_xyz must not exist before test");
    MolgenisException thrown =
        assertThrows(
            MolgenisException.class,
            () -> executeAdmin("mutation { drop(groups: [\"no_such_group_xyz\"]) { message } }"));
    assertTrue(
        thrown.getMessage().contains("not found"),
        "Non-existent group delete must produce 'not found' error");

    assertFalse(
        queryGroupNames().contains("no_such_group_xyz"),
        "Failed drop must not create a row for no_such_group_xyz");
  }

  @Test
  void changeGroups_asEditor_denied() {
    assertFalse(
        queryGroupNames().contains("iota"), "Pre-condition: iota must not exist before test");
    try {
      database.setActiveUser(USER_EDITOR);
      GraphqlExecutor editorExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      MolgenisException thrown =
          assertThrows(
              MolgenisException.class,
              () ->
                  executeAs(
                      editorExecutor,
                      "mutation { change(groups: [{name: \"iota\"}]) { message } }"));
      assertTrue(
          thrown.getMessage().contains("Manager") || thrown.getMessage().contains("Owner"),
          "Editor must be denied with Manager/Owner error");
    } finally {
      database.becomeAdmin();
    }

    assertFalse(queryGroupNames().contains("iota"), "Rejected change must not have created iota");
  }

  @Test
  void changeGroups_asManager_fullLifecycle() throws IOException {
    assertFalse(
        queryGroupNames().contains("kappa"), "Pre-condition: kappa must not exist before test");
    try {
      database.setActiveUser(USER_MANAGER);
      GraphqlExecutor managerExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());

      executeAs(managerExecutor, "mutation { change(groups: [{name: \"kappa\"}]) { message } }");
      database.becomeAdmin();
      JsonNode groupsAfterCreate = queryGroupsNode();
      assertTrue(
          groupNodeNames(groupsAfterCreate).contains("kappa"), "kappa must exist after create");
      assertTrue(
          usersForGroup(groupsAfterCreate, "kappa").isEmpty(),
          "kappa must have no users after create");

      database.setActiveUser(USER_MANAGER);
      managerExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      executeAs(
          managerExecutor,
          "mutation { change(groups: [{name: \"kappa\", users: [\""
              + USER_MANAGER
              + "\"]}]) { message } }");
      database.becomeAdmin();
      JsonNode groupsAfterAddUser = queryGroupsNode();
      assertTrue(
          usersForGroup(groupsAfterAddUser, "kappa").contains(USER_MANAGER),
          "kappa must contain USER_MANAGER after add");

      database.setActiveUser(USER_MANAGER);
      managerExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      executeAs(
          managerExecutor,
          "mutation { change(groups: [{name: \"kappa\", users: []}]) { message } }");
      database.becomeAdmin();
      JsonNode groupsAfterClearUsers = queryGroupsNode();
      assertTrue(
          usersForGroup(groupsAfterClearUsers, "kappa").isEmpty(),
          "kappa must have no users after clear");

      database.setActiveUser(USER_MANAGER);
      managerExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      executeAs(managerExecutor, "mutation { drop(groups: [\"kappa\"]) { message } }");
    } finally {
      database.becomeAdmin();
    }

    assertFalse(queryGroupNames().contains("kappa"), "kappa must be absent after drop");
  }

  private List<String> queryGroupNames() {
    JsonNode groups = queryGroupsNode();
    return groupNodeNames(groups);
  }

  private JsonNode queryGroupsNode() {
    try {
      String json = convertExecutionResultToJson(adminExecutor.executeWithoutSession(QUERY_GROUPS));
      return new ObjectMapper().readTree(json).path("data").path("_schema").path("groups");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<String> groupNodeNames(JsonNode groups) {
    List<String> names = new ArrayList<>();
    if (groups.isArray()) {
      for (JsonNode g : groups) {
        names.add(g.path("name").asText());
      }
    }
    return names;
  }

  private List<String> usersForGroup(JsonNode groups, String groupName) {
    List<String> users = new ArrayList<>();
    if (groups.isArray()) {
      for (JsonNode g : groups) {
        if (groupName.equals(g.path("name").asText())) {
          JsonNode usersNode = g.path("users");
          if (usersNode.isArray()) {
            for (JsonNode u : usersNode) {
              users.add(u.path("name").asText());
            }
          }
          break;
        }
      }
    }
    return users;
  }

  private List<Map<String, String>> userRolePairsForGroup(JsonNode groups, String groupName) {
    List<Map<String, String>> pairs = new ArrayList<>();
    if (groups.isArray()) {
      for (JsonNode g : groups) {
        if (groupName.equals(g.path("name").asText())) {
          JsonNode usersNode = g.path("users");
          if (usersNode.isArray()) {
            for (JsonNode u : usersNode) {
              pairs.add(Map.of("name", u.path("name").asText(), "role", u.path("role").asText()));
            }
          }
          break;
        }
      }
    }
    return pairs;
  }

  @Test
  void users_includesRolePerUser() throws IOException {
    roleManager.createRole(SCHEMA_NAME, ROLE_ONE);
    roleManager.createRole(SCHEMA_NAME, ROLE_TWO);
    executeAdmin("mutation { change(groups: [{name: \"" + GROUP_DUAL + "\"}]) { message } }");

    assertTrue(
        userRolePairsForGroup(queryGroupsNode(), GROUP_DUAL).isEmpty(),
        "Pre-condition: " + GROUP_DUAL + " must have no users before test");

    executeAdmin(
        "mutation { change(members: [{user: \""
            + USER_DUAL_ROLE
            + "\", role: \""
            + ROLE_ONE
            + "\", group: \""
            + GROUP_DUAL
            + "\"}]) { message } }");
    executeAdmin(
        "mutation { change(members: [{user: \""
            + USER_DUAL_ROLE
            + "\", role: \""
            + ROLE_TWO
            + "\", group: \""
            + GROUP_DUAL
            + "\"}]) { message } }");

    try {
      List<Map<String, String>> pairs = userRolePairsForGroup(queryGroupsNode(), GROUP_DUAL);

      assertEquals(2, pairs.size(), "groups.users must have two entries for dual-role user");

      boolean hasRoleOne =
          pairs.stream()
              .anyMatch(
                  p -> USER_DUAL_ROLE.equals(p.get("name")) && ROLE_ONE.equals(p.get("role")));
      boolean hasRoleTwo =
          pairs.stream()
              .anyMatch(
                  p -> USER_DUAL_ROLE.equals(p.get("name")) && ROLE_TWO.equals(p.get("role")));

      assertTrue(hasRoleOne, "Must have entry with roleOne");
      assertTrue(hasRoleTwo, "Must have entry with roleTwo");
    } finally {
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_DUAL, USER_DUAL_ROLE, ROLE_ONE);
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_DUAL, USER_DUAL_ROLE, ROLE_TWO);
      executeAdmin("mutation { drop(groups: [\"" + GROUP_DUAL + "\"]) { message } }");
    }
  }

  private void executeAdmin(String query) throws IOException {
    String json = convertExecutionResultToJson(adminExecutor.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(json);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
  }

  private void executeAs(GraphqlExecutor executor, String query) throws IOException {
    String json = convertExecutionResultToJson(executor.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(json);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
  }
}
