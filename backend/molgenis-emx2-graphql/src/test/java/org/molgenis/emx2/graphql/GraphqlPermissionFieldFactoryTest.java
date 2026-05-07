package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlRoleManager;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class GraphqlPermissionFieldFactoryTest {

  private static final SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();

  private static final String SCHEMA_NAME = "GpfTestSchema";
  private static final String TABLE_NAME = "GpfTable";
  private static final String TEST_USER = "gpf_test_user";
  private static final String ROLE_ANALYST = "GpfAnalyst";
  private static final String ROLE_REVIEWER = "GpfReviewer";
  private static final String ROLE_GATED = "GpfGatedRole";

  private GraphqlExecutor executor;
  private SqlRoleManager roleManager;
  private Schema schema;

  @BeforeEach
  void setup() {
    database.becomeAdmin();
    roleManager = database.getRoleManager();

    if (database.getSetting(Constants.MOLGENIS_JWT_SHARED_SECRET) == null) {
      database.setSetting(
          Constants.MOLGENIS_JWT_SHARED_SECRET, "test-jwt-secret-that-is-at-least-32chars");
    }

    if (database.hasUser(TEST_USER)) {
      database.removeUser(TEST_USER);
    }
    database.addUser(TEST_USER);
    database.dropSchemaIfExists(SCHEMA_NAME);
    schema = database.createSchema(SCHEMA_NAME);
    schema.create(
        TableMetadata.table(TABLE_NAME).add(Column.column("id", ColumnType.STRING).setKey(1)));

    executor = new GraphqlExecutor(schema, new TaskServiceInMemory());
  }

  @AfterEach
  void teardown() {
    database.becomeAdmin();
    for (String role : new String[] {ROLE_ANALYST, ROLE_REVIEWER, ROLE_GATED}) {
      try {
        roleManager.deleteRole(SCHEMA_NAME, role);
      } catch (Exception ignored) {
      }
    }
    if (database.hasUser(TEST_USER)) {
      database.removeUser(TEST_USER);
    }
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void sessionPermissions_currentUserSeesOwnPermissions() throws IOException {
    roleManager.createRole(SCHEMA_NAME, ROLE_ANALYST);
    PermissionSet ps = new PermissionSet();
    PermissionSet.TablePermissions tp = new PermissionSet.TablePermissions();
    tp.setSelect(SelectScope.ALL);
    ps.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, ROLE_ANALYST, ps);
    roleManager.grantRoleToUser(schema, ROLE_ANALYST, TEST_USER);

    database.setActiveUser(TEST_USER);
    JsonNode result =
        executeQuery(
            executor, "{_session{tablePermissions{name canView canInsert canUpdate canDelete}}}");
    database.becomeAdmin();
    JsonNode perms = result.at("/_session/tablePermissions");
    assertFalse(perms.isMissingNode(), "tablePermissions must be present");
    assertTrue(perms.isArray(), "tablePermissions must be array");

    boolean found = false;
    for (JsonNode perm : perms) {
      if (TABLE_NAME.equals(perm.at("/name").asText()) && perm.at("/canView").asBoolean()) {
        found = true;
      }
    }
    assertTrue(found, "Expected canView=true permission entry for " + TABLE_NAME);
  }

  @Test
  void rolesQuery_listsRolesAndPermissions() throws IOException {
    database.becomeAdmin();

    roleManager.createRole(SCHEMA_NAME, ROLE_ANALYST);
    roleManager.createRole(SCHEMA_NAME, ROLE_REVIEWER);

    PermissionSet ps = new PermissionSet();
    PermissionSet.TablePermissions tp = new PermissionSet.TablePermissions();
    tp.setSelect(SelectScope.ALL);
    ps.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, ROLE_ANALYST, ps);

    assertTrue(roleManager.roleExists(SCHEMA_NAME, ROLE_ANALYST), "analyst role must exist");
    assertTrue(roleManager.roleExists(SCHEMA_NAME, ROLE_REVIEWER), "reviewer role must exist");

    List<String> roleNames = roleManager.listRoles(SCHEMA_NAME);
    assertTrue(
        roleNames.contains(ROLE_ANALYST), "analyst with permissions must appear in listRoles");
  }

  @Test
  void adminRolesQuery_nonAdminNotAccessible() {
    database.setActiveUser(TEST_USER);
    GraphqlExecutor userExecutor = new GraphqlExecutor(database, new TaskServiceInMemory());

    assertThrows(
        Exception.class,
        () -> userExecutor.executeWithoutSession("{_admin{users{email}}}"),
        "Non-admin must not have access to _admin");
    database.becomeAdmin();
  }

  @Test
  void changeRoleDefinitions_createsRole() throws IOException {
    database.becomeAdmin();

    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_ANALYST
            + "\",description:\"test\"}]){status message}}";
    JsonNode result = executeQuery(executor, mutation);
    assertEquals("SUCCESS", result.at("/change/status").asText());

    assertTrue(
        roleManager.roleExists(SCHEMA_NAME, ROLE_ANALYST), "Role must exist after change(roles)");
  }

  @Test
  void changePermissions_replaceAll() throws IOException {
    database.becomeAdmin();

    roleManager.createRole(SCHEMA_NAME, ROLE_ANALYST);

    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_ANALYST
            + "\",tables:[{table:\""
            + TABLE_NAME
            + "\",select:ALL}]}]){status message}}";
    JsonNode result = executeQuery(executor, mutation);
    assertEquals("SUCCESS", result.at("/change/status").asText());

    PermissionSet perms = roleManager.getPermissions(schema, ROLE_ANALYST);
    PermissionSet.TablePermissions tablePerms = perms.getTables().get(TABLE_NAME);
    assertNotNull(tablePerms, "Table permissions must be present");
    assertEquals(SelectScope.ALL, tablePerms.getSelect(), "select scope must be ALL");
  }

  @Test
  void changeMembers_grantsRole() throws IOException {
    database.becomeAdmin();

    String mutation =
        "mutation{change(members:[{role:\""
            + Privileges.VIEWER.toString()
            + "\",email:\""
            + TEST_USER
            + "\"}]){status message}}";
    JsonNode result = executeQuery(executor, mutation);
    assertEquals("SUCCESS", result.at("/change/status").asText());

    List<String> schemaRoles = schema.getInheritedRolesForUser(TEST_USER);
    assertTrue(
        schemaRoles.stream().anyMatch(r -> r.contains(Privileges.VIEWER.toString())),
        "User must have Viewer role after grant");
  }

  @Test
  void nonAdminForbidden() {
    database.setActiveUser(TEST_USER);

    String mutation = "mutation{change(roles:[{name:\"" + ROLE_ANALYST + "\"}]){status message}}";
    assertThrows(
        Exception.class,
        () -> executeQuery(executor, mutation),
        "Non-member user must not be able to change roles");
    database.becomeAdmin();
  }

  @Test
  void dropRoles_tombstonesRole() throws IOException {
    database.becomeAdmin();

    roleManager.createRole(SCHEMA_NAME, ROLE_ANALYST);

    String mutation = "mutation{drop(roles:[\"" + ROLE_ANALYST + "\"]){message}}";
    executeQuery(executor, mutation);

    assertFalse(
        roleManager.roleExists(SCHEMA_NAME, ROLE_ANALYST), "Role must not exist after drop");
  }

  @Test
  void dropMembers_revokesSystemRole() throws IOException {
    database.becomeAdmin();

    schema.addMember(TEST_USER, Privileges.VIEWER.toString());
    assertTrue(
        schema.getInheritedRolesForUser(TEST_USER).stream()
            .anyMatch(r -> r.contains(Privileges.VIEWER.toString())),
        "User must be Viewer before drop");

    String mutation =
        "mutation{drop(members:[{user:\""
            + TEST_USER
            + "\",role:\""
            + Privileges.VIEWER.toString()
            + "\"}]){message}}";
    executeQuery(executor, mutation);

    List<String> rolesAfter = schema.getInheritedRolesForUser(TEST_USER);
    assertFalse(
        rolesAfter.stream().anyMatch(r -> r.contains(Privileges.VIEWER.toString())),
        "User must not have Viewer role after drop");
  }

  @Test
  void changePermissions_acceptsAggregateSelect() throws IOException {
    database.becomeAdmin();

    roleManager.createRole(SCHEMA_NAME, ROLE_ANALYST);

    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_ANALYST
            + "\",tables:[{table:\""
            + TABLE_NAME
            + "\",select:AGGREGATE}]}]){status message}}";
    JsonNode result = executeQuery(executor, mutation);
    assertEquals("SUCCESS", result.at("/change/status").asText());

    PermissionSet perms = roleManager.getPermissions(schema, ROLE_ANALYST);
    PermissionSet.TablePermissions tablePerms = perms.getTables().get(TABLE_NAME);
    assertNotNull(tablePerms, "Table permissions must be present");
    assertEquals(SelectScope.AGGREGATE, tablePerms.getSelect(), "select scope must be AGGREGATE");
  }

  @Test
  void sessionPermissions_exposesUnifiedSelect() throws IOException {
    roleManager.createRole(SCHEMA_NAME, ROLE_ANALYST);
    PermissionSet ps = new PermissionSet();
    PermissionSet.TablePermissions tp = new PermissionSet.TablePermissions();
    tp.setSelect(SelectScope.AGGREGATE);
    ps.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, ROLE_ANALYST, ps);
    roleManager.grantRoleToUser(schema, ROLE_ANALYST, TEST_USER);

    database.setActiveUser(TEST_USER);
    JsonNode result =
        executeQuery(
            executor, "{_session{tablePermissions{name canView canInsert canUpdate canDelete}}}");
    database.becomeAdmin();
    JsonNode perms = result.at("/_session/tablePermissions");
    assertFalse(perms.isMissingNode(), "tablePermissions must be present");

    boolean found = false;
    for (JsonNode perm : perms) {
      if (TABLE_NAME.equals(perm.at("/name").asText()) && perm.at("/canView").asBoolean()) {
        found = true;
      }
    }
    assertTrue(found, "Expected canView=true for " + TABLE_NAME + " with AGGREGATE select scope");
  }

  @Test
  void setPermissionsRejectsNonManagerNonOwner() {
    database.becomeAdmin();
    roleManager.createRole(SCHEMA_NAME, ROLE_GATED);
    schema.addMember(TEST_USER, Privileges.EDITOR.toString());

    database.setActiveUser(TEST_USER);
    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_GATED
            + "\",tables:[{table:\""
            + TABLE_NAME
            + "\",select:ALL}]}]){status message}}";
    Exception thrown =
        assertThrows(
            Exception.class,
            () -> executeQuery(executor, mutation),
            "Editor must not be able to change role permissions");
    database.becomeAdmin();
    String message = thrown.getMessage().toLowerCase();
    assertTrue(
        message.contains("manager") || message.contains("owner") || message.contains("permission"),
        "Error must mention MANAGER/OWNER or permission denied, got: " + message);
  }

  @Test
  void setPermissionsAcceptsManager() throws IOException {
    database.becomeAdmin();
    roleManager.createRole(SCHEMA_NAME, ROLE_GATED);
    schema.addMember(TEST_USER, Privileges.MANAGER.toString());

    database.setActiveUser(TEST_USER);
    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_GATED
            + "\",tables:[{table:\""
            + TABLE_NAME
            + "\",select:ALL}]}]){status message}}";
    JsonNode result = executeQuery(executor, mutation);
    database.becomeAdmin();
    assertEquals("SUCCESS", result.at("/change/status").asText());
  }

  @Test
  void setPermissionsAcceptsOwner() throws IOException {
    database.becomeAdmin();
    roleManager.createRole(SCHEMA_NAME, ROLE_GATED);
    schema.addMember(TEST_USER, Privileges.OWNER.toString());

    database.setActiveUser(TEST_USER);
    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_GATED
            + "\",tables:[{table:\""
            + TABLE_NAME
            + "\",select:ALL}]}]){status message}}";
    JsonNode result = executeQuery(executor, mutation);
    database.becomeAdmin();
    assertEquals("SUCCESS", result.at("/change/status").asText());
  }

  @Test
  void setPermissionsSchemaIsolation_managerCannotCrossSchemas() throws IOException {
    database.becomeAdmin();
    roleManager.createRole(SCHEMA_NAME, ROLE_GATED);
    schema.addMember(TEST_USER, Privileges.MANAGER.toString());

    database.setActiveUser(TEST_USER);
    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_GATED
            + "\",schemaName:\"*\",tables:[{table:\""
            + TABLE_NAME
            + "\",select:ALL}]}]){status message}}";
    JsonNode result = executeQuery(executor, mutation);
    database.becomeAdmin();
    assertEquals(
        "SUCCESS",
        result.at("/change/status").asText(),
        "schema isolation: schemaName override is silently normalized to current schema");

    PermissionSet perms = roleManager.getPermissions(schema, ROLE_GATED);
    assertNotNull(perms.getTables().get(TABLE_NAME), "Permission must be on current schema only");
  }

  @Test
  void setPermissionsAcceptsWildcardTableForManager() throws IOException {
    database.becomeAdmin();
    roleManager.createRole(SCHEMA_NAME, ROLE_GATED);
    schema.addMember(TEST_USER, Privileges.MANAGER.toString());

    database.setActiveUser(TEST_USER);
    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_GATED
            + "\",tables:[{table:\"*\",select:ALL}]}]){status message}}";
    JsonNode result = executeQuery(executor, mutation);
    database.becomeAdmin();
    assertEquals("SUCCESS", result.at("/change/status").asText());
  }

  @Test
  void setPermissionsAcceptsWildcardTableForOwner() throws IOException {
    database.becomeAdmin();
    roleManager.createRole(SCHEMA_NAME, ROLE_GATED);
    schema.addMember(TEST_USER, Privileges.OWNER.toString());

    database.setActiveUser(TEST_USER);
    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_GATED
            + "\",tables:[{table:\"*\",select:ALL}]}]){status message}}";
    JsonNode result = executeQuery(executor, mutation);
    database.becomeAdmin();
    assertEquals("SUCCESS", result.at("/change/status").asText());
  }

  @Test
  void applyMembersRejectsManagerGrantingManager() {
    database.becomeAdmin();
    schema.addMember(TEST_USER, Privileges.MANAGER.toString());
    database.addUser("gpf_target_user");

    database.setActiveUser(TEST_USER);
    String mutation =
        "mutation{change(members:[{role:\""
            + Privileges.MANAGER.toString()
            + "\",email:\"gpf_target_user\"}]){status message}}";
    Exception thrown =
        assertThrows(
            Exception.class,
            () -> executeQuery(executor, mutation),
            "Manager must not be able to grant Manager to another user");
    database.becomeAdmin();
    String message = thrown.getMessage().toLowerCase();
    assertTrue(
        message.contains("escalat") || message.contains("owner") || message.contains("admin"),
        "Error must mention privilege escalation restriction, got: " + message);
    if (database.hasUser("gpf_target_user")) database.removeUser("gpf_target_user");
  }

  @Test
  void applyMembersRejectsManagerGrantingOwner() {
    database.becomeAdmin();
    schema.addMember(TEST_USER, Privileges.MANAGER.toString());
    database.addUser("gpf_target_user");

    database.setActiveUser(TEST_USER);
    String mutation =
        "mutation{change(members:[{role:\""
            + Privileges.OWNER.toString()
            + "\",email:\"gpf_target_user\"}]){status message}}";
    Exception thrown =
        assertThrows(
            Exception.class,
            () -> executeQuery(executor, mutation),
            "Manager must not be able to grant Owner to another user");
    database.becomeAdmin();
    String message = thrown.getMessage().toLowerCase();
    assertTrue(
        message.contains("escalat") || message.contains("owner") || message.contains("admin"),
        "Error must mention privilege escalation restriction, got: " + message);
    if (database.hasUser("gpf_target_user")) database.removeUser("gpf_target_user");
  }

  @Test
  void applyMembersAcceptsOwnerGrantingManager() throws IOException {
    database.becomeAdmin();
    schema.addMember(TEST_USER, Privileges.OWNER.toString());
    database.addUser("gpf_target_user");

    database.setActiveUser(TEST_USER);
    String mutation =
        "mutation{change(members:[{role:\""
            + Privileges.MANAGER.toString()
            + "\",email:\"gpf_target_user\"}]){status message}}";
    JsonNode result = executeQuery(executor, mutation);
    database.becomeAdmin();
    assertEquals("SUCCESS", result.at("/change/status").asText());
    if (database.hasUser("gpf_target_user")) database.removeUser("gpf_target_user");
  }

  @Test
  void applyMembersAcceptsManagerGrantingViewer() throws IOException {
    database.becomeAdmin();
    schema.addMember(TEST_USER, Privileges.MANAGER.toString());
    database.addUser("gpf_target_user");

    database.setActiveUser(TEST_USER);
    String mutation =
        "mutation{change(members:[{role:\""
            + Privileges.VIEWER.toString()
            + "\",email:\"gpf_target_user\"}]){status message}}";
    JsonNode result = executeQuery(executor, mutation);
    database.becomeAdmin();
    assertEquals("SUCCESS", result.at("/change/status").asText());
    if (database.hasUser("gpf_target_user")) database.removeUser("gpf_target_user");
  }

  private JsonNode executeQuery(GraphqlExecutor exec, String query) throws IOException {
    String json = convertExecutionResultToJson(exec.executeWithoutSession(query));
    JsonNode root = new ObjectMapper().readTree(json);
    assertNull(root.get("errors"), "GraphQL errors: " + root.get("errors"));
    return root.get("data");
  }
}
