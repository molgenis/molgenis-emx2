package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.TablePermission.Select;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlRoleManager;
import org.molgenis.emx2.sql.TestDatabaseFactory;

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

  @BeforeEach
  void setup() {
    database.becomeAdmin();
    roleManager = database.getRoleManager();

    if (database.getSetting(Constants.MOLGENIS_JWT_SHARED_SECRET) == null) {
      database.setSetting(
          Constants.MOLGENIS_JWT_SHARED_SECRET, "test-jwt-secret-that-is-at-least-32chars");
    }

    cleanupRoles();
    cleanupTestUser();

    database.addUser(TEST_USER);
    database.dropCreateSchema(SCHEMA_NAME);
    database
        .getSchema(SCHEMA_NAME)
        .create(
            TableMetadata.table(TABLE_NAME).add(Column.column("id", ColumnType.STRING).setKey(1)));

    executor = new GraphqlExecutor(database);
  }

  @AfterEach
  void teardown() {
    database.becomeAdmin();
    cleanupRoles();
    cleanupTestUser();
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  private void cleanupRoles() {
    for (String role : new String[] {ROLE_ANALYST, ROLE_REVIEWER, ROLE_GATED}) {
      try {
        String pgRole = "MG_ROLE_" + role;
        database
            .getJooq()
            .execute(
                "DO $$ BEGIN "
                    + "EXECUTE 'REASSIGN OWNED BY \""
                    + pgRole
                    + "\" TO \"MG_USER_admin\"';"
                    + "EXECUTE 'DROP OWNED BY \""
                    + pgRole
                    + "\"';"
                    + "EXECUTE 'DROP ROLE IF EXISTS \""
                    + pgRole
                    + "\"';"
                    + "EXCEPTION WHEN OTHERS THEN NULL; END $$");
      } catch (Exception ignored) {
      }
    }
  }

  private void cleanupTestUser() {
    try {
      if (database.hasUser(TEST_USER)) {
        database.removeUser(TEST_USER);
      }
    } catch (Exception ignored) {
    }
  }

  @Test
  void sessionPermissions_currentUserSeesOwnPermissions() throws IOException {
    roleManager.createRole(ROLE_ANALYST, "analyst role");
    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            SCHEMA_NAME,
            TABLE_NAME,
            Select.ALL,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(ROLE_ANALYST, ps);
    roleManager.grantRoleToUser(ROLE_ANALYST, TEST_USER);

    database.setActiveUser(TEST_USER);
    executor = new GraphqlExecutor(database);

    JsonNode result =
        executeQuery(
            "{_session(schema:\"" + SCHEMA_NAME + "\"){permissions{schema table select}}}");
    JsonNode perms = result.at("/_session/permissions");
    assertFalse(perms.isMissingNode(), "permissions must be present");
    assertTrue(perms.isArray(), "permissions must be array");

    boolean found = false;
    for (JsonNode perm : perms) {
      if (SCHEMA_NAME.equals(perm.at("/schema").asText())
          && TABLE_NAME.equals(perm.at("/table").asText())) {
        assertEquals("ALL", perm.at("/select").asText());
        found = true;
      }
    }
    assertTrue(found, "Expected permission entry for " + SCHEMA_NAME + "." + TABLE_NAME);
  }

  @Test
  void adminRolesQuery_listsRolesAndPermissions() throws IOException {
    database.becomeAdmin();
    executor = new GraphqlExecutor(database);

    roleManager.createRole(ROLE_ANALYST, "analyst");
    roleManager.createRole(ROLE_REVIEWER, "reviewer");

    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            SCHEMA_NAME,
            TABLE_NAME,
            Select.ALL,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(ROLE_ANALYST, ps);

    JsonNode result = executeQuery("{_admin{roles{role permissions{schema table select}}}}");
    JsonNode roles = result.at("/_admin/roles");
    assertFalse(roles.isMissingNode());
    assertTrue(roles.isArray());

    boolean analystFound = false;
    boolean reviewerFound = false;
    for (JsonNode roleNode : roles) {
      String roleName = roleNode.at("/role").asText();
      if (ROLE_ANALYST.equals(roleName)) analystFound = true;
      if (ROLE_REVIEWER.equals(roleName)) reviewerFound = true;
    }
    assertTrue(analystFound, "analyst role must be listed");
    assertTrue(reviewerFound, "reviewer role must be listed");
  }

  @Test
  void adminRolesQuery_nonAdminNotAccessible() {
    database.setActiveUser(TEST_USER);
    executor = new GraphqlExecutor(database);

    assertThrows(
        Exception.class,
        () -> executor.executeWithoutSession("{_admin{roles{role}}}"),
        "Non-admin must not have access to _admin.roles");
  }

  @Test
  void changeRoleDefinitions_createsRole() throws IOException {
    database.becomeAdmin();
    executor = new GraphqlExecutor(database);

    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_ANALYST
            + "\",description:\"test\"}]){status message}}";
    JsonNode result = executeQuery(mutation);
    assertEquals("SUCCESS", result.at("/change/status").asText());

    boolean found =
        roleManager.listRoles().stream().anyMatch(r -> ROLE_ANALYST.equals(r.getRoleName()));
    assertTrue(found, "Role must exist after change(roles)");
  }

  @Test
  void changePermissions_replaceAll() throws IOException {
    database.becomeAdmin();
    executor = new GraphqlExecutor(database);

    roleManager.createRole(ROLE_ANALYST, "analyst");

    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_ANALYST
            + "\",permissions:[{schema:\""
            + SCHEMA_NAME
            + "\",table:\""
            + TABLE_NAME
            + "\",select:ALL}]}]){status message}}";
    JsonNode result = executeQuery(mutation);
    assertEquals("SUCCESS", result.at("/change/status").asText());

    PermissionSet perms = roleManager.getPermissions(ROLE_ANALYST);
    TablePermission found = perms.resolveFor(SCHEMA_NAME, TABLE_NAME);
    assertEquals(Select.ALL, found.select(), "select scope must be ALL");
  }

  @Test
  void changeMembers_grantsRole() throws IOException {
    database.becomeAdmin();
    executor = new GraphqlExecutor(database);

    roleManager.createRole(ROLE_ANALYST, "analyst");

    String mutation =
        "mutation{change(members:[{role:\""
            + ROLE_ANALYST
            + "\",user:\""
            + TEST_USER
            + "\"}]){status message}}";
    JsonNode result = executeQuery(mutation);
    assertEquals("SUCCESS", result.at("/change/status").asText());
  }

  @Test
  void nonAdminForbidden() throws IOException {
    database.setActiveUser(TEST_USER);
    executor = new GraphqlExecutor(database);

    String mutation = "mutation{change(roles:[{name:\"" + ROLE_ANALYST + "\"}]){status message}}";
    JsonNode result = executeQuery(mutation);
    assertEquals("FAILED", result.at("/change/status").asText());
    assertTrue(result.at("/change/message").asText().contains("admin"));
  }

  @Test
  void dropRoles_tombstonesRole() throws IOException {
    database.becomeAdmin();
    executor = new GraphqlExecutor(database);

    roleManager.createRole(ROLE_ANALYST, "analyst");

    String mutation = "mutation{drop(roles:[\"" + ROLE_ANALYST + "\"]){status message}}";
    JsonNode result = executeQuery(mutation);
    assertEquals("SUCCESS", result.at("/drop/status").asText());

    boolean stillActive =
        roleManager.listRoles().stream().anyMatch(r -> ROLE_ANALYST.equals(r.getRoleName()));
    assertFalse(stillActive, "Role must be tombstoned (not in active list) after drop");
  }

  @Test
  void dropMembers_revokesRole() throws IOException {
    database.becomeAdmin();
    executor = new GraphqlExecutor(database);

    roleManager.createRole(ROLE_ANALYST, "analyst");
    roleManager.grantRoleToUser(ROLE_ANALYST, TEST_USER);

    String mutation =
        "mutation{drop(members:[{role:\""
            + ROLE_ANALYST
            + "\",user:\""
            + TEST_USER
            + "\"}]){status message}}";
    JsonNode result = executeQuery(mutation);
    assertEquals("SUCCESS", result.at("/drop/status").asText());

    database.setActiveUser(TEST_USER);
    PermissionSet userPerms = database.getRoleManager().getPermissionsForActiveUser();
    database.becomeAdmin();
    assertEquals(0, userPerms.size(), "User must have no permissions after revoke");
  }

  @Test
  void changePermissions_acceptsAggregateSelect() throws IOException {
    database.becomeAdmin();
    executor = new GraphqlExecutor(database);

    roleManager.createRole(ROLE_ANALYST, "analyst");

    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_ANALYST
            + "\",permissions:[{schema:\""
            + SCHEMA_NAME
            + "\",table:\""
            + TABLE_NAME
            + "\",select:AGGREGATE}]}]){status message}}";
    JsonNode result = executeQuery(mutation);
    assertEquals("SUCCESS", result.at("/change/status").asText());

    PermissionSet perms = roleManager.getPermissions(ROLE_ANALYST);
    TablePermission found = perms.resolveFor(SCHEMA_NAME, TABLE_NAME);
    assertEquals(TablePermission.Select.AGGREGATE, found.select());
  }

  @Test
  void sessionPermissions_exposesUnifiedSelect() throws IOException {
    roleManager.createRole(ROLE_ANALYST, "analyst");
    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            SCHEMA_NAME,
            TABLE_NAME,
            TablePermission.Select.AGGREGATE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(ROLE_ANALYST, ps);
    roleManager.grantRoleToUser(ROLE_ANALYST, TEST_USER);

    database.setActiveUser(TEST_USER);
    executor = new GraphqlExecutor(database);

    JsonNode result =
        executeQuery(
            "{_session(schema:\"" + SCHEMA_NAME + "\"){permissions{schema table select}}}");
    JsonNode perms = result.at("/_session/permissions");
    assertFalse(perms.isMissingNode(), "permissions must be present");

    boolean found = false;
    for (JsonNode perm : perms) {
      if (SCHEMA_NAME.equals(perm.at("/schema").asText())
          && TABLE_NAME.equals(perm.at("/table").asText())) {
        assertEquals(
            "AGGREGATE",
            perm.at("/select").asText(),
            "select field must expose unified AGGREGATE value (no separate selectScope field)");
        found = true;
      }
    }
    assertTrue(found, "Expected permission entry with unified select=AGGREGATE");
  }

  @Test
  void setPermissionsRejectsNonManagerNonOwner() throws IOException {
    database.becomeAdmin();
    roleManager.createRole(ROLE_GATED, "gated role");
    database.getSchema(SCHEMA_NAME).addMember(TEST_USER, Privileges.EDITOR.toString());

    database.setActiveUser(TEST_USER);
    executor = new GraphqlExecutor(database);

    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_GATED
            + "\",permissions:[{schema:\""
            + SCHEMA_NAME
            + "\",table:\""
            + TABLE_NAME
            + "\",select:ALL}]}]){status message}}";
    JsonNode result = executeQuery(mutation);
    assertEquals("FAILED", result.at("/change/status").asText());
    String message = result.at("/change/message").asText().toLowerCase();
    assertTrue(
        message.contains("manager") || message.contains("owner") || message.contains("permission"),
        "Error must mention MANAGER/OWNER or permission denied, got: " + message);
  }

  @Test
  void setPermissionsAcceptsManager() throws IOException {
    database.becomeAdmin();
    roleManager.createRole(ROLE_GATED, "gated role");
    database.getSchema(SCHEMA_NAME).addMember(TEST_USER, Privileges.MANAGER.toString());

    database.setActiveUser(TEST_USER);
    executor = new GraphqlExecutor(database);

    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_GATED
            + "\",permissions:[{schema:\""
            + SCHEMA_NAME
            + "\",table:\""
            + TABLE_NAME
            + "\",select:ALL}]}]){status message}}";
    JsonNode result = executeQuery(mutation);
    assertEquals("SUCCESS", result.at("/change/status").asText());
  }

  @Test
  void setPermissionsAcceptsOwner() throws IOException {
    database.becomeAdmin();
    roleManager.createRole(ROLE_GATED, "gated role");
    database.getSchema(SCHEMA_NAME).addMember(TEST_USER, Privileges.OWNER.toString());

    database.setActiveUser(TEST_USER);
    executor = new GraphqlExecutor(database);

    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_GATED
            + "\",permissions:[{schema:\""
            + SCHEMA_NAME
            + "\",table:\""
            + TABLE_NAME
            + "\",select:ALL}]}]){status message}}";
    JsonNode result = executeQuery(mutation);
    assertEquals("SUCCESS", result.at("/change/status").asText());
  }

  private JsonNode executeQuery(String query) throws IOException {
    String json = convertExecutionResultToJson(executor.executeWithoutSession(query));
    JsonNode root = new ObjectMapper().readTree(json);
    assertNull(root.get("errors"), "GraphQL errors: " + root.get("errors"));
    return root.get("data");
  }
}
