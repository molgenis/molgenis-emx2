package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;

public class TestAccessFunctions {

  private static final String SCHEMA_NAME = TestAccessFunctions.class.getSimpleName();
  private static final String USER_ALICE = "TestAccessFunctionsAlice";
  private static final String USER_BOB = "TestAccessFunctionsBob";
  private static final String GROUP_A = "groupA";
  private static final String GROUP_B = "groupB";

  private static Database db;
  private static DSLContext jooq;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    jooq = ((SqlDatabase) db).getJooq();

    db.dropSchemaIfExists(SCHEMA_NAME);
    db.createSchema(SCHEMA_NAME);

    if (!db.hasUser(USER_ALICE)) db.addUser(USER_ALICE);
    if (!db.hasUser(USER_BOB)) db.addUser(USER_BOB);

    jooq.execute(
        "INSERT INTO \"MOLGENIS\".groups_metadata (schema, name) VALUES (?, ?) ON CONFLICT DO NOTHING",
        SCHEMA_NAME,
        GROUP_A);
    jooq.execute(
        "INSERT INTO \"MOLGENIS\".groups_metadata (schema, name) VALUES (?, ?) ON CONFLICT DO NOTHING",
        SCHEMA_NAME,
        GROUP_B);
  }

  @AfterAll
  public static void tearDown() {
    db.becomeAdmin();
    jooq.execute(
        "DELETE FROM \"MOLGENIS\".group_membership_metadata WHERE schema_name = ?", SCHEMA_NAME);
    jooq.execute(
        "DELETE FROM \"MOLGENIS\".role_permission_metadata"
            + " WHERE schema_name = ? AND role_name NOT IN ('Owner','Manager','Editor','Viewer')",
        SCHEMA_NAME);
    jooq.execute("DELETE FROM \"MOLGENIS\".groups_metadata WHERE schema = ?", SCHEMA_NAME);
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  private void addCustomMembership(String userName, String groupName, String roleName) {
    jooq.execute(
        "INSERT INTO \"MOLGENIS\".group_membership_metadata"
            + " (user_name, schema_name, group_name, role_name)"
            + " VALUES (?, ?, ?, ?)"
            + " ON CONFLICT DO NOTHING",
        userName,
        SCHEMA_NAME,
        groupName,
        roleName);
  }

  private void removeCustomMembership(String userName, String groupName, String roleName) {
    jooq.execute(
        "DELETE FROM \"MOLGENIS\".group_membership_metadata"
            + " WHERE user_name = ? AND schema_name = ? AND group_name = ? AND role_name = ?",
        userName,
        SCHEMA_NAME,
        groupName,
        roleName);
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

  // ── System-role branch: pg_has_role against MG_ROLE_<schema>/RoleName ─────

  @Test
  public void systemRoleViaPgHasRole_viewerCanRead() {
    db.becomeAdmin();
    db.getSchema(SCHEMA_NAME).addMember(USER_ALICE, "Viewer");
    try {
      boolean result = canReadAsUser(USER_ALICE, "anyTable", new String[] {}, null);
      assertTrue(
          result, "User with Viewer system PG role must be able to read via system-role branch");
    } finally {
      db.becomeAdmin();
      db.getSchema(SCHEMA_NAME).removeMember(USER_ALICE);
    }
  }

  @Test
  public void systemRoleViaPgHasRole_noRoleReturnsFalse() {
    boolean result = canReadAsUser(USER_BOB, "anyTable", new String[] {}, null);
    assertFalse(
        result, "User with no system role and no group membership must not be able to read");
  }

  @Test
  public void systemRoleViaPgHasRole_managerCanInsert() {
    db.becomeAdmin();
    db.getSchema(SCHEMA_NAME).addMember(USER_ALICE, "Manager");
    try {
      boolean result = canWriteAsUser(USER_ALICE, "someTable", new String[] {}, null, "insert");
      assertTrue(result, "Manager with ALL insert_scope must be able to insert");
    } finally {
      db.becomeAdmin();
      db.getSchema(SCHEMA_NAME).removeMember(USER_ALICE);
    }
  }

  @Test
  public void systemRoleViaPgHasRole_ownerCanChangeOwner() {
    db.becomeAdmin();
    db.getSchema(SCHEMA_NAME).addMember(USER_ALICE, "Owner");
    try {
      boolean result =
          canWriteAllAsUser(
              USER_ALICE, "someTable", new String[] {}, "owner", "update", true, false);
      assertTrue(result, "Owner.change_owner=true must allow changing mg_owner");
    } finally {
      db.becomeAdmin();
      db.getSchema(SCHEMA_NAME).removeMember(USER_ALICE);
    }
  }

  @Test
  public void systemRoleViaPgHasRole_editorCannotChangeOwner() {
    db.becomeAdmin();
    db.getSchema(SCHEMA_NAME).addMember(USER_ALICE, "Editor");
    try {
      boolean result =
          canWriteAllAsUser(
              USER_ALICE, "someTable", new String[] {}, "owner", "update", true, false);
      assertFalse(result, "Editor.change_owner=false must deny changing mg_owner");
    } finally {
      db.becomeAdmin();
      db.getSchema(SCHEMA_NAME).removeMember(USER_ALICE);
    }
  }

  // ── Custom-role branch: group_membership + role_permission ────────────────

  @Test
  public void mgCanReadReturnsFalseForNoMembership() {
    boolean result = canReadAsUser(USER_BOB, "someTable", new String[] {}, null);
    assertFalse(result, "user with no membership must not be able to read");
  }

  @Test
  public void mgCanReadReturnsTrueForGroupScopeWhenGroupMatches() {
    jooq.execute(
        "INSERT INTO \"MOLGENIS\".role_permission_metadata"
            + " (schema_name, role_name, table_name, select_scope)"
            + " VALUES (?, 'group-reader', 'myTable', 'GROUP')"
            + " ON CONFLICT DO NOTHING",
        SCHEMA_NAME);
    addCustomMembership(USER_ALICE, GROUP_A, "group-reader");
    try {
      boolean resultMatch =
          canReadAsUser(USER_ALICE, "myTable", new String[] {GROUP_A}, "someOwner");
      assertTrue(resultMatch, "GROUP scope with matching group must allow read");

      boolean resultNoMatch =
          canReadAsUser(USER_ALICE, "myTable", new String[] {GROUP_B}, "someOwner");
      assertFalse(resultNoMatch, "GROUP scope with non-matching group must deny read");
    } finally {
      removeCustomMembership(USER_ALICE, GROUP_A, "group-reader");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".role_permission_metadata"
              + " WHERE schema_name = ? AND role_name = 'group-reader'",
          SCHEMA_NAME);
    }
  }

  @Test
  public void mgCanReadReturnsTrueForOwnScope() {
    jooq.execute(
        "INSERT INTO \"MOLGENIS\".role_permission_metadata"
            + " (schema_name, role_name, table_name, select_scope)"
            + " VALUES (?, 'own-reader', 'ownTable', 'OWN')"
            + " ON CONFLICT DO NOTHING",
        SCHEMA_NAME);
    addCustomMembership(USER_ALICE, GROUP_A, "own-reader");
    String pgUserAlice = "MG_USER_" + USER_ALICE;
    try {
      boolean resultOwned = canReadAsUser(USER_ALICE, "ownTable", new String[] {}, pgUserAlice);
      assertTrue(resultOwned, "OWN scope with owner=current_user must allow read");

      boolean resultOtherOwner =
          canReadAsUser(USER_ALICE, "ownTable", new String[] {}, "someone-else");
      assertFalse(resultOtherOwner, "OWN scope with owner != current_user must deny read");
    } finally {
      removeCustomMembership(USER_ALICE, GROUP_A, "own-reader");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".role_permission_metadata"
              + " WHERE schema_name = ? AND role_name = 'own-reader'",
          SCHEMA_NAME);
    }
  }

  @Test
  public void mgCanWriteAllRejectsShareIntoForeignGroup() {
    jooq.execute(
        "INSERT INTO \"MOLGENIS\".role_permission_metadata"
            + " (schema_name, role_name, table_name, insert_scope)"
            + " VALUES (?, 'writer', 'someTable', 'GROUP')"
            + " ON CONFLICT DO NOTHING",
        SCHEMA_NAME);
    addCustomMembership(USER_ALICE, GROUP_A, "writer");
    try {
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
    } finally {
      removeCustomMembership(USER_ALICE, GROUP_A, "writer");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".role_permission_metadata"
              + " WHERE schema_name = ? AND role_name = 'writer'",
          SCHEMA_NAME);
    }
  }

  @Test
  public void mgCanWriteReturnsTrueForGroupUpdateScopeWhenGroupMatches() {
    jooq.execute(
        "INSERT INTO \"MOLGENIS\".role_permission_metadata"
            + " (schema_name, role_name, table_name, update_scope)"
            + " VALUES (?, 'group-updater', 'upTable', 'GROUP')"
            + " ON CONFLICT DO NOTHING",
        SCHEMA_NAME);
    addCustomMembership(USER_ALICE, GROUP_A, "group-updater");
    try {
      boolean result =
          canWriteAsUser(USER_ALICE, "upTable", new String[] {GROUP_A}, "someOwner", "update");
      assertTrue(result, "GROUP update_scope with matching group must allow update");
    } finally {
      removeCustomMembership(USER_ALICE, GROUP_A, "group-updater");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".role_permission_metadata"
              + " WHERE schema_name = ? AND role_name = 'group-updater'",
          SCHEMA_NAME);
    }
  }

  @Test
  public void mgCanWriteReturnsFalseForGroupUpdateScopeWhenNoGroupOverlap() {
    jooq.execute(
        "INSERT INTO \"MOLGENIS\".role_permission_metadata"
            + " (schema_name, role_name, table_name, update_scope)"
            + " VALUES (?, 'group-updater2', 'upTable2', 'GROUP')"
            + " ON CONFLICT DO NOTHING",
        SCHEMA_NAME);
    addCustomMembership(USER_ALICE, GROUP_A, "group-updater2");
    try {
      boolean result =
          canWriteAsUser(USER_ALICE, "upTable2", new String[] {GROUP_B}, "someOwner", "update");
      assertFalse(result, "GROUP update_scope with no group overlap must deny update");
    } finally {
      removeCustomMembership(USER_ALICE, GROUP_A, "group-updater2");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".role_permission_metadata"
              + " WHERE schema_name = ? AND role_name = 'group-updater2'",
          SCHEMA_NAME);
    }
  }

  @Test
  public void mgCanWriteReturnsTrueForOwnDeleteScopeWhenOwnerMatches() {
    jooq.execute(
        "INSERT INTO \"MOLGENIS\".role_permission_metadata"
            + " (schema_name, role_name, table_name, delete_scope)"
            + " VALUES (?, 'own-deleter', 'delTable', 'OWN')"
            + " ON CONFLICT DO NOTHING",
        SCHEMA_NAME);
    addCustomMembership(USER_ALICE, GROUP_A, "own-deleter");
    String pgUserAlice = "MG_USER_" + USER_ALICE;
    try {
      boolean result =
          canWriteAsUser(USER_ALICE, "delTable", new String[] {}, pgUserAlice, "delete");
      assertTrue(result, "OWN delete_scope with owner=current_user must allow delete");
    } finally {
      removeCustomMembership(USER_ALICE, GROUP_A, "own-deleter");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".role_permission_metadata"
              + " WHERE schema_name = ? AND role_name = 'own-deleter'",
          SCHEMA_NAME);
    }
  }

  @Test
  public void mgCanWriteReturnsFalseForOwnDeleteScopeWhenOwnerDiffers() {
    jooq.execute(
        "INSERT INTO \"MOLGENIS\".role_permission_metadata"
            + " (schema_name, role_name, table_name, delete_scope)"
            + " VALUES (?, 'own-deleter2', 'delTable2', 'OWN')"
            + " ON CONFLICT DO NOTHING",
        SCHEMA_NAME);
    addCustomMembership(USER_ALICE, GROUP_A, "own-deleter2");
    try {
      boolean result =
          canWriteAsUser(USER_ALICE, "delTable2", new String[] {}, "someone-else", "delete");
      assertFalse(result, "OWN delete_scope with owner != current_user must deny delete");
    } finally {
      removeCustomMembership(USER_ALICE, GROUP_A, "own-deleter2");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".role_permission_metadata"
              + " WHERE schema_name = ? AND role_name = 'own-deleter2'",
          SCHEMA_NAME);
    }
  }

  @Test
  public void mgCanWriteReturnsFalseForNoneInsertScope() {
    jooq.execute(
        "INSERT INTO \"MOLGENIS\".role_permission_metadata"
            + " (schema_name, role_name, table_name, insert_scope)"
            + " VALUES (?, 'no-inserter', 'noInsertTable', 'NONE')"
            + " ON CONFLICT DO NOTHING",
        SCHEMA_NAME);
    addCustomMembership(USER_ALICE, GROUP_A, "no-inserter");
    try {
      boolean result =
          canWriteAsUser(USER_ALICE, "noInsertTable", new String[] {GROUP_A}, null, "insert");
      assertFalse(result, "NONE insert_scope must deny insert");
    } finally {
      removeCustomMembership(USER_ALICE, GROUP_A, "no-inserter");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".role_permission_metadata"
              + " WHERE schema_name = ? AND role_name = 'no-inserter'",
          SCHEMA_NAME);
    }
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
    jooq.execute(
        "INSERT INTO \"MOLGENIS\".role_permission_metadata"
            + " (schema_name, role_name, table_name, select_scope)"
            + " VALUES (?, 'count-reader', 'cntTable', 'COUNT')"
            + " ON CONFLICT DO NOTHING",
        SCHEMA_NAME);
    addCustomMembership(USER_ALICE, GROUP_A, "count-reader");
    try {
      boolean resultAnyRow =
          canReadAsUser(USER_ALICE, "cntTable", new String[] {GROUP_B}, "some-other-owner");
      assertTrue(
          resultAnyRow,
          "COUNT scope must pass mg_can_read regardless of group/owner match (Path A REQ-4)");
    } finally {
      removeCustomMembership(USER_ALICE, GROUP_A, "count-reader");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".role_permission_metadata"
              + " WHERE schema_name = ? AND role_name = 'count-reader'",
          SCHEMA_NAME);
    }
  }
}
