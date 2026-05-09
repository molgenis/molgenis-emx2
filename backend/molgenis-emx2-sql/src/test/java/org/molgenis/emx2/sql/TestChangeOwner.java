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
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;

class TestChangeOwner {

  private static final String SCHEMA_NAME = "TestChangeOwner";
  private static final String TABLE_NAME = "Items";
  private static final String GROUP_ONE = "groupOne";
  private static final String ROLE_NO_CO = "roleNoChangeOwner";
  private static final String ROLE_YES_CO = "roleYesChangeOwner";
  private static final String USER_ALICE = "TcoAlice";
  private static final String OTHER_OWNER = "TcoOtherUser";

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
    if (!db.hasUser(USER_ALICE)) db.addUser(USER_ALICE);
    if (!db.hasUser(OTHER_OWNER)) db.addUser(OTHER_OWNER);

    roleManager.createRole(SCHEMA_NAME, ROLE_NO_CO);
    roleManager.createRole(SCHEMA_NAME, ROLE_YES_CO);
    schema.getTable(TABLE_NAME).getMetadata().setRlsEnabled(true);

    PermissionSet noCoPerms = new PermissionSet();
    noCoPerms.setChangeOwner(false);
    TablePermission tp = new TablePermission(TABLE_NAME);
    tp.setSelect(SelectScope.ALL);
    tp.setInsert(UpdateScope.ALL);
    tp.setUpdate(UpdateScope.ALL);
    tp.setDelete(UpdateScope.ALL);
    noCoPerms.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, ROLE_NO_CO, noCoPerms);

    PermissionSet yesCoPerms = new PermissionSet();
    yesCoPerms.setChangeOwner(true);
    TablePermission tpYes = new TablePermission(TABLE_NAME);
    tpYes.setSelect(SelectScope.ALL);
    tpYes.setInsert(UpdateScope.ALL);
    tpYes.setUpdate(UpdateScope.ALL);
    tpYes.setDelete(UpdateScope.ALL);
    yesCoPerms.putTable(TABLE_NAME, tpYes);
    roleManager.setPermissions(schema, ROLE_YES_CO, yesCoPerms);

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
  void updateBlockedWhenChangeOwnerFalse() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CO);
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
                      + "\" SET mg_owner = ? WHERE id = 'row1'",
                  OTHER_OWNER),
          "Changing mg_owner must be blocked when change_owner=false");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CO);
    }
  }

  @Test
  void updateOtherColumnAllowedWhenChangeOwnerFalse() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CO);
    try {
      db.setActiveUser(USER_ALICE);
      assertDoesNotThrow(
          () ->
              schema
                  .getTable(TABLE_NAME)
                  .update(new Row().setString("id", "row1").setString("val", "updated")),
          "Updating non-owner columns must be allowed even when change_owner=false");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CO);
    }
  }

  @Test
  void updateOwnerAllowedWhenChangeOwnerTrue() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_YES_CO);
    try {
      db.setActiveUser(USER_ALICE);
      assertDoesNotThrow(
          () ->
              jooq.execute(
                  "UPDATE \""
                      + SCHEMA_NAME
                      + "\".\""
                      + TABLE_NAME
                      + "\" SET mg_owner = ? WHERE id = 'row1'",
                  OTHER_OWNER),
          "Changing mg_owner must be allowed when change_owner=true");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_YES_CO);
      jooq.execute(
          "UPDATE \""
              + SCHEMA_NAME
              + "\".\""
              + TABLE_NAME
              + "\" SET mg_owner = 'admin' WHERE id = 'row1'");
    }
  }

  @Test
  void insertWithForeignOwnerBlockedWhenChangeOwnerFalse() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CO);
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
                      + "\" (id, val, mg_owner, mg_groups) VALUES ('row-foreign-owner', 'v', ?, ?)",
                  OTHER_OWNER,
                  new String[] {GROUP_ONE}),
          "Inserting with mg_owner != current_user must be blocked when change_owner=false");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_NO_CO);
    }
  }

  @Test
  void insertWithForeignOwnerAllowedWhenChangeOwnerTrue() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_YES_CO);
    try {
      db.setActiveUser(USER_ALICE);
      assertDoesNotThrow(
          () ->
              jooq.execute(
                  "INSERT INTO \""
                      + SCHEMA_NAME
                      + "\".\""
                      + TABLE_NAME
                      + "\" (id, val, mg_owner, mg_groups) VALUES ('row-foreign-owner2', 'v', ?, ?)",
                  OTHER_OWNER,
                  new String[] {GROUP_ONE}),
          "Inserting with mg_owner != current_user must be allowed when change_owner=true");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_YES_CO);
      schema.getTable(TABLE_NAME).delete(new Row().setString("id", "row-foreign-owner2"));
    }
  }
}
