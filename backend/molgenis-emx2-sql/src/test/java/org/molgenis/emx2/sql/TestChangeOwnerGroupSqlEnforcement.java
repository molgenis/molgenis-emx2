package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.sql.SQLException;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TablePermission;

public class TestChangeOwnerGroupSqlEnforcement {

  private static final String SCHEMA_NAME = "RmColGrantEnfA";
  private static final String TEST_USER_ALICE = "RmColGrantEnfAlice";
  private static final String TEST_USER_OTHER = "RmColGrantEnfOther";

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
    if (!db.hasUser(TEST_USER_OTHER)) db.addUser(TEST_USER_OTHER);
  }

  private PermissionSet allScopesWithOwner() {
    return new PermissionSet()
        .putTable(
            OWNER_TABLE,
            new TablePermission(OWNER_TABLE)
                .select(SelectScope.OWN)
                .insert(UpdateScope.ALL)
                .update(UpdateScope.ALL));
  }

  private PermissionSet allScopesWithGroup() {
    return new PermissionSet()
        .putTable(
            GROUP_TABLE,
            new TablePermission(GROUP_TABLE)
                .select(SelectScope.ALL)
                .insert(UpdateScope.ALL)
                .update(UpdateScope.ALL));
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
    schema.getTable(OWNER_TABLE).getMetadata().setRlsEnabled(true);
    roleManager.createRole(schema, ENFORCE_ROLE, "enforcer role");
    roleManager.setPermissions(schema, ENFORCE_ROLE, allScopesWithOwner());
    roleManager.grantRoleToUser(schema, ENFORCE_ROLE, TEST_USER_ALICE);
    db.setActiveUser(TEST_USER_ALICE);
    schema.getTable(OWNER_TABLE).insert(new Row().setString("name", "s-1"));

    try {
      DataAccessException thrown =
          assertThrows(
              DataAccessException.class,
              () ->
                  jooq.execute(
                      "UPDATE {0} SET mg_owner = {1} WHERE name = 's-1'",
                      name(SCHEMA_NAME, OWNER_TABLE), inline(TEST_USER_OTHER)),
              "UPDATE setting mg_owner without changeOwner flag must throw permission denied");
      assertInsufficientPrivilege(thrown);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void setPermissions_changeOwnerTrueAllowsMgOwnerUpdate() {
    schema.create(table(OWNER_TABLE, column("name").setPkey()));
    schema.getTable(OWNER_TABLE).getMetadata().setRlsEnabled(true);
    roleManager.createRole(schema, ENFORCE_ROLE, "enforcer role");
    PermissionSet withOwnerFlag = allScopesWithOwner().setChangeOwner(true);
    roleManager.setPermissions(schema, ENFORCE_ROLE, withOwnerFlag);
    roleManager.grantRoleToUser(schema, ENFORCE_ROLE, TEST_USER_ALICE);
    db.setActiveUser(TEST_USER_ALICE);
    schema.getTable(OWNER_TABLE).insert(new Row().setString("name", "s-1"));

    try {
      assertDoesNotThrow(
          () ->
              jooq.execute(
                  "UPDATE {0} SET mg_owner = {1} WHERE name = 's-1'",
                  name(SCHEMA_NAME, OWNER_TABLE), inline(TEST_USER_OTHER)),
          "UPDATE setting mg_owner with changeOwner=true must succeed");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void setPermissions_sqlLevelRejectsMgGroupsUpdateWithoutFlag() {
    schema.create(table(GROUP_TABLE, column("id").setPkey()));
    schema.getTable(GROUP_TABLE).getMetadata().setRlsEnabled(true);
    roleManager.createRole(schema, ENFORCE_ROLE, "enforcer role");
    roleManager.setPermissions(schema, ENFORCE_ROLE, allScopesWithGroup());
    roleManager.grantRoleToUser(schema, ENFORCE_ROLE, TEST_USER_ALICE);
    schema
        .getTable(GROUP_TABLE)
        .insert(new Row().setString("id", "g-1").setStringArray("mg_groups", new String[] {"grp"}));

    db.setActiveUser(TEST_USER_ALICE);
    try {
      DataAccessException thrown =
          assertThrows(
              DataAccessException.class,
              () ->
                  jooq.execute(
                      "UPDATE {0} SET mg_groups = NULL WHERE id = 'g-1'",
                      name(SCHEMA_NAME, GROUP_TABLE)),
              "UPDATE clearing mg_groups without changeGroup flag must throw permission denied");
      assertInsufficientPrivilege(thrown);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void setPermissions_changeGroupTrueAllowsMgGroupsUpdate() {
    schema.create(table(GROUP_TABLE, column("id").setPkey()));
    schema.getTable(GROUP_TABLE).getMetadata().setRlsEnabled(true);
    roleManager.createRole(schema, ENFORCE_ROLE, "enforcer role");
    PermissionSet withGroupFlag = allScopesWithGroup().setChangeGroup(true);
    roleManager.setPermissions(schema, ENFORCE_ROLE, withGroupFlag);
    roleManager.grantRoleToUser(schema, ENFORCE_ROLE, TEST_USER_ALICE);
    schema
        .getTable(GROUP_TABLE)
        .insert(new Row().setString("id", "g-1").setStringArray("mg_groups", new String[] {"grp"}));

    db.setActiveUser(TEST_USER_ALICE);
    try {
      assertDoesNotThrow(
          () ->
              jooq.execute(
                  "UPDATE {0} SET mg_groups = NULL WHERE id = 'g-1'",
                  name(SCHEMA_NAME, GROUP_TABLE)),
          "UPDATE clearing mg_groups with changeGroup=true must succeed");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void setPermissions_sqlLevelRejectsMgOwnerInsertWithoutFlag() {
    schema.create(table(OWNER_TABLE, column("name").setPkey()));
    schema.getTable(OWNER_TABLE).getMetadata().setRlsEnabled(true);
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
                      "INSERT INTO {0} (name, mg_owner) VALUES ('s-2', {1})",
                      name(SCHEMA_NAME, OWNER_TABLE), inline(TEST_USER_OTHER)),
              "INSERT with explicit mg_owner without changeOwner flag must throw permission denied");
      assertInsufficientPrivilege(thrown);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void setPermissions_changeOwnerTrueAllowsMgOwnerInsert() {
    schema.create(table(OWNER_TABLE, column("name").setPkey()));
    schema.getTable(OWNER_TABLE).getMetadata().setRlsEnabled(true);
    roleManager.createRole(schema, ENFORCE_ROLE, "enforcer role");
    PermissionSet withOwnerFlag = allScopesWithOwner().setChangeOwner(true);
    roleManager.setPermissions(schema, ENFORCE_ROLE, withOwnerFlag);
    roleManager.grantRoleToUser(schema, ENFORCE_ROLE, TEST_USER_ALICE);

    db.setActiveUser(TEST_USER_ALICE);
    try {
      assertDoesNotThrow(
          () ->
              jooq.execute(
                  "INSERT INTO {0} (name, mg_owner) VALUES ('s-2', {1})",
                  name(SCHEMA_NAME, OWNER_TABLE), inline(TEST_USER_OTHER)),
          "INSERT with explicit mg_owner with changeOwner=true must succeed");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void setPermissions_changeOwnerFlagRoundTripReflectedInStorage() {
    schema.create(table(OWNER_TABLE, column("name").setPkey()));
    schema.getTable(OWNER_TABLE).getMetadata().setRlsEnabled(true);
    roleManager.createRole(schema, ENFORCE_ROLE, "enforcer role");

    roleManager.setPermissions(schema, ENFORCE_ROLE, allScopesWithOwner());
    assertFalse(
        roleManager.getPermissions(schema, ENFORCE_ROLE).isChangeOwner(),
        "changeOwner must be false when not set");

    roleManager.setPermissions(schema, ENFORCE_ROLE, allScopesWithOwner().setChangeOwner(true));
    assertTrue(
        roleManager.getPermissions(schema, ENFORCE_ROLE).isChangeOwner(),
        "changeOwner must be true after setChangeOwner(true)");

    roleManager.setPermissions(schema, ENFORCE_ROLE, allScopesWithOwner());
    assertFalse(
        roleManager.getPermissions(schema, ENFORCE_ROLE).isChangeOwner(),
        "changeOwner must be false after flipping back to false");
  }
}
