package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;
import org.molgenis.emx2.TablePermission;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlRoleManager;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestGraphqlSchemaMembers {

  private static final String SCHEMA_NAME = "TGraphqlSchemaMembers";
  private static final String TABLE_NAME = "Items";

  private static final String ROLE_ANALYST = "analyst";
  private static final String ROLE_REVIEWER = "reviewer";
  private static final String GROUP_RESEARCH_TEAM = "ResearchTeam";
  private static final String GROUP_TEAM_A = "TeamA";

  private static final String USER_ALICE = "tgm_alice";
  private static final String USER_EDITOR = "tgm_editor";
  private static final String USER_OWNER = "tgm_owner";
  private static final String USER_TARGET = "tgm_target";

  private static final String QUERY_MEMBERS = "{ _schema { members { email role group } } }";
  private static final String QUERY_GROUPS =
      "{ _schema { groups { name members { email role } } } }";

  private static Database database;
  private static Schema schema;
  private static GraphqlExecutor adminExecutor;
  private static SqlRoleManager roleManager;

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    schema = database.createSchema(SCHEMA_NAME);
    schema.create(table(TABLE_NAME).add(column("id").setPkey()).add(column("val")));

    database.setUserPassword(USER_ALICE, USER_ALICE);
    database.setUserPassword(USER_EDITOR, USER_EDITOR);
    database.setUserPassword(USER_OWNER, USER_OWNER);
    if (database.hasUser(USER_TARGET)) {
      database.removeUser(USER_TARGET);
    }
    database.addUser(USER_TARGET);

    schema.addMember(USER_EDITOR, "Editor");
    schema.addMember(USER_OWNER, "Owner");

    roleManager = ((SqlDatabase) database).getRoleManager();
    roleManager.createRole(schema, ROLE_ANALYST, "");
    roleManager.createRole(schema, ROLE_REVIEWER, "");
    roleManager.createGroup(schema, GROUP_RESEARCH_TEAM);
    roleManager.createGroup(schema, GROUP_TEAM_A);

    schema.getTable(TABLE_NAME).getMetadata().setRlsEnabled(true);

    PermissionSet analystPs = new PermissionSet();
    TablePermission analystTp = new TablePermission(TABLE_NAME);
    analystTp.select(SelectScope.ALL);
    analystTp.insert(UpdateScope.NONE);
    analystTp.update(UpdateScope.NONE);
    analystTp.delete(UpdateScope.NONE);
    analystPs.putTable(TABLE_NAME, analystTp);
    roleManager.setPermissions(schema, ROLE_ANALYST, analystPs);

    PermissionSet reviewerPs = new PermissionSet();
    TablePermission reviewerTp = new TablePermission(TABLE_NAME);
    reviewerTp.select(SelectScope.GROUP);
    reviewerTp.insert(UpdateScope.NONE);
    reviewerTp.update(UpdateScope.NONE);
    reviewerTp.delete(UpdateScope.NONE);
    reviewerPs.putTable(TABLE_NAME, reviewerTp);
    roleManager.setPermissions(schema, ROLE_REVIEWER, reviewerPs);

    adminExecutor = new GraphqlExecutor(schema, new TaskServiceInMemory());
  }

  @AfterAll
  static void tearDown() {
    database.dropSchemaIfExists(SCHEMA_NAME);
    if (database.hasUser(USER_TARGET)) {
      database.removeUser(USER_TARGET);
    }
  }

  @Test
  void systemRoleRows_groupIsNull() throws IOException {
    assertFalse(
        findMemberRows(USER_ALICE, null).stream().anyMatch(r -> "Viewer".equals(r.role())),
        "Pre-condition: alice must not already be a Viewer");

    adminExecutor.executeWithoutSession(
        "mutation { change(members: [{user: \""
            + USER_ALICE
            + "\", role: \"Viewer\"}]) { message } }");

    try {
      List<MemberRow> aliceRows = findMemberRows(USER_ALICE, null);
      assertTrue(
          aliceRows.stream().anyMatch(r -> "Viewer".equals(r.role()) && r.group() == null),
          "System-role Viewer grant must appear with group=null in _schema.members");
    } finally {
      schema.removeMember(USER_ALICE);
    }
  }

  @Test
  void customRoleRows_groupSet() throws IOException {
    assertFalse(
        findMemberRows(USER_ALICE, GROUP_RESEARCH_TEAM).stream()
            .anyMatch(r -> ROLE_ANALYST.equals(r.role())),
        "Pre-condition: alice must not already have analyst role in ResearchTeam");

    adminExecutor.executeWithoutSession(
        "mutation { change(members: [{user: \""
            + USER_ALICE
            + "\", role: \""
            + ROLE_ANALYST
            + "\", group: \""
            + GROUP_RESEARCH_TEAM
            + "\"}]) { message } }");

    try {
      List<MemberRow> aliceRows = findMemberRows(USER_ALICE, GROUP_RESEARCH_TEAM);
      assertTrue(
          aliceRows.stream()
              .anyMatch(
                  r -> ROLE_ANALYST.equals(r.role()) && GROUP_RESEARCH_TEAM.equals(r.group())),
          "Custom-role group grant must appear with group="
              + GROUP_RESEARCH_TEAM
              + " in _schema.members");
    } finally {
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_RESEARCH_TEAM, USER_ALICE, ROLE_ANALYST);
    }
  }

  @Test
  void schemaWideCustomGrant_groupIsNull() throws IOException {
    assertFalse(
        findMemberRows(USER_ALICE, null).stream().anyMatch(r -> ROLE_ANALYST.equals(r.role())),
        "Pre-condition: alice must not already have schema-wide analyst role");

    adminExecutor.executeWithoutSession(
        "mutation { change(members: [{user: \""
            + USER_ALICE
            + "\", role: \""
            + ROLE_ANALYST
            + "\"}]) { message } }");

    try {
      List<MemberRow> aliceSchemaWideRows =
          findMemberRows(USER_ALICE, null).stream()
              .filter(r -> ROLE_ANALYST.equals(r.role()))
              .toList();

      assertFalse(aliceSchemaWideRows.isEmpty(), "Schema-wide custom grant must appear in members");
      assertTrue(
          aliceSchemaWideRows.stream().allMatch(r -> r.group() == null),
          "Schema-wide custom grant must have group=null");
    } finally {
      roleManager.revokeRoleFromUser(schema, ROLE_ANALYST, USER_ALICE);
    }
  }

  @Test
  void addGroupMember_sentinelRoleNotInMembers() throws IOException {
    String preMembersJson =
        convertExecutionResultToJson(adminExecutor.executeWithoutSession(QUERY_MEMBERS));
    JsonNode preMembers =
        new ObjectMapper().readTree(preMembersJson).path("data").path("_schema").path("members");
    boolean alreadyMember = false;
    for (JsonNode m : preMembers) {
      if (USER_ALICE.equals(m.path("email").asText())) {
        alreadyMember = true;
        break;
      }
    }
    assertFalse(alreadyMember, "Pre-condition: alice must not already be in _schema.members");

    adminExecutor.executeWithoutSession(
        "mutation { change(groups: [{name: \""
            + GROUP_RESEARCH_TEAM
            + "\", users: [\""
            + USER_ALICE
            + "\"]}]) { message } }");

    try {
      String json =
          convertExecutionResultToJson(adminExecutor.executeWithoutSession(QUERY_MEMBERS));
      JsonNode members =
          new ObjectMapper().readTree(json).path("data").path("_schema").path("members");

      for (JsonNode m : members) {
        if (USER_ALICE.equals(m.path("email").asText())) {
          assertNotEquals(
              "member",
              m.path("role").asText(),
              "Sentinel 'member' role must not appear in _schema.members for alice");
        }
      }

      String groupsJson =
          convertExecutionResultToJson(adminExecutor.executeWithoutSession(QUERY_GROUPS));
      JsonNode groups =
          new ObjectMapper().readTree(groupsJson).path("data").path("_schema").path("groups");
      boolean foundInGroup = false;
      for (JsonNode g : groups) {
        if (GROUP_RESEARCH_TEAM.equals(g.path("name").asText())) {
          for (JsonNode u : g.path("members")) {
            if (USER_ALICE.equals(u.path("email").asText())) {
              foundInGroup = true;
            }
          }
        }
      }
      assertTrue(
          foundInGroup,
          "Alice must still appear in groups[].members after addGroupMember (bare-membership)");
    } finally {
      roleManager.removeGroupMember(schema, GROUP_RESEARCH_TEAM, USER_ALICE);
    }
  }

  @Test
  void changeMember_systemRole_noGroupRequired() throws IOException {
    assertFalse(
        querySystemMembersByRole("Viewer").contains(USER_ALICE),
        "Pre-condition: USER_ALICE must not already be a Viewer");

    String mutation =
        "mutation { change(members: [{user: \""
            + USER_ALICE
            + "\", role: \"Viewer\"}]) { message } }";

    JsonNode result = executeAdmin(mutation);
    assertFalse(result.has("errors"), "System role assignment without group must succeed");
    assertFalse(
        result.at("/data/change/message").asText().isEmpty(),
        "Response must confirm the assignment");

    try {
      List<String> viewerUsers = querySystemMembersByRole("Viewer");
      assertTrue(
          viewerUsers.contains(USER_ALICE),
          "USER_ALICE must appear in _schema.members with Viewer role after assignment");
    } finally {
      schema.removeMember(USER_ALICE);
    }
  }

  @Test
  void changeMember_customRoleWithGroup_succeeds() throws IOException {
    assertFalse(
        queryGroupUsers(GROUP_TEAM_A).contains(USER_ALICE),
        "Pre-condition: USER_ALICE must not already be in TeamA");

    String mutation =
        "mutation { change(members: [{user: \""
            + USER_ALICE
            + "\", role: \""
            + ROLE_REVIEWER
            + "\", group: \""
            + GROUP_TEAM_A
            + "\"}]) { message } }";

    JsonNode result = executeAdmin(mutation);
    assertFalse(result.has("errors"), "Custom role + group assignment must succeed");
    assertTrue(
        result.at("/data/change/message").asText().length() > 0,
        "Response must contain a confirmation message");

    try {
      List<String> groupUsers = queryGroupUsers(GROUP_TEAM_A);
      assertTrue(
          groupUsers.contains(USER_ALICE),
          "USER_ALICE must appear in _schema.groups[TeamA].members after assignment");
    } finally {
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_TEAM_A, USER_ALICE, ROLE_REVIEWER);
    }
  }

  @Test
  void customRoleNoGroup_acceptedAsSchemaWide() throws IOException {
    assertFalse(
        queryGroupUsers(GROUP_TEAM_A).contains(USER_ALICE),
        "Pre-condition: USER_ALICE must not already be in TeamA");

    String mutation =
        "mutation { change(members: [{user: \""
            + USER_ALICE
            + "\", role: \""
            + ROLE_REVIEWER
            + "\"}]) { message } }";

    JsonNode result = executeAdmin(mutation);
    assertFalse(result.has("errors"), "Custom role without group must be accepted as schema-wide");
    assertTrue(
        result.at("/data/change/message").asText().length() > 0,
        "Response must contain a confirmation message");

    try {
      assertFalse(
          queryGroupUsers(GROUP_TEAM_A).contains(USER_ALICE),
          "Schema-wide grant (null group) must not leak USER_ALICE into named group TeamA members");
    } finally {
      roleManager.revokeRoleFromUser(schema, ROLE_REVIEWER, USER_ALICE);
    }
  }

  @Test
  void changeMember_customRoleNoGroup_supersedesGroupScopedRows() throws IOException {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_TEAM_A, USER_ALICE, ROLE_REVIEWER);

    assertTrue(
        queryGroupUsers(GROUP_TEAM_A).contains(USER_ALICE),
        "Pre-condition: USER_ALICE must be in TeamA before schema-wide grant");

    String mutation =
        "mutation { change(members: [{user: \""
            + USER_ALICE
            + "\", role: \""
            + ROLE_REVIEWER
            + "\"}]) { message } }";

    JsonNode result = executeAdmin(mutation);
    assertFalse(result.has("errors"), "Schema-wide grant (no group) must succeed");

    try {
      assertFalse(
          queryGroupUsers(GROUP_TEAM_A).contains(USER_ALICE),
          "Group-bound row must be superseded: USER_ALICE must no longer appear in TeamA after schema-wide grant");

      List<MemberRow> schemaWideRows =
          findMemberRows(USER_ALICE, null).stream()
              .filter(r -> ROLE_REVIEWER.equals(r.role()))
              .toList();
      assertFalse(
          schemaWideRows.isEmpty(),
          "Schema-wide (null-group) row must exist for USER_ALICE after supersede");
    } finally {
      roleManager.revokeRoleFromUser(schema, ROLE_REVIEWER, USER_ALICE);
    }
  }

  @Test
  void changeMember_systemRoleWithGroup_rejected() throws IOException {
    assertFalse(
        querySystemMembersByRole("Editor").contains(USER_ALICE),
        "Pre-condition: USER_ALICE must not already be an Editor");

    String mutation =
        "mutation { change(members: [{user: \""
            + USER_ALICE
            + "\", role: \"Editor\", group: \""
            + GROUP_TEAM_A
            + "\"}]) { message } }";

    MolgenisException thrown =
        assertThrows(
            MolgenisException.class,
            () -> executeAdminExpectError(mutation),
            "System role with group must be rejected");
    assertFalse(
        thrown.getMessage().contains("WrongType") || thrown.getMessage().contains("not in"),
        "Must be a domain-level error, not a GraphQL schema-type error: " + thrown.getMessage());
    assertTrue(
        thrown.getMessage().contains("system") || thrown.getMessage().contains("group"),
        "Error must clarify system roles do not use groups: " + thrown.getMessage());

    List<String> editorUsers = querySystemMembersByRole("Editor");
    assertFalse(
        editorUsers.contains(USER_ALICE),
        "Rejected mutation must not have added USER_ALICE as Editor in _schema.members");
  }

  @Test
  void removeMember_customRoleWithGroup_succeeds() throws IOException {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_TEAM_A, USER_ALICE, ROLE_REVIEWER);

    assertTrue(
        queryGroupUsers(GROUP_TEAM_A).contains(USER_ALICE),
        "Pre-condition: USER_ALICE must be in TeamA before drop");

    String mutation =
        "mutation { drop(members: [{user: \""
            + USER_ALICE
            + "\", role: \""
            + ROLE_REVIEWER
            + "\", group: \""
            + GROUP_TEAM_A
            + "\"}]) { message } }";

    JsonNode result = executeAdmin(mutation);
    assertFalse(result.has("errors"), "drop(members) with custom role + group must succeed");
    assertTrue(
        result.at("/data/drop/message").asText().length() > 0,
        "Response must contain a confirmation message");

    assertFalse(
        queryGroupUsers(GROUP_TEAM_A).contains(USER_ALICE),
        "USER_ALICE must no longer appear in _schema.groups[TeamA].members after drop");
  }

  @Test
  void dropMember_withGroup_leavesOtherGroupMembershipIntact() throws IOException {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_RESEARCH_TEAM, USER_ALICE, ROLE_REVIEWER);
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_TEAM_A, USER_ALICE, ROLE_REVIEWER);

    assertTrue(
        queryGroupUsers(GROUP_RESEARCH_TEAM).contains(USER_ALICE),
        "Pre-condition: USER_ALICE must be in ResearchTeam before drop");
    assertTrue(
        queryGroupUsers(GROUP_TEAM_A).contains(USER_ALICE),
        "Pre-condition: USER_ALICE must be in TeamA before drop");

    String mutation =
        "mutation { drop(members: [{user: \""
            + USER_ALICE
            + "\", role: \""
            + ROLE_REVIEWER
            + "\", group: \""
            + GROUP_TEAM_A
            + "\"}]) { message } }";

    executeAdmin(mutation);

    try {
      assertFalse(
          queryGroupUsers(GROUP_TEAM_A).contains(USER_ALICE),
          "drop(members) with group must remove USER_ALICE from TeamA");
      assertTrue(
          queryGroupUsers(GROUP_RESEARCH_TEAM).contains(USER_ALICE),
          "drop(members) with group must not remove USER_ALICE from ResearchTeam");
    } finally {
      roleManager.removeGroupMembership(
          SCHEMA_NAME, GROUP_RESEARCH_TEAM, USER_ALICE, ROLE_REVIEWER);
    }
  }

  @Test
  void changeMember_editorCannotGrantOwner_rejected() throws IOException {
    assertFalse(
        querySystemMembersByRole("Owner").contains(USER_ALICE),
        "Pre-condition: USER_ALICE must not already be an Owner");

    try {
      database.setActiveUser(USER_EDITOR);
      GraphqlExecutor editorExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      assertFalse(
          changeMutationPresentInSchema(editorExecutor),
          "change mutation must be absent from schema for Editor (defense-in-depth)");
    } finally {
      database.becomeAdmin();
    }

    List<String> ownerUsers = querySystemMembersByRole("Owner");
    assertFalse(
        ownerUsers.contains(USER_ALICE),
        "Rejected escalation must not have added USER_ALICE as Owner in _schema.members");
  }

  @Test
  void changeMember_editorCannotGrantCustomRole_rejected() throws IOException {
    assertFalse(
        queryGroupUsers(GROUP_TEAM_A).contains(USER_ALICE),
        "Pre-condition: USER_ALICE must not already be in TeamA");

    try {
      database.setActiveUser(USER_EDITOR);
      GraphqlExecutor editorExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      assertFalse(
          changeMutationPresentInSchema(editorExecutor),
          "change mutation must be absent from schema for Editor (defense-in-depth)");
    } finally {
      database.becomeAdmin();
    }

    assertFalse(
        queryGroupUsers(GROUP_TEAM_A).contains(USER_ALICE),
        "Rejected escalation must not have added USER_ALICE to TeamA in _schema.groups.members");
  }

  @Test
  void dropMember_editorCannotDropCustomRoleMember_rejected() throws IOException {
    database.becomeAdmin();
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_TEAM_A, USER_ALICE, ROLE_REVIEWER);

    assertTrue(
        queryGroupUsers(GROUP_TEAM_A).contains(USER_ALICE),
        "Pre-condition: USER_ALICE must be in TeamA before attempted drop");

    try {
      database.setActiveUser(USER_EDITOR);
      GraphqlExecutor editorExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      assertFalse(
          dropMutationPresentInSchema(editorExecutor),
          "drop mutation must be absent from schema for Editor (defense-in-depth)");
    } finally {
      database.becomeAdmin();
    }

    try {
      assertTrue(
          queryGroupUsers(GROUP_TEAM_A).contains(USER_ALICE),
          "Rejected drop must leave USER_ALICE still present in TeamA in _schema.groups");
    } finally {
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_TEAM_A, USER_ALICE, ROLE_REVIEWER);
    }
  }

  @Test
  void changeMembers_grantsRole_groupIsNull() throws IOException {
    assertFalse(
        querySystemMembersByRole(Privileges.VIEWER.toString()).contains(USER_ALICE),
        "Pre-condition: USER_ALICE must not already be a Viewer");

    String mutation =
        "mutation{change(members:[{role:\""
            + Privileges.VIEWER.toString()
            + "\",email:\""
            + USER_ALICE
            + "\"}]){status message}}";
    JsonNode result = executeAdmin(mutation);
    assertEquals("SUCCESS", result.at("/data/change/status").asText());

    try {
      List<String> schemaRoles = schema.getInheritedRolesForUser(USER_ALICE);
      assertTrue(
          schemaRoles.stream().anyMatch(r -> r.contains(Privileges.VIEWER.toString())),
          "User must have Viewer role after grant");
    } finally {
      schema.removeMember(USER_ALICE);
    }
  }

  @Test
  void dropMembers_revokesSystemRole() throws IOException {
    schema.addMember(USER_ALICE, Privileges.VIEWER.toString());
    assertTrue(
        schema.getInheritedRolesForUser(USER_ALICE).stream()
            .anyMatch(r -> r.contains(Privileges.VIEWER.toString())),
        "User must be Viewer before drop");

    String mutation =
        "mutation{drop(members:[{user:\""
            + USER_ALICE
            + "\",role:\""
            + Privileges.VIEWER.toString()
            + "\"}]){message}}";
    executeAdmin(mutation);

    List<String> rolesAfter = schema.getInheritedRolesForUser(USER_ALICE);
    assertFalse(
        rolesAfter.stream().anyMatch(r -> r.contains(Privileges.VIEWER.toString())),
        "User must not have Viewer role after drop");
  }

  @Test
  void applyMembersRejectsManagerGrantingManager() {
    database.becomeAdmin();
    schema.addMember(USER_ALICE, Privileges.MANAGER.toString());
    if (!database.hasUser(USER_TARGET)) {
      database.addUser(USER_TARGET);
    }

    try {
      database.setActiveUser(USER_ALICE);
      GraphqlExecutor aliceExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      String mutation =
          "mutation{change(members:[{role:\""
              + Privileges.MANAGER.toString()
              + "\",email:\""
              + USER_TARGET
              + "\"}]){status message}}";
      Exception thrown =
          assertThrows(
              Exception.class,
              () -> executeAsExpectError(aliceExecutor, mutation),
              "Manager must not be able to grant Manager to another user");
      String message = thrown.getMessage().toLowerCase();
      assertTrue(
          message.contains("escalat") || message.contains("owner") || message.contains("admin"),
          "Error must mention privilege escalation restriction, got: " + message);
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_ALICE);
    }
  }

  @Test
  void applyMembersRejectsManagerGrantingOwner() {
    database.becomeAdmin();
    schema.addMember(USER_ALICE, Privileges.MANAGER.toString());
    if (!database.hasUser(USER_TARGET)) {
      database.addUser(USER_TARGET);
    }

    try {
      database.setActiveUser(USER_ALICE);
      GraphqlExecutor aliceExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      String mutation =
          "mutation{change(members:[{role:\""
              + Privileges.OWNER.toString()
              + "\",email:\""
              + USER_TARGET
              + "\"}]){status message}}";
      Exception thrown =
          assertThrows(
              Exception.class,
              () -> executeAsExpectError(aliceExecutor, mutation),
              "Manager must not be able to grant Owner to another user");
      String message = thrown.getMessage().toLowerCase();
      assertTrue(
          message.contains("escalat") || message.contains("owner") || message.contains("admin"),
          "Error must mention privilege escalation restriction, got: " + message);
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_ALICE);
    }
  }

  @Test
  void applyMembersAcceptsOwnerGrantingManager() throws IOException {
    database.becomeAdmin();
    schema.addMember(USER_ALICE, Privileges.OWNER.toString());
    if (!database.hasUser(USER_TARGET)) {
      database.addUser(USER_TARGET);
    }

    try {
      database.setActiveUser(USER_ALICE);
      GraphqlExecutor aliceExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      String mutation =
          "mutation{change(members:[{role:\""
              + Privileges.MANAGER.toString()
              + "\",email:\""
              + USER_TARGET
              + "\"}]){status message}}";
      JsonNode result = executeQuery(aliceExecutor, mutation);
      assertEquals("SUCCESS", result.at("/change/status").asText());
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_ALICE);
      schema.removeMember(USER_TARGET);
    }
  }

  @Test
  void applyMembersAcceptsManagerGrantingViewer() throws IOException {
    database.becomeAdmin();
    schema.addMember(USER_ALICE, Privileges.MANAGER.toString());
    if (!database.hasUser(USER_TARGET)) {
      database.addUser(USER_TARGET);
    }

    try {
      database.setActiveUser(USER_ALICE);
      GraphqlExecutor aliceExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      String mutation =
          "mutation{change(members:[{role:\""
              + Privileges.VIEWER.toString()
              + "\",email:\""
              + USER_TARGET
              + "\"}]){status message}}";
      JsonNode result = executeQuery(aliceExecutor, mutation);
      assertEquals("SUCCESS", result.at("/change/status").asText());
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_ALICE);
      schema.removeMember(USER_TARGET);
    }
  }

  private List<MemberRow> findMemberRows(String email, String group) {
    try {
      String json =
          convertExecutionResultToJson(adminExecutor.executeWithoutSession(QUERY_MEMBERS));
      JsonNode members =
          new ObjectMapper().readTree(json).path("data").path("_schema").path("members");
      List<MemberRow> result = new ArrayList<>();
      if (members.isArray()) {
        for (JsonNode m : members) {
          String memberEmail = m.path("email").asText();
          if (email.equals(memberEmail)) {
            String role = m.path("role").asText();
            JsonNode groupNode = m.path("group");
            String memberGroup =
                (groupNode.isNull() || groupNode.isMissingNode()) ? null : groupNode.asText();
            if ((group == null && memberGroup == null)
                || (group != null && group.equals(memberGroup))) {
              result.add(new MemberRow(memberEmail, role, memberGroup));
            }
          }
        }
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<String> querySystemMembersByRole(String role) {
    try {
      String json =
          convertExecutionResultToJson(adminExecutor.executeWithoutSession(QUERY_MEMBERS));
      JsonNode members =
          new ObjectMapper().readTree(json).path("data").path("_schema").path("members");
      List<String> users = new ArrayList<>();
      if (members.isArray()) {
        for (JsonNode m : members) {
          if (role.equals(m.path("role").asText())) {
            users.add(m.path("email").asText());
          }
        }
      }
      return users;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<String> queryGroupUsers(String groupName) {
    try {
      String json = convertExecutionResultToJson(adminExecutor.executeWithoutSession(QUERY_GROUPS));
      JsonNode groups =
          new ObjectMapper().readTree(json).path("data").path("_schema").path("groups");
      if (groups.isArray()) {
        for (JsonNode g : groups) {
          if (groupName.equals(g.path("name").asText())) {
            JsonNode membersNode = g.path("members");
            List<String> users = new ArrayList<>();
            if (membersNode.isArray()) {
              for (JsonNode u : membersNode) {
                users.add(u.path("email").asText());
              }
            }
            return users;
          }
        }
      }
      return List.of();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private JsonNode executeAdmin(String query) throws IOException {
    String json = convertExecutionResultToJson(adminExecutor.executeWithoutSession(query));
    return new ObjectMapper().readTree(json);
  }

  private void executeAdminExpectError(String query) throws IOException {
    String json = convertExecutionResultToJson(adminExecutor.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(json);
    if (node.has("errors")) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
  }

  private void executeAsExpectError(GraphqlExecutor exec, String query) throws IOException {
    String json = convertExecutionResultToJson(exec.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(json);
    if (node.has("errors")) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
  }

  private JsonNode executeQuery(GraphqlExecutor exec, String query) throws IOException {
    String json = convertExecutionResultToJson(exec.executeWithoutSession(query));
    JsonNode root = new ObjectMapper().readTree(json);
    assertNull(root.get("errors"), "GraphQL errors: " + root.get("errors"));
    return root.get("data");
  }

  private boolean changeMutationPresentInSchema(GraphqlExecutor exec) throws IOException {
    return mutationFieldPresentInSchema(exec, "change");
  }

  private boolean dropMutationPresentInSchema(GraphqlExecutor exec) throws IOException {
    return mutationFieldPresentInSchema(exec, "drop");
  }

  private boolean mutationFieldPresentInSchema(GraphqlExecutor exec, String fieldName)
      throws IOException {
    String introspection = "{ __schema { mutationType { fields { name } } } }";
    String json = convertExecutionResultToJson(exec.executeWithoutSession(introspection));
    JsonNode fields = new ObjectMapper().readTree(json).at("/data/__schema/mutationType/fields");
    if (!fields.isArray()) return false;
    for (JsonNode field : fields) {
      if (fieldName.equals(field.at("/name").asText())) return true;
    }
    return false;
  }

  private record MemberRow(String email, String role, String group) {}
}
