package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestChangeGroup {

  private static final String SCHEMA_NAME = "TestChangeGroup";
  private static final String TABLE_NAME = "Items";
  private static final String GROUP_ONE = "groupOne";
  private static final String GROUP_TWO = "groupTwo";
  private static final String ROLE_NO_CG = "roleNoChangeGroup";
  private static final String ROLE_YES_CG = "roleYesChangeGroup";
  private static final String USER_ALICE = "TcgAlice";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final DSLContext jooq = ((SqlDatabase) db).getJooq();
  private static final SqlRoleManager roleManager = new SqlRoleManager((SqlDatabase) db);

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(TABLE_NAME).add(column("id").setPkey()).add(column("val")));
    roleManager.createGroup(schema, GROUP_ONE);
    roleManager.createGroup(schema, GROUP_TWO);
    if (!db.hasUser(USER_ALICE)) db.addUser(USER_ALICE);

    roleManager.createRole(SCHEMA_NAME, ROLE_NO_CG);
    roleManager.createRole(SCHEMA_NAME, ROLE_YES_CG);

    PermissionSet noCgPerms = new PermissionSet();
    noCgPerms.setChangeGroup(false);
    PermissionSet.TablePermissions tp = new PermissionSet.TablePermissions();
    tp.setSelect(SelectScope.ALL);
    tp.setInsert(UpdateScope.ALL);
    tp.setUpdate(UpdateScope.ALL);
    tp.setDelete(UpdateScope.ALL);
    noCgPerms.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, ROLE_NO_CG, noCgPerms);

    PermissionSet yesCgPerms = new PermissionSet();
    yesCgPerms.setChangeGroup(true);
    PermissionSet.TablePermissions tpYes = new PermissionSet.TablePermissions();
    tpYes.setSelect(SelectScope.ALL);
    tpYes.setInsert(UpdateScope.ALL);
    tpYes.setUpdate(UpdateScope.ALL);
    tpYes.setDelete(UpdateScope.ALL);
    yesCgPerms.putTable(TABLE_NAME, tpYes);
    roleManager.setPermissions(schema, ROLE_YES_CG, yesCgPerms);

    insertRowAsAdmin("row1", "val1", "MG_USER_admin", new String[] {GROUP_ONE});
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  private void insertRowAsAdmin(String id, String val, String owner, String[] groups) {
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

  @Test
  void updateGroupsBlockedWhenChangeGroupFalse() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CG);
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_TWO, USER_ALICE, ROLE_NO_CG);
    try {
      db.setActiveUser(USER_ALICE);
      assertThrows(
          DataAccessException.class,
          () ->
              jooq.execute(
                  "UPDATE \""
                      + SCHEMA_NAME
                      + "\".\""
                      + TABLE_NAME
                      + "\" SET mg_groups = ARRAY[?]::text[] WHERE id = 'row1'",
                  GROUP_TWO),
          "Changing mg_groups must be blocked when change_group=false");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CG);
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_TWO, USER_ALICE, ROLE_NO_CG);
    }
  }

  @Test
  void updateOtherColumnAllowedWhenChangeGroupFalse() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CG);
    try {
      db.setActiveUser(USER_ALICE);
      assertDoesNotThrow(
          () ->
              jooq.execute(
                  "UPDATE \""
                      + SCHEMA_NAME
                      + "\".\""
                      + TABLE_NAME
                      + "\" SET val = 'updated' WHERE id = 'row1'"),
          "Updating non-group columns must be allowed even when change_group=false");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CG);
    }
  }

  @Test
  void updateGroupsAllowedWhenChangeGroupTrue() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_YES_CG);
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_TWO, USER_ALICE, ROLE_YES_CG);
    try {
      db.setActiveUser(USER_ALICE);
      assertDoesNotThrow(
          () ->
              jooq.execute(
                  "UPDATE \""
                      + SCHEMA_NAME
                      + "\".\""
                      + TABLE_NAME
                      + "\" SET mg_groups = ARRAY[?]::text[] WHERE id = 'row1'",
                  GROUP_TWO),
          "Changing mg_groups must be allowed when change_group=true");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_YES_CG);
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_TWO, USER_ALICE, ROLE_YES_CG);
      jooq.execute(
          "UPDATE \""
              + SCHEMA_NAME
              + "\".\""
              + TABLE_NAME
              + "\" SET mg_groups = ARRAY[?]::text[] WHERE id = 'row1'",
          GROUP_ONE);
    }
  }

  @Test
  void insertWithGroupsAllowedWhenChangeGroupFalse() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CG);
    try {
      db.setActiveUser(USER_ALICE);
      assertDoesNotThrow(
          () ->
              jooq.execute(
                  "INSERT INTO \""
                      + SCHEMA_NAME
                      + "\".\""
                      + TABLE_NAME
                      + "\" (id, val, mg_groups) VALUES ('row-insert-cg', 'v', ?)",
                  new Object[] {new String[] {GROUP_ONE}}),
          "Inserting with groups must be allowed even when change_group=false (group-subset check handles authorization)");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CG);
      jooq.execute(
          "DELETE FROM \"" + SCHEMA_NAME + "\".\"" + TABLE_NAME + "\" WHERE id = 'row-insert-cg'");
    }
  }
}
