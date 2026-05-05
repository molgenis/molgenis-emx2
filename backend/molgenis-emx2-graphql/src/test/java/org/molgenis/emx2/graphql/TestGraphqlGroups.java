package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestGraphqlGroups {

  private static final String SCHEMA_NAME = "TGraphqlGroups";
  private static final String USER_MANAGER = "grp_manager";
  private static final String USER_EDITOR = "grp_editor";

  private static Database database;
  private static Schema schema;
  private static GraphqlExecutor adminExecutor;
  private static DSLContext jooq;

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    schema = database.createSchema(SCHEMA_NAME);

    database.setUserPassword(USER_MANAGER, USER_MANAGER);
    database.setUserPassword(USER_EDITOR, USER_EDITOR);

    schema.addMember(USER_MANAGER, "Manager");
    schema.addMember(USER_EDITOR, "Editor");

    adminExecutor = new GraphqlExecutor(schema, new TaskServiceInMemory());
    jooq = ((SqlDatabase) database).getJooq();
  }

  @AfterAll
  static void tearDown() {
    jooq.execute("DELETE FROM \"MOLGENIS\".groups_metadata WHERE schema = ?", SCHEMA_NAME);
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void createGroup_roundTrip_rowExistsInGroupsMetadata() throws IOException {
    executeAdmin("mutation { createGroup(name: \"alpha\") { message } }");

    List<Record> rows =
        jooq.fetch(
            "SELECT name FROM \"MOLGENIS\".groups_metadata WHERE schema = ? AND name = ?",
            SCHEMA_NAME,
            "alpha");
    assertEquals(1, rows.size(), "group 'alpha' must exist after createGroup");
  }

  @Test
  void createGroup_duplicate_returnsError() throws IOException {
    executeAdmin("mutation { createGroup(name: \"beta\") { message } }");

    MolgenisException thrown =
        assertThrows(
            MolgenisException.class,
            () -> executeAdmin("mutation { createGroup(name: \"beta\") { message } }"));
    assertTrue(
        thrown.getMessage().contains("already exists"),
        "Duplicate group must produce 'already exists' error");
  }

  @Test
  void addGroupMember_addsUserToUsersArray() throws IOException {
    executeAdmin("mutation { createGroup(name: \"gamma\") { message } }");
    executeAdmin(
        "mutation { addGroupMember(group: \"gamma\", user: \""
            + USER_MANAGER
            + "\") { message } }");

    String[] users = fetchGroupUsers("gamma");
    assertTrue(List.of(users).contains(USER_MANAGER), "Manager must be in group 'gamma'");
  }

  @Test
  void addGroupMember_idempotent_noErrorWhenAlreadyMember() throws IOException {
    executeAdmin("mutation { createGroup(name: \"delta\") { message } }");
    executeAdmin(
        "mutation { addGroupMember(group: \"delta\", user: \""
            + USER_MANAGER
            + "\") { message } }");
    executeAdmin(
        "mutation { addGroupMember(group: \"delta\", user: \""
            + USER_MANAGER
            + "\") { message } }");

    String[] users = fetchGroupUsers("delta");
    long count = List.of(users).stream().filter(u -> u.equals(USER_MANAGER)).count();
    assertEquals(1, count, "User must appear exactly once despite double add");
  }

  @Test
  void addGroupMember_nonExistentUser_returnsError() throws IOException {
    executeAdmin("mutation { createGroup(name: \"epsilon\") { message } }");

    MolgenisException thrown =
        assertThrows(
            MolgenisException.class,
            () ->
                executeAdmin(
                    "mutation { addGroupMember(group: \"epsilon\", user: \"no_such_user_xyz\") { message } }"));
    assertTrue(
        thrown.getMessage().contains("does not exist"),
        "Non-existent user must produce 'does not exist' error");
  }

  @Test
  void removeGroupMember_removesUserFromUsersArray() throws IOException {
    executeAdmin("mutation { createGroup(name: \"zeta\") { message } }");
    executeAdmin(
        "mutation { addGroupMember(group: \"zeta\", user: \"" + USER_MANAGER + "\") { message } }");
    executeAdmin(
        "mutation { removeGroupMember(group: \"zeta\", user: \""
            + USER_MANAGER
            + "\") { message } }");

    String[] users = fetchGroupUsers("zeta");
    assertFalse(List.of(users).contains(USER_MANAGER), "Manager must be removed from group 'zeta'");
  }

  @Test
  void removeGroupMember_idempotent_noErrorWhenNotMember() throws IOException {
    executeAdmin("mutation { createGroup(name: \"eta\") { message } }");

    assertDoesNotThrow(
        () ->
            executeAdmin(
                "mutation { removeGroupMember(group: \"eta\", user: \""
                    + USER_MANAGER
                    + "\") { message } }"));
  }

  @Test
  void deleteGroup_removesRowFromGroupsMetadata() throws IOException {
    executeAdmin("mutation { createGroup(name: \"theta\") { message } }");
    executeAdmin("mutation { deleteGroup(name: \"theta\") { message } }");

    List<Record> rows =
        jooq.fetch(
            "SELECT name FROM \"MOLGENIS\".groups_metadata WHERE schema = ? AND name = ?",
            SCHEMA_NAME,
            "theta");
    assertEquals(0, rows.size(), "group 'theta' must not exist after deleteGroup");
  }

  @Test
  void deleteGroup_nonExistent_returnsError() {
    MolgenisException thrown =
        assertThrows(
            MolgenisException.class,
            () ->
                executeAdmin("mutation { deleteGroup(name: \"no_such_group_xyz\") { message } }"));
    assertTrue(
        thrown.getMessage().contains("not found"),
        "Non-existent group delete must produce 'not found' error");
  }

  @Test
  void createGroup_asEditor_denied() {
    try {
      database.setActiveUser(USER_EDITOR);
      GraphqlExecutor editorExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      MolgenisException thrown =
          assertThrows(
              MolgenisException.class,
              () ->
                  executeAs(
                      editorExecutor, "mutation { createGroup(name: \"iota\") { message } }"));
      assertTrue(
          thrown.getMessage().contains("Manager") || thrown.getMessage().contains("Owner"),
          "Editor must be denied with Manager/Owner error");
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  void allMutations_asManager_succeed() throws IOException {
    try {
      database.setActiveUser(USER_MANAGER);
      GraphqlExecutor managerExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());

      executeAs(managerExecutor, "mutation { createGroup(name: \"kappa\") { message } }");
      executeAs(
          managerExecutor,
          "mutation { addGroupMember(group: \"kappa\", user: \""
              + USER_MANAGER
              + "\") { message } }");
      executeAs(
          managerExecutor,
          "mutation { removeGroupMember(group: \"kappa\", user: \""
              + USER_MANAGER
              + "\") { message } }");
      executeAs(managerExecutor, "mutation { deleteGroup(name: \"kappa\") { message } }");
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  void queryGroups_emptySchema_returnsEmptyList() throws IOException {
    database.dropSchemaIfExists("TGraphqlGroupsEmpty");
    Schema emptySchema = database.createSchema("TGraphqlGroupsEmpty");
    try {
      GraphqlExecutor emptyExecutor = new GraphqlExecutor(emptySchema, new TaskServiceInMemory());
      String json =
          convertExecutionResultToJson(
              emptyExecutor.executeWithoutSession("{ _schema { groups { name users } } }"));
      JsonNode groups = new ObjectMapper().readTree(json).at("/data/_schema/groups");
      assertTrue(
          groups.isArray() && groups.size() == 0, "Empty schema must return empty groups array");
    } finally {
      database.dropSchemaIfExists("TGraphqlGroupsEmpty");
    }
  }

  @Test
  void queryGroups_afterCreateGroup_returnsGroupWithEmptyUsers() throws IOException {
    executeAdmin("mutation { createGroup(name: \"lambda\") { message } }");
    try {
      String json =
          convertExecutionResultToJson(
              adminExecutor.executeWithoutSession("{ _schema { groups { name users } } }"));
      JsonNode groups = new ObjectMapper().readTree(json).at("/data/_schema/groups");
      assertTrue(groups.isArray(), "groups must be an array");
      boolean found = false;
      for (JsonNode group : groups) {
        if ("lambda".equals(group.get("name").asText())) {
          assertTrue(
              group.get("users").isArray() && group.get("users").size() == 0,
              "Newly created group must have empty users");
          found = true;
        }
      }
      assertTrue(found, "Group 'lambda' must appear in _schema.groups");
    } finally {
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".groups_metadata WHERE schema = ? AND name = ?",
          SCHEMA_NAME,
          "lambda");
    }
  }

  @Test
  void queryGroups_afterAddGroupMember_returnsGroupWithUser() throws IOException {
    executeAdmin("mutation { createGroup(name: \"mu\") { message } }");
    executeAdmin(
        "mutation { addGroupMember(group: \"mu\", user: \"" + USER_MANAGER + "\") { message } }");
    try {
      String json =
          convertExecutionResultToJson(
              adminExecutor.executeWithoutSession("{ _schema { groups { name users } } }"));
      JsonNode groups = new ObjectMapper().readTree(json).at("/data/_schema/groups");
      boolean found = false;
      for (JsonNode group : groups) {
        if ("mu".equals(group.get("name").asText())) {
          JsonNode users = group.get("users");
          assertTrue(users.isArray() && users.size() == 1, "Group 'mu' must have exactly one user");
          assertEquals(USER_MANAGER, users.get(0).asText(), "User must be " + USER_MANAGER);
          found = true;
        }
      }
      assertTrue(found, "Group 'mu' must appear in _schema.groups");
    } finally {
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".groups_metadata WHERE schema = ? AND name = ?",
          SCHEMA_NAME,
          "mu");
    }
  }

  @Test
  void queryGroups_multipleGroups_allListed() throws IOException {
    executeAdmin("mutation { createGroup(name: \"nu\") { message } }");
    executeAdmin("mutation { createGroup(name: \"xi\") { message } }");
    try {
      String json =
          convertExecutionResultToJson(
              adminExecutor.executeWithoutSession("{ _schema { groups { name } } }"));
      JsonNode groups = new ObjectMapper().readTree(json).at("/data/_schema/groups");
      List<String> names = new java.util.ArrayList<>();
      for (JsonNode group : groups) {
        names.add(group.get("name").asText());
      }
      assertTrue(names.contains("nu"), "Group 'nu' must be listed");
      assertTrue(names.contains("xi"), "Group 'xi' must be listed");
    } finally {
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".groups_metadata WHERE schema = ? AND name IN ('nu', 'xi')",
          SCHEMA_NAME);
    }
  }

  @Test
  void queryGroups_crossSchemaIsolation_groupsNotLeakedToOtherSchema() throws IOException {
    database.dropSchemaIfExists("TGraphqlGroupsOther");
    Schema otherSchema = database.createSchema("TGraphqlGroupsOther");
    try {
      executeAdmin("mutation { createGroup(name: \"omicron\") { message } }");
      GraphqlExecutor otherExecutor = new GraphqlExecutor(otherSchema, new TaskServiceInMemory());
      String json =
          convertExecutionResultToJson(
              otherExecutor.executeWithoutSession("{ _schema { groups { name } } }"));
      JsonNode groups = new ObjectMapper().readTree(json).at("/data/_schema/groups");
      for (JsonNode group : groups) {
        assertNotEquals(
            "omicron",
            group.get("name").asText(),
            "Group 'omicron' from other schema must not appear in this schema's groups");
      }
    } finally {
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".groups_metadata WHERE schema = ? AND name = ?",
          SCHEMA_NAME,
          "omicron");
      database.dropSchemaIfExists("TGraphqlGroupsOther");
    }
  }

  private String[] fetchGroupUsers(String groupName) {
    Record rec =
        jooq.fetchOne(
            "SELECT users FROM \"MOLGENIS\".groups_metadata WHERE schema = ? AND name = ?",
            SCHEMA_NAME,
            groupName);
    if (rec == null) return new String[] {};
    String[] result = rec.get(0, String[].class);
    return result == null ? new String[] {} : result;
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
