package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.ReferenceScope;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.TablePermission;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlRoleManager;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestGraphqlSession {

  private static final String SCHEMA_NAME = "TGraphqlSession";
  private static final String TABLE_NAME = "SessionItems";
  private static final String ONTOLOGY_TABLE_NAME = "SessionTerms";
  private static final String ROLE_ANALYST = "sess_analyst";
  private static final String USER_TEST = "sess_test_user";
  private static final String USER_NO_ROLE = "sess_norole_user";
  private static final String USER_VICTIM = "sess_victim";

  private static Database database;
  private static SqlRoleManager roleManager;
  private static Schema schema;
  private static GraphqlExecutor executor;

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    schema = database.createSchema(SCHEMA_NAME);
    schema.create(
        TableMetadata.table(TABLE_NAME).add(Column.column("id", ColumnType.STRING).setKey(1)));
    schema.create(
        TableMetadata.table(ONTOLOGY_TABLE_NAME)
            .setTableType(TableType.ONTOLOGIES)
            .add(Column.column("name", ColumnType.STRING).setKey(1)));

    if (database.hasUser(USER_TEST)) {
      database.removeUser(USER_TEST);
    }
    database.addUser(USER_TEST);

    if (database.hasUser(USER_NO_ROLE)) {
      database.removeUser(USER_NO_ROLE);
    }
    database.addUser(USER_NO_ROLE);

    database.setUserPassword(USER_VICTIM, "initial_password");

    roleManager = ((SqlDatabase) database).getRoleManager();

    if (database.getSetting(Constants.MOLGENIS_JWT_SHARED_SECRET) == null) {
      database.setSetting(
          Constants.MOLGENIS_JWT_SHARED_SECRET, "test-jwt-secret-that-is-at-least-32chars");
    }

    executor = new GraphqlExecutor(schema, new TaskServiceInMemory());
  }

  @Test
  void sessionPermissions_currentUserSeesOwnPermissions() throws IOException {
    roleManager.createRole(schema, ROLE_ANALYST, "");
    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_NAME);
    tp.select(SelectScope.ALL);
    ps.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, ROLE_ANALYST, ps);
    roleManager.grantRoleToUser(schema, ROLE_ANALYST, USER_TEST);

    try {
      database.setActiveUser(USER_TEST);
      JsonNode result =
          executeQuery(
              executor,
              "{_session{tablePermissions{name canView canAggregate canInsert canUpdate canDelete}}}");
      database.becomeAdmin();
      JsonNode perms = result.at("/_session/tablePermissions");
      assertFalse(perms.isMissingNode(), "tablePermissions must be present");
      assertTrue(perms.isArray(), "tablePermissions must be array");

      boolean found = false;
      for (JsonNode perm : perms) {
        if (TABLE_NAME.equals(perm.at("/name").asText())
            && perm.at("/canView").asBoolean()
            && perm.at("/canAggregate").asBoolean()) {
          found = true;
        }
      }
      assertTrue(found, "Expected canView=true and canAggregate=true for " + TABLE_NAME);
    } finally {
      database.becomeAdmin();
      roleManager.revokeRoleFromUser(schema, ROLE_ANALYST, USER_TEST);
      roleManager.deleteRole(SCHEMA_NAME, ROLE_ANALYST);
    }
  }

  @Test
  void sessionPermissions_exposesUnifiedSelect() throws IOException {
    roleManager.createRole(schema, ROLE_ANALYST, "");
    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_NAME);
    tp.select(SelectScope.AGGREGATE);
    ps.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, ROLE_ANALYST, ps);
    roleManager.grantRoleToUser(schema, ROLE_ANALYST, USER_TEST);

    try {
      database.setActiveUser(USER_TEST);
      JsonNode result =
          executeQuery(
              executor,
              "{_session{tablePermissions{name canView canAggregate canInsert canUpdate canDelete}}}");
      database.becomeAdmin();
      JsonNode perms = result.at("/_session/tablePermissions");
      assertFalse(perms.isMissingNode(), "tablePermissions must be present");

      boolean found = false;
      for (JsonNode perm : perms) {
        if (TABLE_NAME.equals(perm.at("/name").asText())
            && !perm.at("/canView").asBoolean()
            && perm.at("/canAggregate").asBoolean()) {
          found = true;
        }
      }
      assertTrue(
          found,
          "Expected canView=false and canAggregate=true for "
              + TABLE_NAME
              + " with AGGREGATE select scope");
    } finally {
      database.becomeAdmin();
      roleManager.revokeRoleFromUser(schema, ROLE_ANALYST, USER_TEST);
      roleManager.deleteRole(SCHEMA_NAME, ROLE_ANALYST);
    }
  }

  private static final String SESSION_QUERY =
      "{_session{tablePermissions{name canView canAggregate canInsert canUpdate canDelete canReference}}}";

  @Test
  void sessionPermissions_viewImpliesCanReference() throws IOException {
    roleManager.createRole(schema, ROLE_ANALYST, "");
    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_NAME);
    tp.select(SelectScope.ALL);
    ps.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, ROLE_ANALYST, ps);
    roleManager.grantRoleToUser(schema, ROLE_ANALYST, USER_TEST);

    try {
      database.setActiveUser(USER_TEST);
      JsonNode result = executeQuery(executor, SESSION_QUERY);
      database.becomeAdmin();
      JsonNode perms = result.at("/_session/tablePermissions");

      boolean found = false;
      for (JsonNode perm : perms) {
        if (TABLE_NAME.equals(perm.at("/name").asText())) {
          assertTrue(perm.at("/canView").asBoolean(), "VIEW=ALL implies canView");
          assertTrue(perm.at("/canReference").asBoolean(), "VIEW=ALL implies canReference");
          found = true;
        }
      }
      assertTrue(found, "Permission entry for " + TABLE_NAME + " must exist");
    } finally {
      database.becomeAdmin();
      roleManager.revokeRoleFromUser(schema, ROLE_ANALYST, USER_TEST);
      roleManager.deleteRole(SCHEMA_NAME, ROLE_ANALYST);
    }
  }

  @Test
  void sessionPermissions_referenceOnlyGivesCanReferenceWithoutCanView() throws IOException {
    roleManager.createRole(schema, ROLE_ANALYST, "");
    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_NAME);
    tp.reference(ReferenceScope.ALL);
    ps.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, ROLE_ANALYST, ps);
    roleManager.grantRoleToUser(schema, ROLE_ANALYST, USER_TEST);

    try {
      database.setActiveUser(USER_TEST);
      JsonNode result = executeQuery(executor, SESSION_QUERY);
      database.becomeAdmin();
      JsonNode perms = result.at("/_session/tablePermissions");

      boolean found = false;
      for (JsonNode perm : perms) {
        if (TABLE_NAME.equals(perm.at("/name").asText())) {
          assertFalse(perm.at("/canView").asBoolean(), "REFERENCE-only must not grant canView");
          assertFalse(perm.at("/canInsert").asBoolean(), "REFERENCE-only must not grant canInsert");
          assertFalse(perm.at("/canUpdate").asBoolean(), "REFERENCE-only must not grant canUpdate");
          assertFalse(perm.at("/canDelete").asBoolean(), "REFERENCE-only must not grant canDelete");
          assertTrue(perm.at("/canReference").asBoolean(), "REFERENCE=ALL grants canReference");
          found = true;
        }
      }
      assertTrue(found, "Permission entry for " + TABLE_NAME + " must exist");
    } finally {
      database.becomeAdmin();
      roleManager.revokeRoleFromUser(schema, ROLE_ANALYST, USER_TEST);
      roleManager.deleteRole(SCHEMA_NAME, ROLE_ANALYST);
    }
  }

  @Test
  void sessionPermissions_privacyScopeCount_doesNotGrantCanReference() throws IOException {
    roleManager.createRole(schema, ROLE_ANALYST, "");
    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_NAME);
    tp.select(SelectScope.COUNT);
    ps.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, ROLE_ANALYST, ps);
    roleManager.grantRoleToUser(schema, ROLE_ANALYST, USER_TEST);

    try {
      database.setActiveUser(USER_TEST);
      JsonNode result = executeQuery(executor, SESSION_QUERY);
      database.becomeAdmin();
      JsonNode perms = result.at("/_session/tablePermissions");

      boolean found = false;
      for (JsonNode perm : perms) {
        if (TABLE_NAME.equals(perm.at("/name").asText())) {
          assertFalse(perm.at("/canView").asBoolean(), "COUNT scope must not grant canView");
          assertFalse(
              perm.at("/canReference").asBoolean(),
              "COUNT privacy scope must not grant canReference");
          found = true;
        }
      }
      assertTrue(found, "Permission entry for " + TABLE_NAME + " must exist");
    } finally {
      database.becomeAdmin();
      roleManager.revokeRoleFromUser(schema, ROLE_ANALYST, USER_TEST);
      roleManager.deleteRole(SCHEMA_NAME, ROLE_ANALYST);
    }
  }

  @Test
  void sessionPermissions_ontologyTable_alwaysVisibleWithCanReference() throws IOException {
    database.setActiveUser(USER_NO_ROLE);
    try {
      JsonNode result = executeQuery(executor, SESSION_QUERY);
      JsonNode perms = result.at("/_session/tablePermissions");

      boolean found = false;
      for (JsonNode perm : perms) {
        if (ONTOLOGY_TABLE_NAME.equals(perm.at("/name").asText())) {
          assertTrue(perm.at("/canView").asBoolean(), "Ontology table must have canView=true");
          assertTrue(
              perm.at("/canReference").asBoolean(), "Ontology table must have canReference=true");
          assertFalse(
              perm.at("/canInsert").asBoolean(), "Ontology table must have canInsert=false");
          assertFalse(
              perm.at("/canUpdate").asBoolean(), "Ontology table must have canUpdate=false");
          assertFalse(
              perm.at("/canDelete").asBoolean(), "Ontology table must have canDelete=false");
          found = true;
        }
      }
      assertTrue(
          found, "Ontology table " + ONTOLOGY_TABLE_NAME + " must appear in tablePermissions");
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  void changePassword_nonAdmin_cannotChangeAnotherUsersPassword() {
    database.setActiveUser(USER_TEST);
    try {
      String mutation =
          "mutation { changePassword(email: \""
              + USER_VICTIM
              + "\", password: \"hacked\") { status message } }";
      assertThrows(
          MolgenisException.class,
          () -> executeQuery(executor, mutation),
          "Non-admin must not change another user's password via email arg");
    } finally {
      database.becomeAdmin();
    }

    database.becomeAdmin();
    assertTrue(
        database.checkUserPassword(USER_VICTIM, "initial_password"),
        "Victim password must remain unchanged after rejected mutation");
  }

  @Test
  void changePassword_admin_canChangeAnotherUsersPassword() throws IOException {
    database.becomeAdmin();
    String mutation =
        "mutation { changePassword(email: \""
            + USER_VICTIM
            + "\", password: \"admin_changed\") { status message } }";
    JsonNode result = executeQuery(executor, mutation);
    assertEquals("SUCCESS", result.at("/changePassword/status").asText());

    assertTrue(
        database.checkUserPassword(USER_VICTIM, "admin_changed"),
        "Admin must be able to change victim password");

    database.setUserPassword(USER_VICTIM, "initial_password");
  }

  @Test
  void changePassword_nonAdmin_canChangeOwnPassword() throws IOException {
    database.setUserPassword(USER_TEST, "old_pass");
    database.setActiveUser(USER_TEST);
    try {
      String mutation = "mutation { changePassword(password: \"new_pass\") { status message } }";
      JsonNode result = executeQuery(executor, mutation);
      assertEquals("SUCCESS", result.at("/changePassword/status").asText());
    } finally {
      database.becomeAdmin();
    }

    database.becomeAdmin();
    assertTrue(
        database.checkUserPassword(USER_TEST, "new_pass"),
        "Non-admin must be able to change own password");
  }

  private JsonNode executeQuery(GraphqlExecutor exec, String query) throws IOException {
    String json = convertExecutionResultToJson(exec.executeWithoutSession(query));
    JsonNode root = new ObjectMapper().readTree(json);
    assertNull(root.get("errors"), "GraphQL errors: " + root.get("errors"));
    return root.get("data");
  }
}
