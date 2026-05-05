package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.UpdateScope;
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
    assertEquals(UpdateScope.OWN, petPerms.getInsert());
    assertEquals(UpdateScope.OWN, petPerms.getUpdate());
    assertEquals(UpdateScope.NONE, petPerms.getDelete());
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
    assertEquals(UpdateScope.NONE, psSecond.getTables().get(TABLE_PET).getInsert());
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
  void rolesQuery_roundTrip_returnsSetPermissions() throws IOException {
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

    JsonNode roles =
        execute(
                "{_schema { roles { name tables { table select insert update delete }"
                    + " changeOwner changeGroup } } }")
            .at("/_schema/roles");
    assertFalse(roles.isEmpty(), "roles should not be empty");

    JsonNode found = findRoleByName(roles, roleA);
    assertNotNull(found, "Role " + roleA + " should appear in roles");
    assertEquals("GROUP", found.at("/tables/0/select").asText());
    assertEquals("OWN", found.at("/tables/0/insert").asText());
    assertEquals("NONE", found.at("/tables/0/update").asText());
    assertEquals("NONE", found.at("/tables/0/delete").asText());
    assertTrue(found.at("/changeOwner").asBoolean());
    assertFalse(found.at("/changeGroup").asBoolean());
  }

  @Test
  void rolesQuery_multipleCustomRoles_allListed() throws IOException {
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
    execute(
        "mutation { change(roles: [{"
            + "name: \""
            + roleWithDesc
            + "\", description: \"team alpha role\", "
            + "tables: [{table: \""
            + TABLE_PET
            + "\", select: ALL}]"
            + "}]) { message } }");

    JsonNode roles =
        execute("{_schema { roles { name description tables { table select } } } }")
            .at("/_schema/roles");
    JsonNode found = findRoleByName(roles, roleWithDesc);
    assertNotNull(found, "Role " + roleWithDesc + " should appear in roles");
    assertEquals("team alpha role", found.at("/description").asText());

    execute(
        "mutation { change(roles: [{"
            + "name: \""
            + roleWithDesc
            + "\", "
            + "tables: [{table: \""
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
  void rolesQuery_systemRolesPresent_inMergedRoles() throws IOException {
    JsonNode roles =
        execute(
                "{_schema { roles { name description tables { table select insert update delete } } } }")
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
    String roleWithSchema = "schemaRoleA";
    execute(
        "mutation { change(roles: [{"
            + "name: \""
            + roleWithSchema
            + "\", "
            + "tables: [{table: \""
            + TABLE_PET
            + "\", select: ALL}]"
            + "}]) { message } }");

    JsonNode roles = execute("{_schema { roles { name schemaName } } }").at("/_schema/roles");
    JsonNode found = findRoleByName(roles, roleWithSchema);
    assertNotNull(found, "Role should appear in roles");
    assertEquals(
        SCHEMA_NAME, found.at("/schemaName").asText(), "schemaName field must match schema name");
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
