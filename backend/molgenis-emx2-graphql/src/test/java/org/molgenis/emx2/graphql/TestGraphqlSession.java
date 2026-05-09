package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.TablePermission;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlRoleManager;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestGraphqlSession {

  private static final String SCHEMA_NAME = "TGraphqlSession";
  private static final String TABLE_NAME = "SessionItems";
  private static final String ROLE_ANALYST = "sess_analyst";
  private static final String USER_TEST = "sess_test_user";

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

    if (database.hasUser(USER_TEST)) {
      database.removeUser(USER_TEST);
    }
    database.addUser(USER_TEST);

    roleManager = ((SqlDatabase) database).getRoleManager();

    if (database.getSetting(Constants.MOLGENIS_JWT_SHARED_SECRET) == null) {
      database.setSetting(
          Constants.MOLGENIS_JWT_SHARED_SECRET, "test-jwt-secret-that-is-at-least-32chars");
    }

    executor = new GraphqlExecutor(schema, new TaskServiceInMemory());
  }

  @Test
  void sessionPermissions_currentUserSeesOwnPermissions() throws IOException {
    roleManager.createRole(SCHEMA_NAME, ROLE_ANALYST);
    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_NAME);
    tp.setSelect(SelectScope.ALL);
    ps.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, ROLE_ANALYST, ps);
    roleManager.grantRoleToUser(schema, ROLE_ANALYST, USER_TEST);

    try {
      database.setActiveUser(USER_TEST);
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
    } finally {
      database.becomeAdmin();
      roleManager.revokeRoleFromUser(schema, ROLE_ANALYST, USER_TEST);
      roleManager.deleteRole(SCHEMA_NAME, ROLE_ANALYST);
    }
  }

  @Test
  void sessionPermissions_exposesUnifiedSelect() throws IOException {
    roleManager.createRole(SCHEMA_NAME, ROLE_ANALYST);
    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_NAME);
    tp.setSelect(SelectScope.AGGREGATE);
    ps.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, ROLE_ANALYST, ps);
    roleManager.grantRoleToUser(schema, ROLE_ANALYST, USER_TEST);

    try {
      database.setActiveUser(USER_TEST);
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
    } finally {
      database.becomeAdmin();
      roleManager.revokeRoleFromUser(schema, ROLE_ANALYST, USER_TEST);
      roleManager.deleteRole(SCHEMA_NAME, ROLE_ANALYST);
    }
  }

  private JsonNode executeQuery(GraphqlExecutor exec, String query) throws IOException {
    String json = convertExecutionResultToJson(exec.executeWithoutSession(query));
    JsonNode root = new ObjectMapper().readTree(json);
    assertNull(root.get("errors"), "GraphQL errors: " + root.get("errors"));
    return root.get("data");
  }
}
