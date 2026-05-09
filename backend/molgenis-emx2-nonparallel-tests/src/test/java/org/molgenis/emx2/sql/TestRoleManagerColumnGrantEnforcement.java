package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.sql.SQLException;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TablePermission;

@Disabled(
    "Blocks on J.7 — requires column-level GRANTs for change_owner/change_group enforcement; see .plan/plans/rls_v4.md §J.7")
public class TestRoleManagerColumnGrantEnforcement {

  private static final String SCHEMA_NAME = "RmColGrantEnfA";
  private static final String TEST_USER_ALICE = "RmColGrantEnfAlice";

  private static final String OWNER_TABLE = "OwnerTbl";
  private static final String GROUP_TABLE = "GrpTbl";
  private static final String ENFORCE_ROLE = "enforcer";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final DSLContext jooq = ((SqlDatabase) db).getJooq();
  private static final SqlRoleManager roleManager = new SqlRoleManager((SqlDatabase) db);

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    if (!db.hasUser(TEST_USER_ALICE)) db.addUser(TEST_USER_ALICE);
  }

  private PermissionSet allScopesWithOwner() {
    return new PermissionSet()
        .putTable(
            OWNER_TABLE,
            new TablePermission(OWNER_TABLE)
                .setSelect(SelectScope.OWN)
                .setInsert(UpdateScope.ALL)
                .setUpdate(UpdateScope.ALL));
  }

  private PermissionSet allScopesWithGroup() {
    return new PermissionSet()
        .putTable(
            GROUP_TABLE,
            new TablePermission(GROUP_TABLE)
                .setSelect(SelectScope.GROUP)
                .setInsert(UpdateScope.ALL)
                .setUpdate(UpdateScope.ALL));
  }

  private List<String> fetchColumnGrants(
      String schemaName, String tableName, String fullRole, String verb) {
    return jooq
        .fetch(
            "SELECT column_name FROM information_schema.column_privileges "
                + "WHERE table_schema = {0} AND table_name = {1} "
                + "AND grantee = {2} AND privilege_type = {3}",
            inline(schemaName), inline(tableName), inline(fullRole), inline(verb))
        .stream()
        .map(r -> r.get("column_name", String.class))
        .toList();
  }

  private static void assertInsufficientPrivilege(DataAccessException thrown) {
    assertTrue(
        thrown.getCause() instanceof SQLException,
        "DataAccessException cause must be a SQLException, got: " + thrown.getCause());
    assertEquals(
        "42501",
        ((SQLException) thrown.getCause()).getSQLState(),
        "SQLException must carry SQLSTATE 42501 (insufficient_privilege)");
  }

  @Test
  void setPermissions_sqlLevelRejectsMgOwnerUpdateWithoutFlag() {
    schema.create(table(OWNER_TABLE, column("name").setPkey()));
    roleManager.createRole(schema, ENFORCE_ROLE, "enforcer role");
    roleManager.setPermissions(schema, ENFORCE_ROLE, allScopesWithOwner());
    roleManager.grantRoleToUser(schema, ENFORCE_ROLE, TEST_USER_ALICE);
    schema.getTable(OWNER_TABLE).insert(new Row().set("name", "s-1"));

    db.setActiveUser(TEST_USER_ALICE);
    try {
      DataAccessException thrown =
          assertThrows(
              DataAccessException.class,
              () ->
                  jooq.execute(
                      "UPDATE {0} SET mg_owner = 'other' WHERE name = 's-1'",
                      name(SCHEMA_NAME, OWNER_TABLE)),
              "UPDATE setting mg_owner without changeOwner flag must throw permission denied");
      assertInsufficientPrivilege(thrown);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void setPermissions_changeOwnerTrueAllowsMgOwnerUpdate() {
    schema.create(table(OWNER_TABLE, column("name").setPkey()));
    roleManager.createRole(schema, ENFORCE_ROLE, "enforcer role");
    PermissionSet withOwnerFlag = allScopesWithOwner().setChangeOwner(true);
    roleManager.setPermissions(schema, ENFORCE_ROLE, withOwnerFlag);
    roleManager.grantRoleToUser(schema, ENFORCE_ROLE, TEST_USER_ALICE);
    schema.getTable(OWNER_TABLE).insert(new Row().set("name", "s-1"));

    db.setActiveUser(TEST_USER_ALICE);
    try {
      assertDoesNotThrow(
          () ->
              jooq.execute(
                  "UPDATE {0} SET mg_owner = 'other' WHERE name = 's-1'",
                  name(SCHEMA_NAME, OWNER_TABLE)),
          "UPDATE setting mg_owner with changeOwner=true must succeed");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void setPermissions_sqlLevelRejectsMgGroupsUpdateWithoutFlag() {
    schema.create(table(GROUP_TABLE, column("id").setPkey()));
    roleManager.createRole(schema, ENFORCE_ROLE, "enforcer role");
    roleManager.setPermissions(schema, ENFORCE_ROLE, allScopesWithGroup());
    roleManager.grantRoleToUser(schema, ENFORCE_ROLE, TEST_USER_ALICE);
    schema.getTable(GROUP_TABLE).insert(new Row().set("id", "g-1"));

    db.setActiveUser(TEST_USER_ALICE);
    try {
      DataAccessException thrown =
          assertThrows(
              DataAccessException.class,
              () ->
                  jooq.execute(
                      "UPDATE {0} SET mg_groups = ARRAY['somegroup'] WHERE id = 'g-1'",
                      name(SCHEMA_NAME, GROUP_TABLE)),
              "UPDATE setting mg_groups without changeGroup flag must throw permission denied");
      assertInsufficientPrivilege(thrown);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void setPermissions_changeGroupTrueAllowsMgGroupsUpdate() {
    schema.create(table(GROUP_TABLE, column("id").setPkey()));
    roleManager.createRole(schema, ENFORCE_ROLE, "enforcer role");
    PermissionSet withGroupFlag = allScopesWithGroup().setChangeGroup(true);
    roleManager.setPermissions(schema, ENFORCE_ROLE, withGroupFlag);
    roleManager.grantRoleToUser(schema, ENFORCE_ROLE, TEST_USER_ALICE);
    schema.getTable(GROUP_TABLE).insert(new Row().set("id", "g-1"));

    db.setActiveUser(TEST_USER_ALICE);
    try {
      assertDoesNotThrow(
          () ->
              jooq.execute(
                  "UPDATE {0} SET mg_groups = ARRAY['somegroup'] WHERE id = 'g-1'",
                  name(SCHEMA_NAME, GROUP_TABLE)),
          "UPDATE setting mg_groups with changeGroup=true must succeed");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void setPermissions_sqlLevelRejectsMgOwnerInsertWithoutFlag() {
    schema.create(table(OWNER_TABLE, column("name").setPkey()));
    roleManager.createRole(schema, ENFORCE_ROLE, "enforcer role");
    roleManager.setPermissions(schema, ENFORCE_ROLE, allScopesWithOwner());
    roleManager.grantRoleToUser(schema, ENFORCE_ROLE, TEST_USER_ALICE);

    db.setActiveUser(TEST_USER_ALICE);
    try {
      DataAccessException thrown =
          assertThrows(
              DataAccessException.class,
              () ->
                  jooq.execute(
                      "INSERT INTO {0} (name, mg_owner) VALUES ('s-2', 'otheruser')",
                      name(SCHEMA_NAME, OWNER_TABLE)),
              "INSERT with explicit mg_owner without changeOwner flag must throw permission denied");
      assertInsufficientPrivilege(thrown);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void setPermissions_changeOwnerTrueAllowsMgOwnerInsert() {
    schema.create(table(OWNER_TABLE, column("name").setPkey()));
    roleManager.createRole(schema, ENFORCE_ROLE, "enforcer role");
    PermissionSet withOwnerFlag = allScopesWithOwner().setChangeOwner(true);
    roleManager.setPermissions(schema, ENFORCE_ROLE, withOwnerFlag);
    roleManager.grantRoleToUser(schema, ENFORCE_ROLE, TEST_USER_ALICE);

    db.setActiveUser(TEST_USER_ALICE);
    try {
      assertDoesNotThrow(
          () ->
              jooq.execute(
                  "INSERT INTO {0} (name, mg_owner) VALUES ('s-2', 'otheruser')",
                  name(SCHEMA_NAME, OWNER_TABLE)),
          "INSERT with explicit mg_owner with changeOwner=true must succeed");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void setPermissions_changeOwnerFlagRoundTripReflectedInColumnPrivileges() {
    schema.create(table(OWNER_TABLE, column("name").setPkey()));
    roleManager.createRole(schema, ENFORCE_ROLE, "enforcer role");
    String fullRole = SqlRoleManager.fullRoleName(SCHEMA_NAME, ENFORCE_ROLE);

    roleManager.setPermissions(schema, ENFORCE_ROLE, allScopesWithOwner());
    assertFalse(
        fetchColumnGrants(SCHEMA_NAME, OWNER_TABLE, fullRole, "INSERT")
            .contains(Constants.MG_OWNER_COLUMN),
        "mg_owner INSERT grant must be absent when changeOwner=false");
    assertFalse(
        fetchColumnGrants(SCHEMA_NAME, OWNER_TABLE, fullRole, "UPDATE")
            .contains(Constants.MG_OWNER_COLUMN),
        "mg_owner UPDATE grant must be absent when changeOwner=false");

    roleManager.setPermissions(schema, ENFORCE_ROLE, allScopesWithOwner().setChangeOwner(true));
    assertTrue(
        fetchColumnGrants(SCHEMA_NAME, OWNER_TABLE, fullRole, "INSERT")
            .contains(Constants.MG_OWNER_COLUMN),
        "mg_owner INSERT grant must be present when changeOwner=true");
    assertTrue(
        fetchColumnGrants(SCHEMA_NAME, OWNER_TABLE, fullRole, "UPDATE")
            .contains(Constants.MG_OWNER_COLUMN),
        "mg_owner UPDATE grant must be present when changeOwner=true");

    roleManager.setPermissions(schema, ENFORCE_ROLE, allScopesWithOwner());
    assertFalse(
        fetchColumnGrants(SCHEMA_NAME, OWNER_TABLE, fullRole, "INSERT")
            .contains(Constants.MG_OWNER_COLUMN),
        "mg_owner INSERT grant must be absent after changeOwner flipped back to false");
    assertFalse(
        fetchColumnGrants(SCHEMA_NAME, OWNER_TABLE, fullRole, "UPDATE")
            .contains(Constants.MG_OWNER_COLUMN),
        "mg_owner UPDATE grant must be absent after changeOwner flipped back to false");
  }
}
