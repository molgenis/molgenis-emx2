package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;

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

  @BeforeAll
  static void applyMigration() {
    Migrations.executeMigrationFile(
        db, "migration32.sql", "re-apply migration32 for change_group INSERT fix");
  }

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
    schema.getTable(TABLE_NAME).getMetadata().setRlsEnabled(true);

    PermissionSet noCgPerms = new PermissionSet();
    noCgPerms.setChangeGroup(false);
    TablePermission tp = new TablePermission(TABLE_NAME);
    tp.select(SelectScope.ALL);
    tp.insert(UpdateScope.ALL);
    tp.update(UpdateScope.ALL);
    tp.delete(UpdateScope.ALL);
    noCgPerms.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, ROLE_NO_CG, noCgPerms);

    PermissionSet yesCgPerms = new PermissionSet();
    yesCgPerms.setChangeGroup(true);
    TablePermission tpYes = new TablePermission(TABLE_NAME);
    tpYes.select(SelectScope.ALL);
    tpYes.insert(UpdateScope.ALL);
    tpYes.update(UpdateScope.ALL);
    tpYes.delete(UpdateScope.ALL);
    yesCgPerms.putTable(TABLE_NAME, tpYes);
    roleManager.setPermissions(schema, ROLE_YES_CG, yesCgPerms);

    insertRowAsAdmin("row1", "val1", "admin", new String[] {GROUP_ONE});
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  private void insertRowAsAdmin(String id, String val, String owner, String[] groups) {
    db.becomeAdmin();
    schema
        .getTable(TABLE_NAME)
        .insert(
            new Row()
                .setString("id", id)
                .setString("val", val)
                .setString("mg_owner", owner)
                .setStringArray("mg_groups", groups));
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
              schema
                  .getTable(TABLE_NAME)
                  .update(new Row().setString("id", "row1").setString("val", "updated")),
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
          "Inserting with own group must be allowed when change_group=false");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CG);
      schema.getTable(TABLE_NAME).delete(new Row().setString("id", "row-insert-cg"));
    }
  }

  @Test
  void insertWithForeignGroupBlockedWhenChangeGroupFalse() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CG);
    try {
      db.setActiveUser(USER_ALICE);
      assertThrows(
          DataAccessException.class,
          () ->
              jooq.execute(
                  "INSERT INTO \""
                      + SCHEMA_NAME
                      + "\".\""
                      + TABLE_NAME
                      + "\" (id, val, mg_groups) VALUES ('row-foreign-group', 'v', ?)",
                  new Object[] {new String[] {GROUP_TWO}}),
          "Inserting with a group the user is not a member of must be blocked when change_group=false");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CG);
      jooq.execute(
          "DELETE FROM \""
              + SCHEMA_NAME
              + "\".\""
              + TABLE_NAME
              + "\" WHERE id = 'row-foreign-group'");
    }
  }

  @Test
  void insertWithForeignGroupAllowedWhenChangeGroupTrue() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_YES_CG);
    try {
      db.setActiveUser(USER_ALICE);
      assertDoesNotThrow(
          () ->
              jooq.execute(
                  "INSERT INTO \""
                      + SCHEMA_NAME
                      + "\".\""
                      + TABLE_NAME
                      + "\" (id, val, mg_groups) VALUES ('row-delegate-group', 'v', ?)",
                  new Object[] {new String[] {GROUP_TWO}}),
          "Inserting with a foreign group must be allowed when change_group=true");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_YES_CG);
      schema.getTable(TABLE_NAME).delete(new Row().setString("id", "row-delegate-group"));
    }
  }

  @Test
  void insertWithGroupScopeEmptyGroupsBlocked() {
    String roleGroupScope = "roleGroupScopeInsert";
    roleManager.createRole(SCHEMA_NAME, roleGroupScope);
    PermissionSet groupScopePerms = new PermissionSet();
    groupScopePerms.setChangeGroup(false);
    TablePermission tpGroup = new TablePermission(TABLE_NAME);
    tpGroup.select(SelectScope.ALL);
    tpGroup.insert(UpdateScope.GROUP);
    groupScopePerms.putTable(TABLE_NAME, tpGroup);
    roleManager.setPermissions(schema, roleGroupScope, groupScopePerms);
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, roleGroupScope);
    try {
      db.setActiveUser(USER_ALICE);
      assertThrows(
          DataAccessException.class,
          () ->
              jooq.execute(
                  "INSERT INTO \""
                      + SCHEMA_NAME
                      + "\".\""
                      + TABLE_NAME
                      + "\" (id, val, mg_groups) VALUES ('row-empty-group', 'v', '{}'::text[])"),
          "INSERT with empty mg_groups must be blocked for GROUP-scoped user (#16 stealth-publish prevention)");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, roleGroupScope);
      jooq.execute(
          "DELETE FROM \""
              + SCHEMA_NAME
              + "\".\""
              + TABLE_NAME
              + "\" WHERE id = 'row-empty-group'");
    }
  }

  @Test
  void insertWithAllScopeEmptyGroupsAllowed() {
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
                      + "\" (id, val, mg_groups) VALUES ('row-all-scope-empty', 'v', '{}'::text[])"),
          "INSERT with empty mg_groups must be allowed for ALL-scoped user");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CG);
      schema.getTable(TABLE_NAME).delete(new Row().setString("id", "row-all-scope-empty"));
    }
  }
}
