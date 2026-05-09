package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TablePermission;

public class TestAccessFunctions {

  private static final String SCHEMA_NAME = TestAccessFunctions.class.getSimpleName();
  private static final String USER_ALICE = "TestAccessFunctionsAlice";
  private static final String GROUP_A = "groupA";
  private static final String GROUP_B = "groupB";

  private static Database db;
  private static DSLContext jooq;
  private static SqlRoleManager roleManager;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    jooq = ((SqlDatabase) db).getJooq();
    roleManager = ((SqlDatabase) db).getRoleManager();

    db.dropSchemaIfExists(SCHEMA_NAME);
    Schema schema = db.createSchema(SCHEMA_NAME);

    if (!db.hasUser(USER_ALICE)) db.addUser(USER_ALICE);

    roleManager.createGroup(schema, GROUP_A);
    roleManager.createGroup(schema, GROUP_B);
  }

  private boolean canReadAsUser(String userName, String tableName, String[] groups, String owner) {
    db.setActiveUser(userName);
    try {
      return Boolean.TRUE.equals(
          jooq.fetchOne(
                  "SELECT \"MOLGENIS\".mg_can_read(?, ?, ?, ?) AS result",
                  SCHEMA_NAME,
                  tableName,
                  groups,
                  owner)
              .get("result", Boolean.class));
    } finally {
      db.becomeAdmin();
    }
  }

  private boolean canWriteAsUser(
      String userName, String tableName, String[] groups, String owner, String verb) {
    db.setActiveUser(userName);
    try {
      return Boolean.TRUE.equals(
          jooq.fetchOne(
                  "SELECT \"MOLGENIS\".mg_can_write(?, ?, ?, ?, ?) AS result",
                  SCHEMA_NAME,
                  tableName,
                  groups,
                  owner,
                  verb)
              .get("result", Boolean.class));
    } finally {
      db.becomeAdmin();
    }
  }

  private boolean canWriteAllAsUser(
      String userName,
      String tableName,
      String[] groups,
      String owner,
      String verb,
      boolean changingOwner,
      boolean changingGroup) {
    db.setActiveUser(userName);
    try {
      return Boolean.TRUE.equals(
          jooq.fetchOne(
                  "SELECT \"MOLGENIS\".mg_can_write_all(?, ?, ?, ?, ?, ?, ?) AS result",
                  SCHEMA_NAME,
                  tableName,
                  groups,
                  owner,
                  verb,
                  changingOwner,
                  changingGroup)
              .get("result", Boolean.class));
    } finally {
      db.becomeAdmin();
    }
  }

  private Schema schema() {
    return db.getSchema(SCHEMA_NAME);
  }

  // ── Custom-role branch: group_membership + role_permission ────────────────

  @Test
  public void mgCanReadReturnsTrueForGroupScopeWhenGroupMatches() {
    roleManager.createRole(schema(), "group-reader", "");
    roleManager.setPermissions(
        schema(),
        "group-reader",
        new PermissionSet()
            .putTable("myTable", new TablePermission("myTable").setSelect(SelectScope.GROUP)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "group-reader");

    boolean resultMatch = canReadAsUser(USER_ALICE, "myTable", new String[] {GROUP_A}, "someOwner");
    assertTrue(resultMatch, "GROUP scope with matching group must allow read");

    boolean resultNoMatch =
        canReadAsUser(USER_ALICE, "myTable", new String[] {GROUP_B}, "someOwner");
    assertFalse(resultNoMatch, "GROUP scope with non-matching group must deny read");
  }

  @Test
  public void mgCanReadReturnsTrueForOwnScope() {
    roleManager.createRole(schema(), "own-reader", "");
    roleManager.setPermissions(
        schema(),
        "own-reader",
        new PermissionSet()
            .putTable("ownTable", new TablePermission("ownTable").setSelect(SelectScope.OWN)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "own-reader");

    boolean resultOwned = canReadAsUser(USER_ALICE, "ownTable", new String[] {}, USER_ALICE);
    assertTrue(resultOwned, "OWN scope with owner=current_user must allow read");

    boolean resultOtherOwner =
        canReadAsUser(USER_ALICE, "ownTable", new String[] {}, "someone-else");
    assertFalse(resultOtherOwner, "OWN scope with owner != current_user must deny read");
  }

  @Test
  public void mgCanWriteAllRejectsShareIntoForeignGroup() {
    roleManager.createRole(schema(), "writer", "");
    roleManager.setPermissions(
        schema(),
        "writer",
        new PermissionSet()
            .putTable("someTable", new TablePermission("someTable").setInsert(UpdateScope.GROUP)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "writer");

    boolean result =
        canWriteAllAsUser(
            USER_ALICE,
            "someTable",
            new String[] {GROUP_A, GROUP_B},
            "owner",
            "insert",
            false,
            false);
    assertFalse(result, "sharing into group B when user is not member of B must be rejected");
  }

  @Test
  public void mgCanWriteReturnsTrueForGroupUpdateScopeWhenGroupMatches() {
    roleManager.createRole(schema(), "group-updater", "");
    roleManager.setPermissions(
        schema(),
        "group-updater",
        new PermissionSet()
            .putTable("upTable", new TablePermission("upTable").setUpdate(UpdateScope.GROUP)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "group-updater");

    boolean result =
        canWriteAsUser(USER_ALICE, "upTable", new String[] {GROUP_A}, "someOwner", "update");
    assertTrue(result, "GROUP update_scope with matching group must allow update");
  }

  @Test
  public void mgCanWriteReturnsFalseForGroupUpdateScopeWhenNoGroupOverlap() {
    roleManager.createRole(schema(), "group-updater2", "");
    roleManager.setPermissions(
        schema(),
        "group-updater2",
        new PermissionSet()
            .putTable("upTable2", new TablePermission("upTable2").setUpdate(UpdateScope.GROUP)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "group-updater2");

    boolean result =
        canWriteAsUser(USER_ALICE, "upTable2", new String[] {GROUP_B}, "someOwner", "update");
    assertFalse(result, "GROUP update_scope with no group overlap must deny update");
  }

  @Test
  public void mgCanWriteReturnsTrueForOwnDeleteScopeWhenOwnerMatches() {
    roleManager.createRole(schema(), "own-deleter", "");
    roleManager.setPermissions(
        schema(),
        "own-deleter",
        new PermissionSet()
            .putTable("delTable", new TablePermission("delTable").setDelete(UpdateScope.OWN)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "own-deleter");

    boolean result = canWriteAsUser(USER_ALICE, "delTable", new String[] {}, USER_ALICE, "delete");
    assertTrue(result, "OWN delete_scope with owner=current_user must allow delete");
  }

  @Test
  public void mgCanWriteReturnsFalseForOwnDeleteScopeWhenOwnerDiffers() {
    roleManager.createRole(schema(), "own-deleter2", "");
    roleManager.setPermissions(
        schema(),
        "own-deleter2",
        new PermissionSet()
            .putTable("delTable2", new TablePermission("delTable2").setDelete(UpdateScope.OWN)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "own-deleter2");

    boolean result =
        canWriteAsUser(USER_ALICE, "delTable2", new String[] {}, "someone-else", "delete");
    assertFalse(result, "OWN delete_scope with owner != current_user must deny delete");
  }

  @Test
  public void mgCanWriteReturnsFalseForNoneInsertScope() {
    roleManager.createRole(schema(), "no-inserter", "");
    roleManager.setPermissions(
        schema(),
        "no-inserter",
        new PermissionSet()
            .putTable(
                "noInsertTable", new TablePermission("noInsertTable").setInsert(UpdateScope.NONE)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "no-inserter");

    boolean result =
        canWriteAsUser(USER_ALICE, "noInsertTable", new String[] {GROUP_A}, null, "insert");
    assertFalse(result, "NONE insert_scope must deny insert");
  }

  @Test
  public void mgPrivacyCountReturnsRoundedValue() {
    Record result = jooq.fetchOne("SELECT \"MOLGENIS\".mg_privacy_count(23) AS cnt");
    assertNotNull(result, "mg_privacy_count must return a result");
    Long count = result.get("cnt", Long.class);
    assertNotNull(count, "count must not be null");
    assertEquals(0, count % 10, "mg_privacy_count must return a multiple of 10");
    assertEquals(30L, count, "mg_privacy_count(23) must ceil to 30");
  }

  @Test
  public void customRoleCountScopePassesMgCanRead() {
    roleManager.createRole(schema(), "count-reader", "");
    roleManager.setPermissions(
        schema(),
        "count-reader",
        new PermissionSet()
            .putTable("cntTable", new TablePermission("cntTable").setSelect(SelectScope.COUNT)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "count-reader");

    boolean resultAnyRow =
        canReadAsUser(USER_ALICE, "cntTable", new String[] {GROUP_B}, "some-other-owner");
    assertTrue(
        resultAnyRow,
        "COUNT scope must pass mg_can_read regardless of group/owner match (Path A REQ-4)");
  }
}
