package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlRoleManager;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestGraphqlPermissions {

  private static final String SCHEMA_NAME = "TGraphqlPermSchema";
  private static final String TABLE_PET = "Pet";
  private static final String CUSTOM_ROLE = "tester";

  private static final String USER_MANAGER = "perm_manager";
  private static final String USER_OWNER = "perm_owner";
  private static final String USER_EDITOR = "perm_editor";
  private static final String USER_VIEWER = "perm_viewer";
  private static final String USER_NOROLE = "perm_norole";

  private static Database database;
  private static Schema schema;
  private static GraphqlExecutor executor;
  private static SqlRoleManager roleManager;

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
    roleManager = ((SqlDatabase) database).getRoleManager();

    database.setUserPassword(USER_MANAGER, USER_MANAGER);
    database.setUserPassword(USER_OWNER, USER_OWNER);
    database.setUserPassword(USER_EDITOR, USER_EDITOR);
    database.setUserPassword(USER_VIEWER, USER_VIEWER);
    database.setUserPassword(USER_NOROLE, USER_NOROLE);

    schema.addMember(USER_MANAGER, "Manager");
    schema.addMember(USER_OWNER, "Owner");
    schema.addMember(USER_EDITOR, "Editor");
    schema.addMember(USER_VIEWER, "Viewer");
  }

  @Test
  void changeRoles_customRole_setsPermissionsViaPermissionSet() throws IOException {
    String mutation =
        "mutation { change(roles: [{"
            + "name: \""
            + CUSTOM_ROLE
            + "\", "
            + "tables: [{table: \""
            + TABLE_PET
            + "\", select: OWN, insert: OWN, update: OWN, delete: NONE}], "
            + "changeOwner: false, changeGroup: false"
            + "}]) { message } }";

    JsonNode result = execute(mutation);
    assertNotNull(result.at("/change/message").textValue());

    PermissionSet ps = roleManager.getPermissions(schema, CUSTOM_ROLE);
    PermissionSet.TablePermissions petPerms = ps.getTables().get(TABLE_PET);
    assertNotNull(petPerms, "Pet table permissions should be present");
    assertEquals(SelectScope.OWN, petPerms.getSelect());
    assertEquals(SelectScope.OWN, petPerms.getInsert());
    assertEquals(SelectScope.OWN, petPerms.getUpdate());
    assertEquals(SelectScope.NONE, petPerms.getDelete());
    assertFalse(ps.isChangeOwner());
    assertFalse(ps.isChangeGroup());
  }

  @Test
  void changeRoles_updateExistingRole_overwritesPriorScopes() throws IOException {
    String roleToUpdate = "updatable";

    String firstMutation =
        "mutation { change(roles: [{"
            + "name: \""
            + roleToUpdate
            + "\", "
            + "tables: [{table: \""
            + TABLE_PET
            + "\", select: ALL, insert: ALL, update: ALL, delete: ALL}]"
            + "}]) { message } }";
    execute(firstMutation);

    PermissionSet psFirst = roleManager.getPermissions(schema, roleToUpdate);
    assertEquals(SelectScope.ALL, psFirst.getTables().get(TABLE_PET).getSelect());

    String secondMutation =
        "mutation { change(roles: [{"
            + "name: \""
            + roleToUpdate
            + "\", "
            + "tables: [{table: \""
            + TABLE_PET
            + "\", select: NONE, insert: NONE, update: NONE, delete: NONE}]"
            + "}]) { message } }";
    execute(secondMutation);

    PermissionSet psSecond = roleManager.getPermissions(schema, roleToUpdate);
    assertEquals(SelectScope.NONE, psSecond.getTables().get(TABLE_PET).getSelect());
    assertEquals(SelectScope.NONE, psSecond.getTables().get(TABLE_PET).getInsert());
  }

  @Test
  void changeRoles_invalidViewModeOnInsert_throwsMolgenisException() {
    String mutation =
        "mutation { change(roles: [{"
            + "name: \"badscope\", "
            + "tables: [{table: \""
            + TABLE_PET
            + "\", insert: EXISTS}]"
            + "}]) { message } }";

    MolgenisException thrown =
        assertThrows(MolgenisException.class, () -> executor.executeWithoutSession(mutation));
    assertTrue(
        thrown.getMessage().contains("EXISTS"), "Expected error message to mention EXISTS scope");
  }

  @Test
  void changeRoles_systemRoleInInput_noExceptionAndSystemRoleStillPresent() throws IOException {
    String sysRoleSibling = "sibling";
    String mutation =
        "mutation { change(roles: ["
            + "{name: \"Viewer\", tables: [{table: \""
            + TABLE_PET
            + "\", select: ALL}]}, "
            + "{name: \""
            + sysRoleSibling
            + "\", tables: [{table: \""
            + TABLE_PET
            + "\", select: OWN}], changeOwner: false, changeGroup: false}"
            + "]) { message } }";

    JsonNode result = execute(mutation);
    assertNotNull(result.at("/change/message").textValue());

    boolean viewerStillPresent =
        schema.getRoleInfos().stream().anyMatch(role -> "Viewer".equals(role.name()));
    assertTrue(viewerStillPresent, "Viewer system role should still be visible in getRoleInfos");

    PermissionSet customPs = roleManager.getPermissions(schema, sysRoleSibling);
    assertEquals(SelectScope.OWN, customPs.getTables().get(TABLE_PET).getSelect());
  }

  @Test
  void changeRoles_managerUser_canGrantCustomRole() throws IOException {
    String mutation = buildGrantMutation("managertarget");
    try {
      database.setActiveUser(USER_MANAGER);
      GraphqlExecutor userExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      JsonNode result = executeAs(userExecutor, mutation);
      assertNotNull(result.at("/change/message").textValue());
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  void changeRoles_ownerUser_canGrantCustomRole() throws IOException {
    String mutation = buildGrantMutation("ownertarget");
    try {
      database.setActiveUser(USER_OWNER);
      GraphqlExecutor userExecutor =
          new GraphqlExecutor(database.getSchema(SCHEMA_NAME), new TaskServiceInMemory());
      JsonNode result = executeAs(userExecutor, mutation);
      assertNotNull(result.at("/change/message").textValue());
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  void changeRoles_adminUser_canGrantCustomRole() throws IOException {
    String mutation = buildGrantMutation("admintarget");
    JsonNode result = execute(mutation);
    assertNotNull(result.at("/change/message").textValue());
  }

  @Test
  void changeRoles_editorUser_deniedGrantingCustomRole() {
    String mutation = buildGrantMutation("editortarget");
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
  }

  @Test
  void changeRoles_viewerUser_deniedGrantingCustomRole() {
    String mutation = buildGrantMutation("viewertarget");
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
  }

  @Test
  void changeRoles_noRoleUser_deniedGrantingCustomRole() {
    try {
      database.setActiveUser(USER_NOROLE);
      MolgenisException thrown =
          assertThrows(
              MolgenisException.class,
              () ->
                  GraphqlPermissionFieldFactory.requireManagerOrOwner(
                      database, database.getSchema(SCHEMA_NAME)));
      assertTrue(
          thrown.getMessage().contains("Manager") || thrown.getMessage().contains("Owner"),
          "Error should mention Manager or Owner requirement");
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  void customRolesQuery_roundTrip_returnsSetPermissions() throws IOException {
    String roleA = "queryRoleA";
    execute(
        "mutation { change(roles: [{"
            + "name: \""
            + roleA
            + "\", "
            + "tables: [{table: \""
            + TABLE_PET
            + "\", select: GROUP, insert: OWN, update: NONE, delete: NONE}], "
            + "changeOwner: true, changeGroup: false"
            + "}]) { message } }");

    JsonNode customRoles =
        execute(
                "{_schema { customRoles { name tables { table select insert update delete }"
                    + " changeOwner changeGroup } } }")
            .at("/_schema/customRoles");
    assertFalse(customRoles.isEmpty(), "customRoles should not be empty");

    JsonNode found = findRoleByName(customRoles, roleA);
    assertNotNull(found, "Role " + roleA + " should appear in customRoles");
    assertEquals("GROUP", found.at("/tables/0/select").asText());
    assertEquals("OWN", found.at("/tables/0/insert").asText());
    assertEquals("NONE", found.at("/tables/0/update").asText());
    assertEquals("NONE", found.at("/tables/0/delete").asText());
    assertTrue(found.at("/changeOwner").asBoolean());
    assertFalse(found.at("/changeGroup").asBoolean());
  }

  @Test
  void customRolesQuery_multipleCustomRoles_allListed() throws IOException {
    String roleB = "multiRoleB";
    String roleC = "multiRoleC";
    execute(
        "mutation { change(roles: ["
            + "{name: \""
            + roleB
            + "\", tables: [{table: \""
            + TABLE_PET
            + "\", select: ALL, insert: ALL, update: ALL, delete: ALL}]}, "
            + "{name: \""
            + roleC
            + "\", tables: [{table: \""
            + TABLE_PET
            + "\", select: OWN, insert: NONE, update: NONE, delete: NONE}]}"
            + "]) { message } }");

    JsonNode customRoles =
        execute("{_schema { customRoles { name } } }").at("/_schema/customRoles");
    assertNotNull(findRoleByName(customRoles, roleB), "Role " + roleB + " should appear");
    assertNotNull(findRoleByName(customRoles, roleC), "Role " + roleC + " should appear");
  }

  @Test
  void customRolesQuery_noCustomRoles_emptyList() throws IOException {
    Database emptyDb = TestDatabaseFactory.getTestDatabase();
    String emptySchemaName = "TGraphqlPermEmpty";
    emptyDb.dropSchemaIfExists(emptySchemaName);
    Schema emptySchema = emptyDb.createSchema(emptySchemaName);
    GraphqlExecutor emptyExecutor = new GraphqlExecutor(emptySchema, new TaskServiceInMemory());

    String json =
        convertExecutionResultToJson(
            emptyExecutor.executeWithoutSession("{_schema { customRoles { name } } }"));
    JsonNode customRoles = new ObjectMapper().readTree(json).at("/data/_schema/customRoles");
    assertTrue(customRoles.isArray() && customRoles.isEmpty(), "customRoles should be empty");
    emptyDb.dropSchemaIfExists(emptySchemaName);
  }

  @Test
  void customRolesQuery_systemRolesAbsent_inCustomRoles() throws IOException {
    JsonNode customRoles =
        execute("{_schema { customRoles { name } } }").at("/_schema/customRoles");
    for (JsonNode roleNode : customRoles) {
      String name = roleNode.at("/name").asText();
      assertFalse(
          isSystemRoleName(name), "System role '" + name + "' must not appear in customRoles");
    }
  }

  @Test
  void rolesQuery_systemRolesStillPresent_inLegacyRoles() throws IOException {
    JsonNode roles = execute("{_schema { roles { name system } } }").at("/_schema/roles");
    boolean hasSystemRole = false;
    for (JsonNode roleNode : roles) {
      if (roleNode.at("/system").asBoolean()) {
        hasSystemRole = true;
        break;
      }
    }
    assertTrue(hasSystemRole, "Legacy roles field should still contain system roles");
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
        + "tables: [{table: \""
        + TABLE_PET
        + "\", select: OWN}]"
        + "}]) { message } }";
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
