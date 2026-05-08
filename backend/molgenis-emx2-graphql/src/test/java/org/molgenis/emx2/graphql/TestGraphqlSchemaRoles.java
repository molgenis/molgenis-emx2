package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.SelectScope;
import org.molgenis.emx2.TablePermission;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlRoleManager;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestGraphqlSchemaRoles {

  private static final String SCHEMA_NAME = "TGraphqlPermSchema";
  private static final String TABLE_PET = "Pet";
  private static final String CUSTOM_ROLE = "tester";

  private static final String USER_MANAGER = "perm_manager";
  private static final String USER_OWNER = "perm_owner";
  private static final String USER_EDITOR = "perm_editor";
  private static final String USER_VIEWER = "perm_viewer";
  private static final String USER_NOROLE = "perm_norole";
  private static final String USER_TEST = "perm_test_user";

  private static final String ROLE_ANALYST = "perm_analyst";
  private static final String ROLE_REVIEWER = "perm_reviewer";
  private static final String ROLE_GATED = "perm_gated";
  private static final String TABLE_NAME = "Pet";

  private static final String ROLES_QUERY =
      "{_schema { roles { name permissions { table select insert update delete } changeOwner changeGroup } } }";

  private static Database database;
  private static SqlRoleManager roleManager;
  private static Schema schema;
  private static GraphqlExecutor executor;

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    schema = database.createSchema(SCHEMA_NAME);
    schema
        .getMetadata()
        .create(
            TableMetadata.table(TABLE_PET)
                .add(Column.column("name").setType(ColumnType.STRING).setKey(1)));
    executor = new GraphqlExecutor(schema, new TaskServiceInMemory());

    database.setUserPassword(USER_MANAGER, USER_MANAGER);
    database.setUserPassword(USER_OWNER, USER_OWNER);
    database.setUserPassword(USER_EDITOR, USER_EDITOR);
    database.setUserPassword(USER_VIEWER, USER_VIEWER);
    database.setUserPassword(USER_NOROLE, USER_NOROLE);

    if (database.hasUser(USER_TEST)) {
      database.removeUser(USER_TEST);
    }
    database.addUser(USER_TEST);

    schema.addMember(USER_MANAGER, "Manager");
    schema.addMember(USER_OWNER, "Owner");
    schema.addMember(USER_EDITOR, "Editor");
    schema.addMember(USER_VIEWER, "Viewer");

    roleManager = ((SqlDatabase) database).getRoleManager();
    schema.getTable(TABLE_PET).getMetadata().setRlsEnabled(true);
  }

  private static final String SCHEMA_ROLE_A = "schemaRoleA";

  @AfterEach
  void tearDownCustomRoles() {
    database.becomeAdmin();
    for (String roleName : roleManager.listRoles(SCHEMA_NAME)) {
      try {
        roleManager.deleteRole(SCHEMA_NAME, roleName);
      } catch (Exception ignored) {
      }
    }
  }

  @Test
  void changeRoles_customRole_setsPermissionsViaPermissionSet() throws IOException {
    JsonNode rolesBefore = execute(ROLES_QUERY).at("/_schema/roles");
    assertNull(findRoleByName(rolesBefore, CUSTOM_ROLE), "Role should not exist before mutation");

    String mutation =
        "mutation { change(roles: [{"
            + "name: \""
            + CUSTOM_ROLE
            + "\", "
            + "permissions: [{table: \""
            + TABLE_PET
            + "\", select: OWN, insert: OWN, update: OWN, delete: NONE}], "
            + "changeOwner: false, changeGroup: false"
            + "}]) { message } }";

    JsonNode result = execute(mutation);
    assertNotNull(result.at("/change/message").textValue());

    JsonNode rolesAfter = execute(ROLES_QUERY).at("/_schema/roles");
    JsonNode found = findRoleByName(rolesAfter, CUSTOM_ROLE);
    assertNotNull(found, "Role " + CUSTOM_ROLE + " should appear after mutation");
    assertEquals("OWN", found.at("/permissions/0/select").asText());
    assertEquals("OWN", found.at("/permissions/0/insert").asText());
    assertEquals("OWN", found.at("/permissions/0/update").asText());
    assertEquals("NONE", found.at("/permissions/0/delete").asText());
    assertFalse(found.at("/changeOwner").asBoolean());
    assertFalse(found.at("/changeGroup").asBoolean());
  }

  @Test
  void changeRoles_updateExistingRole_overwritesPriorScopes() throws IOException {
    String roleToUpdate = "updatable";

    JsonNode rolesBefore = execute(ROLES_QUERY).at("/_schema/roles");
    assertNull(findRoleByName(rolesBefore, roleToUpdate), "Role should not exist before mutations");

    String firstMutation =
        "mutation { change(roles: [{"
            + "name: \""
            + roleToUpdate
            + "\", "
            + "permissions: [{table: \""
            + TABLE_PET
            + "\", select: ALL, insert: ALL, update: ALL, delete: ALL}]"
            + "}]) { message } }";
    execute(firstMutation);

    JsonNode rolesAfterFirst = execute(ROLES_QUERY).at("/_schema/roles");
    JsonNode foundAfterFirst = findRoleByName(rolesAfterFirst, roleToUpdate);
    assertNotNull(foundAfterFirst, "Role should exist after first mutation");
    assertEquals("ALL", foundAfterFirst.at("/permissions/0/select").asText());

    String secondMutation =
        "mutation { change(roles: [{"
            + "name: \""
            + roleToUpdate
            + "\", "
            + "permissions: [{table: \""
            + TABLE_PET
            + "\", select: NONE, insert: NONE, update: NONE, delete: NONE}]"
            + "}]) { message } }";
    execute(secondMutation);

    JsonNode rolesAfterSecond = execute(ROLES_QUERY).at("/_schema/roles");
    JsonNode foundAfterSecond = findRoleByName(rolesAfterSecond, roleToUpdate);
    assertNotNull(foundAfterSecond, "Role should still exist after second mutation");
    assertEquals("NONE", foundAfterSecond.at("/permissions/0/select").asText());
    assertEquals("NONE", foundAfterSecond.at("/permissions/0/insert").asText());
  }

  @Test
  void changeRoles_invalidViewModeOnInsert_throwsMolgenisException() throws IOException {
    JsonNode rolesBefore = execute(ROLES_QUERY).at("/_schema/roles");
    int countBefore = rolesBefore.size();

    String mutation =
        "mutation { change(roles: [{"
            + "name: \"badscope\", "
            + "permissions: [{table: \""
            + TABLE_PET
            + "\", insert: EXISTS}]"
            + "}]) { message } }";

    MolgenisException thrown =
        assertThrows(MolgenisException.class, () -> executor.executeWithoutSession(mutation));
    assertTrue(
        thrown.getMessage().contains("EXISTS"), "Expected error message to mention EXISTS scope");

    JsonNode rolesAfter = execute(ROLES_QUERY).at("/_schema/roles");
    assertEquals(countBefore, rolesAfter.size(), "Role count should be unchanged after rejection");
    assertNull(
        findRoleByName(rolesAfter, "badscope"), "badscope role should not have been created");
  }

  @Test
  void changeRoles_systemRoleInInput_throwsImmutableError() throws IOException {
    JsonNode rolesBefore = execute(ROLES_QUERY).at("/_schema/roles");
    JsonNode viewerBefore = findRoleByName(rolesBefore, "Viewer");
    assertNotNull(viewerBefore, "Viewer system role must be present before mutation attempt");

    String mutation =
        "mutation { change(roles: ["
            + "{name: \"Viewer\", permissions: [{table: \""
            + TABLE_PET
            + "\", select: ALL}]}"
            + "]) { message } }";

    MolgenisException thrown =
        assertThrows(MolgenisException.class, () -> executor.executeWithoutSession(mutation));
    assertTrue(
        thrown.getMessage().contains("immutable"),
        "Error must mention immutable; got: " + thrown.getMessage());

    JsonNode rolesAfter = execute(ROLES_QUERY).at("/_schema/roles");
    JsonNode viewerAfter = findRoleByName(rolesAfter, "Viewer");
    assertNotNull(viewerAfter, "Viewer role must still be present after rejected mutation");
  }

  @Test
  void changeRoles_managerUser_canGrantCustomRole() throws IOException {
    String roleName = "managertarget";
    JsonNode rolesBefore = execute(ROLES_QUERY).at("/_schema/roles");
    assertNull(findRoleByName(rolesBefore, roleName), "Role should not exist before mutation");

    String mutation = buildGrantMutation(roleName);
    try {
      database.setActiveUser(USER_MANAGER);
      GraphqlExecutor userExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      JsonNode result = executeAs(userExecutor, mutation);
      assertNotNull(result.at("/change/message").textValue());
    } finally {
      database.becomeAdmin();
    }

    JsonNode rolesAfter = execute(ROLES_QUERY).at("/_schema/roles");
    assertNotNull(
        findRoleByName(rolesAfter, roleName), "Role " + roleName + " should appear after grant");
  }

  @Test
  void changeRoles_ownerUser_canGrantCustomRole() throws IOException {
    String roleName = "ownertarget";
    JsonNode rolesBefore = execute(ROLES_QUERY).at("/_schema/roles");
    assertNull(findRoleByName(rolesBefore, roleName), "Role should not exist before mutation");

    String mutation = buildGrantMutation(roleName);
    try {
      database.setActiveUser(USER_OWNER);
      GraphqlExecutor userExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      JsonNode result = executeAs(userExecutor, mutation);
      assertNotNull(result.at("/change/message").textValue());
    } finally {
      database.becomeAdmin();
    }

    JsonNode rolesAfter = execute(ROLES_QUERY).at("/_schema/roles");
    assertNotNull(
        findRoleByName(rolesAfter, roleName), "Role " + roleName + " should appear after grant");
  }

  @Test
  void changeRoles_adminUser_canGrantCustomRole() throws IOException {
    String roleName = "admintarget";
    JsonNode rolesBefore = execute(ROLES_QUERY).at("/_schema/roles");
    assertNull(findRoleByName(rolesBefore, roleName), "Role should not exist before mutation");

    JsonNode result = execute(buildGrantMutation(roleName));
    assertNotNull(result.at("/change/message").textValue());

    JsonNode rolesAfter = execute(ROLES_QUERY).at("/_schema/roles");
    assertNotNull(
        findRoleByName(rolesAfter, roleName), "Role " + roleName + " should appear after grant");
  }

  @Test
  void changeRoles_editorUser_deniedGrantingCustomRole() throws IOException {
    String roleName = "editortarget";
    JsonNode rolesBefore = execute(ROLES_QUERY).at("/_schema/roles");
    assertNull(
        findRoleByName(rolesBefore, roleName), "Role should not exist before mutation attempt");

    String mutation = buildGrantMutation(roleName);
    try {
      database.setActiveUser(USER_EDITOR);
      GraphqlExecutor userExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      MolgenisException thrown =
          assertThrows(MolgenisException.class, () -> executeAs(userExecutor, mutation));
      assertTrue(
          thrown.getMessage().contains("Manager") || thrown.getMessage().contains("Owner"),
          "Error should mention Manager or Owner requirement");
    } finally {
      database.becomeAdmin();
    }

    JsonNode rolesAfter = execute(ROLES_QUERY).at("/_schema/roles");
    assertNull(
        findRoleByName(rolesAfter, roleName), "Role should not have been created after rejection");
  }

  @Test
  void changeRoles_viewerUser_deniedGrantingCustomRole() throws IOException {
    String roleName = "viewertarget";
    JsonNode rolesBefore = execute(ROLES_QUERY).at("/_schema/roles");
    assertNull(
        findRoleByName(rolesBefore, roleName), "Role should not exist before mutation attempt");

    String mutation = buildGrantMutation(roleName);
    try {
      database.setActiveUser(USER_VIEWER);
      GraphqlExecutor userExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      MolgenisException thrown =
          assertThrows(MolgenisException.class, () -> executeAs(userExecutor, mutation));
      assertTrue(
          thrown.getMessage().contains("Manager") || thrown.getMessage().contains("Owner"),
          "Error should mention Manager or Owner requirement");
    } finally {
      database.becomeAdmin();
    }

    JsonNode rolesAfter = execute(ROLES_QUERY).at("/_schema/roles");
    assertNull(
        findRoleByName(rolesAfter, roleName), "Role should not have been created after rejection");
  }

  @Test
  void changeRoles_noRoleUser_deniedGrantingCustomRole() throws IOException {
    String roleName = "noroletarget";
    JsonNode rolesBefore = execute(ROLES_QUERY).at("/_schema/roles");
    assertNull(
        findRoleByName(rolesBefore, roleName), "Role should not exist before mutation attempt");

    String mutation = buildGrantMutation(roleName);
    try {
      database.setActiveUser(USER_NOROLE);
      GraphqlExecutor userExecutor = new GraphqlExecutor(schema, new TaskServiceInMemory());
      MolgenisException thrown =
          assertThrows(MolgenisException.class, () -> executeAs(userExecutor, mutation));
      assertFalse(thrown.getMessage().isEmpty(), "Denial must produce a non-empty error message");
    } finally {
      database.becomeAdmin();
    }

    JsonNode rolesAfter = execute(ROLES_QUERY).at("/_schema/roles");
    assertNull(
        findRoleByName(rolesAfter, roleName), "Role should not have been created after rejection");
  }

  @Test
  void rolesQuery_roundTrip_returnsSetPermissions() throws IOException {
    String roleA = "queryRoleA";

    JsonNode rolesBefore = execute(ROLES_QUERY).at("/_schema/roles");
    assertNull(findRoleByName(rolesBefore, roleA), "Role should not exist before mutation");

    execute(
        "mutation { change(roles: [{"
            + "name: \""
            + roleA
            + "\", "
            + "permissions: [{table: \""
            + TABLE_PET
            + "\", select: GROUP, insert: OWN, update: NONE, delete: NONE}], "
            + "changeOwner: true, changeGroup: false"
            + "}]) { message } }");

    JsonNode roles =
        execute(
                "{_schema { roles { name permissions { table select insert update delete }"
                    + " changeOwner changeGroup } } }")
            .at("/_schema/roles");
    assertFalse(roles.isEmpty(), "roles should not be empty");

    JsonNode found = findRoleByName(roles, roleA);
    assertNotNull(found, "Role " + roleA + " should appear in roles");
    assertEquals("GROUP", found.at("/permissions/0/select").asText());
    assertEquals("OWN", found.at("/permissions/0/insert").asText());
    assertEquals("NONE", found.at("/permissions/0/update").asText());
    assertEquals("NONE", found.at("/permissions/0/delete").asText());
    assertTrue(found.at("/changeOwner").asBoolean());
    assertFalse(found.at("/changeGroup").asBoolean());
  }

  @Test
  void rolesQuery_multipleCustomRoles_allListed() throws IOException {
    String roleB = "multiRoleB";
    String roleC = "multiRoleC";

    JsonNode rolesBefore = execute("{_schema { roles { name } } }").at("/_schema/roles");
    assertNull(
        findRoleByName(rolesBefore, roleB), "Role " + roleB + " should not exist before mutation");
    assertNull(
        findRoleByName(rolesBefore, roleC), "Role " + roleC + " should not exist before mutation");

    execute(
        "mutation { change(roles: ["
            + "{name: \""
            + roleB
            + "\", permissions: [{table: \""
            + TABLE_PET
            + "\", select: ALL, insert: ALL, update: ALL, delete: ALL}]}, "
            + "{name: \""
            + roleC
            + "\", permissions: [{table: \""
            + TABLE_PET
            + "\", select: OWN, insert: NONE, update: NONE, delete: NONE}]}"
            + "]) { message } }");

    JsonNode roles = execute("{_schema { roles { name } } }").at("/_schema/roles");
    assertNotNull(findRoleByName(roles, roleB), "Role " + roleB + " should appear");
    assertNotNull(findRoleByName(roles, roleC), "Role " + roleC + " should appear");
  }

  @Test
  void rolesQuery_noCustomRoles_onlySystemRoles() throws IOException {
    Database emptyDb = TestDatabaseFactory.getTestDatabase();
    String emptySchemaName = "TGraphqlPermEmpty";
    emptyDb.dropSchemaIfExists(emptySchemaName);
    Schema emptySchema = emptyDb.createSchema(emptySchemaName);
    GraphqlExecutor emptyExecutor = new GraphqlExecutor(emptySchema, new TaskServiceInMemory());

    String json =
        convertExecutionResultToJson(
            emptyExecutor.executeWithoutSession("{_schema { roles { name } } }"));
    JsonNode roles = new ObjectMapper().readTree(json).at("/data/_schema/roles");
    assertTrue(roles.isArray() && !roles.isEmpty(), "roles should contain system roles");
    for (JsonNode roleNode : roles) {
      String name = roleNode.at("/name").asText();
      assertTrue(isSystemRoleName(name), "Only system roles expected; found: " + name);
    }
    emptyDb.dropSchemaIfExists(emptySchemaName);
  }

  @Test
  void rolesQuery_description_roundTrip() throws IOException {
    String roleWithDesc = "descRole";

    JsonNode rolesBefore = execute("{_schema { roles { name } } }").at("/_schema/roles");
    assertNull(findRoleByName(rolesBefore, roleWithDesc), "Role should not exist before mutation");

    execute(
        "mutation { change(roles: [{"
            + "name: \""
            + roleWithDesc
            + "\", description: \"team alpha role\", "
            + "permissions: [{table: \""
            + TABLE_PET
            + "\", select: ALL}]"
            + "}]) { message } }");

    JsonNode roles =
        execute("{_schema { roles { name description permissions { table select } } } }")
            .at("/_schema/roles");
    JsonNode found = findRoleByName(roles, roleWithDesc);
    assertNotNull(found, "Role " + roleWithDesc + " should appear in roles");
    assertEquals("team alpha role", found.at("/description").asText());

    execute(
        "mutation { change(roles: [{"
            + "name: \""
            + roleWithDesc
            + "\", "
            + "permissions: [{table: \""
            + TABLE_PET
            + "\", select: ALL}]"
            + "}]) { message } }");

    JsonNode rolesAfter = execute("{_schema { roles { name description } } }").at("/_schema/roles");
    JsonNode foundAfter = findRoleByName(rolesAfter, roleWithDesc);
    assertNotNull(foundAfter, "Role " + roleWithDesc + " should still appear after update");
    assertEquals(
        "", foundAfter.at("/description").asText(), "description_null_input_overwrites_to_empty");
  }

  @Test
  void rolesQuery_returnsMerged() throws IOException {
    String customRole = "mergedQueryRole";

    JsonNode rolesBefore = execute("{_schema { roles { name system } } }").at("/_schema/roles");
    assertNull(
        findRoleByName(rolesBefore, customRole), "Custom role should not exist before mutation");

    execute(
        "mutation { change(roles: [{"
            + "name: \""
            + customRole
            + "\", "
            + "permissions: [{table: \""
            + TABLE_PET
            + "\", select: ALL}]"
            + "}]) { message } }");

    JsonNode roles = execute("{_schema { roles { name system } } }").at("/_schema/roles");

    boolean hasSystemRole = false;
    boolean hasCustomRole = false;
    for (JsonNode roleNode : roles) {
      String name = roleNode.at("/name").asText();
      if (isSystemRoleName(name)) {
        hasSystemRole = true;
        assertTrue(
            roleNode.at("/system").asBoolean(), "System role " + name + " must have system:true");
      }
      if (customRole.equals(name)) {
        hasCustomRole = true;
        assertFalse(roleNode.at("/system").asBoolean(), "Custom role must have system:false");
      }
    }
    assertTrue(hasSystemRole, "_schema.roles must include system roles");
    assertTrue(hasCustomRole, "_schema.roles must include custom role " + customRole);
  }

  @Test
  void rolesQuery_systemRolesPresent_inMergedRoles() throws IOException {
    JsonNode roles =
        execute(
                "{_schema { roles { name description permissions { table select insert update delete } } } }")
            .at("/_schema/roles");
    boolean hasViewer = false;
    for (JsonNode roleNode : roles) {
      if ("Viewer".equals(roleNode.at("/name").asText())) {
        hasViewer = true;
        break;
      }
    }
    assertTrue(hasViewer, "Merged roles field must contain system role Viewer");
  }

  @Test
  void rolesQuery_schemaField_roundTrips() throws IOException {
    JsonNode rolesBefore = execute("{_schema { roles { name } } }").at("/_schema/roles");
    assertNull(findRoleByName(rolesBefore, SCHEMA_ROLE_A), "Role should not exist before mutation");

    execute(
        "mutation { change(roles: [{"
            + "name: \""
            + SCHEMA_ROLE_A
            + "\", "
            + "permissions: [{table: \""
            + TABLE_PET
            + "\", select: ALL}]"
            + "}]) { message } }");

    JsonNode roles = execute("{_schema { roles { name schemaName } } }").at("/_schema/roles");
    JsonNode found = findRoleByName(roles, SCHEMA_ROLE_A);
    assertNotNull(found, "Role should appear in roles");
    assertEquals(
        SCHEMA_NAME, found.at("/schemaName").asText(), "schemaName field must match schema name");
  }

  @Test
  void rolesQuery_listsRolesAndPermissions() throws IOException {
    database.becomeAdmin();

    roleManager.createRole(SCHEMA_NAME, ROLE_ANALYST);
    roleManager.createRole(SCHEMA_NAME, ROLE_REVIEWER);

    try {
      PermissionSet ps = new PermissionSet();
      TablePermission tp = new TablePermission(TABLE_NAME);
      tp.setSelect(SelectScope.ALL);
      ps.putTable(TABLE_NAME, tp);
      roleManager.setPermissions(schema, ROLE_ANALYST, ps);

      assertTrue(roleManager.roleExists(SCHEMA_NAME, ROLE_ANALYST), "analyst role must exist");
      assertTrue(roleManager.roleExists(SCHEMA_NAME, ROLE_REVIEWER), "reviewer role must exist");

      java.util.List<String> roleNames = roleManager.listRoles(SCHEMA_NAME);
      assertTrue(
          roleNames.contains(ROLE_ANALYST), "analyst with permissions must appear in listRoles");
    } finally {
      deleteRoleIfExists(ROLE_ANALYST);
      deleteRoleIfExists(ROLE_REVIEWER);
    }
  }

  @Test
  void changeRoleDefinitions_createsRole() throws IOException {
    database.becomeAdmin();

    String mutation =
        "mutation{change(roles:[{name:\""
            + ROLE_ANALYST
            + "\",description:\"test\"}]){status message}}";
    JsonNode result = execute(mutation);
    assertEquals("SUCCESS", result.at("/change/status").asText());

    try {
      assertTrue(
          roleManager.roleExists(SCHEMA_NAME, ROLE_ANALYST), "Role must exist after change(roles)");
    } finally {
      deleteRoleIfExists(ROLE_ANALYST);
    }
  }

  @Test
  void changePermissions_replaceAll() throws IOException {
    database.becomeAdmin();

    roleManager.createRole(SCHEMA_NAME, ROLE_ANALYST);

    try {
      String mutation =
          "mutation{change(roles:[{name:\""
              + ROLE_ANALYST
              + "\",permissions:[{table:\""
              + TABLE_NAME
              + "\",select:ALL}]}]){status message}}";
      JsonNode result = execute(mutation);
      assertEquals("SUCCESS", result.at("/change/status").asText());

      PermissionSet perms = roleManager.getPermissions(schema, ROLE_ANALYST);
      TablePermission tablePerms = perms.getTables().get(TABLE_NAME);
      assertNotNull(tablePerms, "Table permissions must be present");
      assertEquals(SelectScope.ALL, tablePerms.getSelect(), "select scope must be ALL");
    } finally {
      deleteRoleIfExists(ROLE_ANALYST);
    }
  }

  @Test
  void dropRoles_tombstonesRole() throws IOException {
    database.becomeAdmin();

    roleManager.createRole(SCHEMA_NAME, ROLE_ANALYST);

    String mutation = "mutation{drop(roles:[\"" + ROLE_ANALYST + "\"]){message}}";
    execute(mutation);

    assertFalse(
        roleManager.roleExists(SCHEMA_NAME, ROLE_ANALYST), "Role must not exist after drop");
  }

  @Test
  void changePermissions_acceptsAggregateSelect() throws IOException {
    database.becomeAdmin();

    roleManager.createRole(SCHEMA_NAME, ROLE_ANALYST);

    try {
      String mutation =
          "mutation{change(roles:[{name:\""
              + ROLE_ANALYST
              + "\",permissions:[{table:\""
              + TABLE_NAME
              + "\",select:AGGREGATE}]}]){status message}}";
      JsonNode result = execute(mutation);
      assertEquals("SUCCESS", result.at("/change/status").asText());

      PermissionSet perms = roleManager.getPermissions(schema, ROLE_ANALYST);
      TablePermission tablePerms = perms.getTables().get(TABLE_NAME);
      assertNotNull(tablePerms, "Table permissions must be present");
      assertEquals(SelectScope.AGGREGATE, tablePerms.getSelect(), "select scope must be AGGREGATE");
    } finally {
      deleteRoleIfExists(ROLE_ANALYST);
    }
  }

  @Test
  void nonAdminForbidden() {
    database.setActiveUser(USER_TEST);

    String mutation = "mutation{change(roles:[{name:\"" + ROLE_ANALYST + "\"}]){status message}}";
    assertThrows(
        Exception.class,
        () -> execute(mutation),
        "Non-member user must not be able to change roles");
    database.becomeAdmin();
  }

  @Test
  void setPermissionsRejectsNonManagerNonOwner() {
    database.becomeAdmin();
    roleManager.createRole(SCHEMA_NAME, ROLE_GATED);
    schema.addMember(USER_TEST, Privileges.EDITOR.toString());

    try {
      database.setActiveUser(USER_TEST);
      String mutation =
          "mutation{change(roles:[{name:\""
              + ROLE_GATED
              + "\",permissions:[{table:\""
              + TABLE_NAME
              + "\",select:ALL}]}]){status message}}";
      Exception thrown =
          assertThrows(
              Exception.class,
              () -> execute(mutation),
              "Editor must not be able to change role permissions");
      String message = thrown.getMessage().toLowerCase();
      assertTrue(
          message.contains("manager")
              || message.contains("owner")
              || message.contains("permission"),
          "Error must mention MANAGER/OWNER or permission denied, got: " + message);
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_TEST);
      deleteRoleIfExists(ROLE_GATED);
    }
  }

  @Test
  void setPermissionsAcceptsManager() throws IOException {
    database.becomeAdmin();
    roleManager.createRole(SCHEMA_NAME, ROLE_GATED);
    schema.addMember(USER_TEST, Privileges.MANAGER.toString());

    try {
      database.setActiveUser(USER_TEST);
      String mutation =
          "mutation{change(roles:[{name:\""
              + ROLE_GATED
              + "\",permissions:[{table:\""
              + TABLE_NAME
              + "\",select:ALL}]}]){status message}}";
      JsonNode result = execute(mutation);
      assertEquals("SUCCESS", result.at("/change/status").asText());
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_TEST);
      deleteRoleIfExists(ROLE_GATED);
    }
  }

  @Test
  void setPermissionsAcceptsOwner() throws IOException {
    database.becomeAdmin();
    roleManager.createRole(SCHEMA_NAME, ROLE_GATED);
    schema.addMember(USER_TEST, Privileges.OWNER.toString());

    try {
      database.setActiveUser(USER_TEST);
      String mutation =
          "mutation{change(roles:[{name:\""
              + ROLE_GATED
              + "\",permissions:[{table:\""
              + TABLE_NAME
              + "\",select:ALL}]}]){status message}}";
      JsonNode result = execute(mutation);
      assertEquals("SUCCESS", result.at("/change/status").asText());
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_TEST);
      deleteRoleIfExists(ROLE_GATED);
    }
  }

  @Test
  void setPermissionsSchemaIsolation_managerCannotCrossSchemas() throws IOException {
    database.becomeAdmin();
    roleManager.createRole(SCHEMA_NAME, ROLE_GATED);
    schema.addMember(USER_TEST, Privileges.MANAGER.toString());

    try {
      database.setActiveUser(USER_TEST);
      String mutation =
          "mutation{change(roles:[{name:\""
              + ROLE_GATED
              + "\",schemaName:\"*\",permissions:[{table:\""
              + TABLE_NAME
              + "\",select:ALL}]}]){status message}}";
      JsonNode result = execute(mutation);
      assertEquals(
          "SUCCESS",
          result.at("/change/status").asText(),
          "schema isolation: schemaName override is silently normalized to current schema");

      PermissionSet perms = roleManager.getPermissions(schema, ROLE_GATED);
      assertNotNull(perms.getTables().get(TABLE_NAME), "Permission must be on current schema only");
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_TEST);
      deleteRoleIfExists(ROLE_GATED);
    }
  }

  @Test
  void setPermissionsAcceptsWildcardTableForManager() throws IOException {
    database.becomeAdmin();
    roleManager.createRole(SCHEMA_NAME, ROLE_GATED);
    schema.addMember(USER_TEST, Privileges.MANAGER.toString());

    try {
      database.setActiveUser(USER_TEST);
      String mutation =
          "mutation{change(roles:[{name:\""
              + ROLE_GATED
              + "\",permissions:[{table:\"*\",select:ALL}]}]){status message}}";
      JsonNode result = execute(mutation);
      assertEquals("SUCCESS", result.at("/change/status").asText());
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_TEST);
      deleteRoleIfExists(ROLE_GATED);
    }
  }

  @Test
  void setPermissionsAcceptsWildcardTableForOwner() throws IOException {
    database.becomeAdmin();
    roleManager.createRole(SCHEMA_NAME, ROLE_GATED);
    schema.addMember(USER_TEST, Privileges.OWNER.toString());

    try {
      database.setActiveUser(USER_TEST);
      String mutation =
          "mutation{change(roles:[{name:\""
              + ROLE_GATED
              + "\",permissions:[{table:\"*\",select:ALL}]}]){status message}}";
      JsonNode result = execute(mutation);
      assertEquals("SUCCESS", result.at("/change/status").asText());
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_TEST);
      deleteRoleIfExists(ROLE_GATED);
    }
  }

  @Test
  void adminRolesQuery_nonAdminNotAccessible() {
    database.setActiveUser(USER_TEST);
    GraphqlExecutor userExecutor = new GraphqlExecutor(database, new TaskServiceInMemory());

    assertThrows(
        Exception.class,
        () -> userExecutor.executeWithoutSession("{_admin{users{email}}}"),
        "Non-admin must not have access to _admin");
    database.becomeAdmin();
  }

  private static JsonNode findRoleByName(JsonNode rolesArray, String roleName) {
    for (JsonNode node : rolesArray) {
      if (roleName.equals(node.at("/name").asText())) {
        return node;
      }
    }
    return null;
  }

  private static boolean isSystemRoleName(String name) {
    return name.equals("Viewer")
        || name.equals("Editor")
        || name.equals("Manager")
        || name.equals("Owner")
        || name.equals("Count")
        || name.equals("Range")
        || name.equals("Exists")
        || name.equals("Aggregator");
  }

  private static String buildGrantMutation(String roleName) {
    return "mutation { change(roles: [{"
        + "name: \""
        + roleName
        + "\", "
        + "permissions: [{table: \""
        + TABLE_PET
        + "\", select: OWN}]"
        + "}]) { message } }";
  }

  private void deleteRoleIfExists(String roleName) {
    try {
      roleManager.deleteRole(SCHEMA_NAME, roleName);
    } catch (Exception ignored) {
    }
  }

  private JsonNode execute(String query) throws IOException {
    String json = convertExecutionResultToJson(executor.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(json);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return node.get("data");
  }

  private JsonNode executeAs(GraphqlExecutor userExecutor, String query) throws IOException {
    String json = convertExecutionResultToJson(userExecutor.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(json);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return node.get("data");
  }
}
