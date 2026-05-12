package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;

class TestUpdateScope {

  private static final String SCHEMA_NAME = "TestUpdateScope";
  private static final String TABLE_NAME = "Items";
  private static final String GROUP_RED = "groupRed";
  private static final String GROUP_BLUE = "groupBlue";
  private static final String ROLE_ALL = "updateRoleAll";
  private static final String ROLE_GROUP = "updateRoleGroup";
  private static final String ROLE_OWN = "updateRoleOwn";
  private static final String ROLE_NONE = "updateRoleNone";
  private static final String USER_ALICE = "TusAlice";
  private static final String USER_BOB = "TusBob";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final DSLContext jooq = ((SqlDatabase) db).getJooq();
  private static final SqlRoleManager roleManager = new SqlRoleManager((SqlDatabase) db);

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(TABLE_NAME).add(column("id").setPkey()).add(column("val")));

    roleManager.createGroup(schema, GROUP_RED);
    roleManager.createGroup(schema, GROUP_BLUE);

    if (!db.hasUser(USER_ALICE)) db.addUser(USER_ALICE);
    if (!db.hasUser(USER_BOB)) db.addUser(USER_BOB);

    roleManager.createRole(schema, ROLE_ALL, "");
    roleManager.createRole(schema, ROLE_GROUP, "");
    roleManager.createRole(schema, ROLE_OWN, "");
    roleManager.createRole(schema, ROLE_NONE, "");

    ((SqlTableMetadata) schema.getTable(TABLE_NAME).getMetadata()).setRlsEnabled(true);

    configureRole(ROLE_ALL, SelectScope.ALL, UpdateScope.ALL, UpdateScope.ALL, UpdateScope.ALL);
    configureRole(
        ROLE_GROUP, SelectScope.GROUP, UpdateScope.GROUP, UpdateScope.GROUP, UpdateScope.GROUP);
    configureRole(ROLE_OWN, SelectScope.OWN, UpdateScope.OWN, UpdateScope.OWN, UpdateScope.OWN);
    configureRole(
        ROLE_NONE, SelectScope.NONE, UpdateScope.NONE, UpdateScope.NONE, UpdateScope.NONE);

    insertRow("row-alice-red", "v1", USER_ALICE, new String[] {GROUP_RED});
    insertRow("row-alice-nogroup", "v4", USER_ALICE, new String[] {});
    insertRow("row-bob-red", "v2", USER_BOB, new String[] {GROUP_RED});
    insertRow("row-bob-nogroup", "v5", USER_BOB, new String[] {});
    insertRow("row-bob-blue", "v3", USER_BOB, new String[] {GROUP_BLUE});
  }

  private void configureRole(
      String role, SelectScope select, UpdateScope insert, UpdateScope update, UpdateScope delete) {
    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_NAME);
    tp.select(select);
    tp.insert(insert);
    tp.update(update);
    tp.delete(delete);
    ps.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, role, ps);
  }

  private void insertRow(String id, String val, String owner, String[] groups) {
    jooq.execute(
        "INSERT INTO \""
            + SCHEMA_NAME
            + "\".\""
            + TABLE_NAME
            + "\" (id, val, mg_owner, mg_groups) VALUES (?, ?, ?, ?)",
        id,
        val,
        owner,
        groups);
  }

  private boolean tryUpdateAsUser(String userName, String rowId, String newVal) {
    db.setActiveUser(userName);
    try {
      int affected =
          schema
              .getTable(TABLE_NAME)
              .update(new Row().setString("id", rowId).setString("val", newVal));
      return affected > 0;
    } catch (Exception ex) {
      return false;
    } finally {
      db.becomeAdmin();
    }
  }

  private boolean tryInsertAsUser(String userName, String id, String owner, String[] groups) {
    db.setActiveUser(userName);
    try {
      jooq.execute(
          "INSERT INTO \""
              + SCHEMA_NAME
              + "\".\""
              + TABLE_NAME
              + "\" (id, val, mg_owner, mg_groups) VALUES (?, ?, ?, ?)",
          id,
          "v",
          owner,
          groups);
      return true;
    } catch (Exception ex) {
      return false;
    } finally {
      db.becomeAdmin();
    }
  }

  private boolean tryDeleteAsUser(String userName, String rowId) {
    db.setActiveUser(userName);
    try {
      int affected = schema.getTable(TABLE_NAME).delete(new Row().setString("id", rowId));
      return affected > 0;
    } catch (Exception ex) {
      return false;
    } finally {
      db.becomeAdmin();
    }
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void allScopeCanUpdateAnyRow() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_ALL);
    try {
      assertTrue(
          tryUpdateAsUser(USER_ALICE, "row-bob-red", "updated"),
          "UpdateScope.ALL must allow updating rows owned by others");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_ALL);
    }
  }

  @Test
  void groupScopeCanUpdateGroupRow() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_GROUP);
    try {
      assertTrue(
          tryUpdateAsUser(USER_ALICE, "row-bob-red", "updated"),
          "UpdateScope.GROUP must allow updating rows in own group");
      assertFalse(
          tryUpdateAsUser(USER_ALICE, "row-bob-blue", "updated"),
          "UpdateScope.GROUP must not allow updating rows in foreign group");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_GROUP);
    }
  }

  @Test
  void ownScopeCanUpdateOnlyOwnRow() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_OWN);
    try {
      assertTrue(
          tryUpdateAsUser(USER_ALICE, "row-alice-nogroup", "updated"),
          "UpdateScope.OWN must allow updating own row (no groups so WITH CHECK group-subset passes)");
      assertFalse(
          tryUpdateAsUser(USER_ALICE, "row-bob-nogroup", "updated"),
          "UpdateScope.OWN must not allow updating rows owned by others");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_OWN);
    }
  }

  @Test
  void noneScopeBlocksInsert() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_NONE);
    try {
      assertFalse(
          tryInsertAsUser(USER_ALICE, "new-row", USER_ALICE, new String[] {GROUP_RED}),
          "UpdateScope.NONE must block INSERT");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_NONE);
    }
  }

  @Test
  void noneScopeBlocksDelete() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_NONE);
    try {
      assertFalse(
          tryDeleteAsUser(USER_ALICE, "row-alice-red"), "UpdateScope.NONE must block DELETE");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_NONE);
    }
  }

  @Test
  void allScopeCanInsertAndDelete() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_ALL);
    try {
      assertTrue(
          tryInsertAsUser(USER_ALICE, "new-all", USER_ALICE, new String[] {GROUP_RED}),
          "UpdateScope.ALL must allow INSERT");
      assertTrue(
          tryDeleteAsUser(USER_ALICE, "new-all"),
          "UpdateScope.ALL must allow DELETE of own insert");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_ALL);
    }
  }
}
