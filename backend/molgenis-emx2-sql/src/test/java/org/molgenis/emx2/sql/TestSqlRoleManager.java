package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SelectScope;
import org.molgenis.emx2.UpdateScope;

class TestSqlRoleManager {

  private static final String SCHEMA_A = "SqlRoleManagerTestA";
  private static final String SCHEMA_B = "SqlRoleManagerTestB";
  private static final String SCHEMA_ENF = "TestSqlRoleManagerEnforcement";

  private static final String ENFORCEMENT_TABLE = "Items";
  private static final String GROUP_A = "groupA";
  private static final String GROUP_B = "groupB";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final DSLContext jooq = ((SqlDatabase) db).getJooq();
  private static final SqlRoleManager roleManager = new SqlRoleManager((SqlDatabase) db);

  private static final String TEST_USER_ALICE = "SqlRoleManagerTestAlice";
  private static final String TEST_USER_BOB = "SqlRoleManagerTestBob";
  private static final String USER_ALICE = "TsrmAlice";
  private static final String USER_BOB = "TsrmBob";

  private Schema schemaA;
  private Schema schemaB;
  private Schema schemaEnf;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schemaA = db.dropCreateSchema(SCHEMA_A);
    schemaB = db.dropCreateSchema(SCHEMA_B);
    schemaEnf = db.dropCreateSchema(SCHEMA_ENF);
    schemaEnf.create(table(ENFORCEMENT_TABLE).add(column("id").setPkey()).add(column("val")));
    if (!db.hasUser(TEST_USER_ALICE)) db.addUser(TEST_USER_ALICE);
    if (!db.hasUser(TEST_USER_BOB)) db.addUser(TEST_USER_BOB);
    if (!db.hasUser(USER_ALICE)) db.addUser(USER_ALICE);
    if (!db.hasUser(USER_BOB)) db.addUser(USER_BOB);
    roleManager.createGroup(schemaEnf, GROUP_A);
    roleManager.createGroup(schemaEnf, GROUP_B);
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_A);
    db.dropSchemaIfExists(SCHEMA_B);
    db.dropSchemaIfExists(SCHEMA_ENF);
  }

  // ── createRole: validation ─────────────────────────────────────────────────

  @Test
  void createRole_rejectsSystemRoleName() {
    for (String systemRole : new String[] {"Owner", "Manager", "Editor", "Viewer"}) {
      assertThrows(
          MolgenisException.class,
          () -> roleManager.createRole(SCHEMA_A, systemRole),
          "System role name '" + systemRole + "' must be rejected by createRole");
    }
  }

  @Test
  void createRole_rejectsInvalidNames() {
    for (String invalid :
        new String[] {"bad/name", "bad name", "1leading", "-leading", "_leading"}) {
      assertThrows(
          MolgenisException.class,
          () -> roleManager.createRole(SCHEMA_A, invalid),
          "Role name '" + invalid + "' must be rejected");
    }
  }

  @Test
  void createRole_acceptsValidHyphenatedName() {
    assertDoesNotThrow(() -> roleManager.createRole(SCHEMA_A, "good-name"));
  }

  @Test
  void createRole_acceptsUnderscoreInMiddle() {
    assertDoesNotThrow(() -> roleManager.createRole(SCHEMA_A, "good_name"));
  }

  @Test
  void createRole_rejectsTooLongName() {
    String schemaPrefix = "MG_ROLE_" + SCHEMA_A + "/";
    int prefixBytes = schemaPrefix.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    String tooLong = "a".repeat(SqlRoleManager.PG_MAX_ID_LENGTH - prefixBytes + 1);
    assertThrows(MolgenisException.class, () -> roleManager.createRole(SCHEMA_A, tooLong));
  }

  // ── deleteRole ─────────────────────────────────────────────────────────────

  @Test
  void deleteRole_rejectsSystemRole() {
    for (String systemRole : new String[] {"Owner", "Manager", "Editor", "Viewer"}) {
      assertThrows(
          MolgenisException.class,
          () -> roleManager.deleteRole(SCHEMA_A, systemRole),
          "System role '" + systemRole + "' must be rejected by deleteRole");
    }
  }

  // ── setPermissions / getPermissionSet round-trip ──────────────────────────

  @Test
  void setPermissions_emptyRoundTrip() {
    roleManager.createRole(SCHEMA_A, "fresh");
    PermissionSet empty = new PermissionSet();

    roleManager.setPermissions(schemaA, "fresh", empty);
    PermissionSet result = roleManager.getPermissionSet(SCHEMA_A, "fresh");

    assertTrue(result.getTables().isEmpty());
    assertFalse(result.isChangeOwner());
    assertFalse(result.isChangeGroup());
  }

  @Test
  void setPermissions_withTableScopesRoundTrip() {
    roleManager.createRole(SCHEMA_A, "scoped");
    PermissionSet permissions =
        new PermissionSet()
            .putTable(
                "myTable",
                new TablePermission("myTable")
                    .setSelect(SelectScope.ALL)
                    .setInsert(UpdateScope.OWN)
                    .setUpdate(UpdateScope.GROUP)
                    .setDelete(UpdateScope.NONE));

    roleManager.setPermissions(schemaA, "scoped", permissions);
    PermissionSet result = roleManager.getPermissionSet(SCHEMA_A, "scoped");

    TablePermission tp = result.getTables().get("myTable");
    assertNotNull(tp);
    assertEquals(SelectScope.ALL, tp.getSelect());
    assertEquals(UpdateScope.OWN, tp.getInsert());
    assertEquals(UpdateScope.GROUP, tp.getUpdate());
    assertEquals(UpdateScope.NONE, tp.getDelete());
  }

  @Test
  void setPermissions_withFlagsRoundTrip() {
    roleManager.createRole(SCHEMA_A, "flagged");
    PermissionSet permissions = new PermissionSet().setChangeOwner(true).setChangeGroup(true);
    roleManager.setPermissions(
        schemaA,
        "flagged",
        permissions.putTable("t", new TablePermission("t").setSelect(SelectScope.ALL)));

    PermissionSet result = roleManager.getPermissionSet(SCHEMA_A, "flagged");

    assertTrue(result.isChangeOwner());
    assertTrue(result.isChangeGroup());
  }

  @Test
  void setPermissions_overwritesPriorRows() {
    roleManager.createRole(SCHEMA_A, "overwrite");
    PermissionSet first =
        new PermissionSet()
            .setChangeOwner(true)
            .putTable("tableA", new TablePermission("tableA").setSelect(SelectScope.OWN));
    PermissionSet second =
        new PermissionSet()
            .putTable("tableB", new TablePermission("tableB").setSelect(SelectScope.GROUP));

    roleManager.setPermissions(schemaA, "overwrite", first);
    roleManager.setPermissions(schemaA, "overwrite", second);
    PermissionSet result = roleManager.getPermissionSet(SCHEMA_A, "overwrite");

    assertFalse(result.isChangeOwner(), "changeOwner must be reset by second setPermissions");
    assertNull(
        result.getTables().get("tableA"), "first table must be removed by second setPermissions");
    assertNotNull(result.getTables().get("tableB"), "second table must be present");
  }

  @Test
  void setPermissions_rejectsSystemRole() {
    assertThrows(
        MolgenisException.class,
        () -> roleManager.setPermissions(schemaA, "Editor", new PermissionSet()));
  }

  @Test
  void getPermissionSet_returnsEmptyForUnknownRole() {
    PermissionSet result = roleManager.getPermissionSet(SCHEMA_A, "nonexistent");
    assertNotNull(result);
    assertTrue(result.getTables().isEmpty());
  }

  // ── addGroupMembership idempotency ─────────────────────────────────────────

  @Test
  void addGroupMembership_idempotent() {
    roleManager.createGroup(schemaA, "groupY");
    roleManager.createRole(SCHEMA_A, "roleY");

    roleManager.addGroupMembership(SCHEMA_A, "groupY", TEST_USER_ALICE, "roleY");
    assertDoesNotThrow(
        () -> roleManager.addGroupMembership(SCHEMA_A, "groupY", TEST_USER_ALICE, "roleY"),
        "Second addGroupMembership with same params must be idempotent");

    roleManager.removeGroupMembership(SCHEMA_A, "groupY", TEST_USER_ALICE, "roleY");
    roleManager.deleteGroup(schemaA, "groupY");
  }

  // ── isSystemRole ──────────────────────────────────────────────────────────

  @Test
  void isSystemRole_trueForSystemRoles() {
    assertTrue(roleManager.isSystemRole("Owner"));
    assertTrue(roleManager.isSystemRole("Manager"));
    assertTrue(roleManager.isSystemRole("Editor"));
    assertTrue(roleManager.isSystemRole("Viewer"));
  }

  @Test
  void isSystemRole_falseForCustom() {
    assertFalse(roleManager.isSystemRole("analyst"));
    assertFalse(roleManager.isSystemRole(""));
  }

  // ── enforcement: system-role RLS behaviour ───────────────────────────────

  @Test
  void viewerCanReadRows() {
    roleManager.enableRlsForTable(schemaEnf, ENFORCEMENT_TABLE);
    db.becomeAdmin();
    schemaEnf
        .getTable(ENFORCEMENT_TABLE)
        .insert(new Row().setString("id", "r1").setString("val", "v1"));
    schemaEnf.addMember(USER_ALICE, "Viewer");

    db.setActiveUser(USER_ALICE);
    try {
      List<Row> rows = schemaEnf.getTable(ENFORCEMENT_TABLE).retrieveRows();
      assertFalse(rows.isEmpty(), "Viewer must be able to read rows");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void viewerCannotWriteRows() {
    roleManager.enableRlsForTable(schemaEnf, ENFORCEMENT_TABLE);
    db.becomeAdmin();
    schemaEnf
        .getTable(ENFORCEMENT_TABLE)
        .insert(new Row().setString("id", "r1").setString("val", "v1"));
    schemaEnf.addMember(USER_ALICE, "Viewer");

    db.setActiveUser(USER_ALICE);
    try {
      assertThrows(
          Exception.class,
          () ->
              schemaEnf
                  .getTable(ENFORCEMENT_TABLE)
                  .insert(new Row().setString("id", "new").setString("val", "x")),
          "Viewer must not insert");

      try {
        int updated =
            schemaEnf
                .getTable(ENFORCEMENT_TABLE)
                .update(new Row().setString("id", "r1").setString("val", "changed"));
        assertEquals(0, updated, "Viewer must not update (0 rows affected)");
      } catch (Exception ignored) {
      }

      try {
        int deleted = schemaEnf.getTable(ENFORCEMENT_TABLE).delete(new Row().setString("id", "r1"));
        assertEquals(0, deleted, "Viewer must not delete (0 rows affected)");
      } catch (Exception ignored) {
      }
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void noRoleCannotRead() {
    roleManager.enableRlsForTable(schemaEnf, ENFORCEMENT_TABLE);

    db.setActiveUser(USER_BOB);
    try {
      assertThrows(
          Exception.class,
          () -> schemaEnf.getTable(ENFORCEMENT_TABLE).retrieveRows(),
          "User with no role must not read");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void editorCanReadAndWrite() {
    roleManager.enableRlsForTable(schemaEnf, ENFORCEMENT_TABLE);
    db.becomeAdmin();
    schemaEnf.addMember(USER_ALICE, "Editor");

    db.setActiveUser(USER_ALICE);
    try {
      schemaEnf
          .getTable(ENFORCEMENT_TABLE)
          .insert(new Row().setString("id", "e1").setString("val", "v"));
      List<Row> rows = schemaEnf.getTable(ENFORCEMENT_TABLE).retrieveRows();
      assertFalse(rows.isEmpty(), "Editor must read rows");

      assertDoesNotThrow(
          () ->
              schemaEnf
                  .getTable(ENFORCEMENT_TABLE)
                  .update(new Row().setString("id", "e1").setString("val", "updated")),
          "Editor must update");

      assertDoesNotThrow(
          () -> schemaEnf.getTable(ENFORCEMENT_TABLE).delete(new Row().setString("id", "e1")),
          "Editor must delete");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void ownScopeSeesOnlyOwnRows() {
    setupEnforcementRole(
        "own-reader", SelectScope.OWN, UpdateScope.OWN, UpdateScope.NONE, UpdateScope.NONE);

    db.becomeAdmin();
    schemaEnf.addMember(USER_BOB, "Editor");
    db.setActiveUser(USER_BOB);
    schemaEnf
        .getTable(ENFORCEMENT_TABLE)
        .insert(new Row().setString("id", "bob-row").setString("val", "v"));
    db.becomeAdmin();

    roleManager.addGroupMembership(SCHEMA_ENF, GROUP_A, USER_ALICE, "own-reader");
    db.setActiveUser(USER_ALICE);
    schemaEnf
        .getTable(ENFORCEMENT_TABLE)
        .insert(new Row().setString("id", "alice-row").setString("val", "v"));

    List<Row> rows = schemaEnf.getTable(ENFORCEMENT_TABLE).retrieveRows();
    List<String> ids = rows.stream().map(r -> r.getString("id")).toList();
    db.becomeAdmin();

    assertTrue(ids.contains("alice-row"), "OWN scope must show alice's own row");
    assertFalse(ids.contains("bob-row"), "OWN scope must not show bob's row");
  }

  @Test
  void groupScopeSeesOnlyGroupRows() {
    setupEnforcementRole(
        "group-reader", SelectScope.GROUP, UpdateScope.NONE, UpdateScope.NONE, UpdateScope.NONE);

    db.becomeAdmin();
    insertGroupTaggedRow("row-a", "va", new String[] {GROUP_A});
    insertGroupTaggedRow("row-b", "vb", new String[] {GROUP_B});

    roleManager.addGroupMembership(SCHEMA_ENF, GROUP_A, USER_ALICE, "group-reader");
    db.setActiveUser(USER_ALICE);
    List<Row> rows = schemaEnf.getTable(ENFORCEMENT_TABLE).retrieveRows();
    List<String> ids = rows.stream().map(r -> r.getString("id")).toList();
    db.becomeAdmin();

    assertTrue(ids.contains("row-a"), "GROUP scope must show row tagged with user's group");
    assertFalse(
        ids.contains("row-b"), "GROUP scope must not show row tagged with a different group");
  }

  @Test
  void ownScopeUpdatesOnlyOwnRows() {
    setupEnforcementRole(
        "own-updater", SelectScope.OWN, UpdateScope.OWN, UpdateScope.OWN, UpdateScope.NONE);

    db.becomeAdmin();
    schemaEnf.addMember(USER_BOB, "Editor");
    db.setActiveUser(USER_BOB);
    schemaEnf
        .getTable(ENFORCEMENT_TABLE)
        .insert(new Row().setString("id", "bob-row").setString("val", "orig"));
    db.becomeAdmin();

    roleManager.addGroupMembership(SCHEMA_ENF, GROUP_A, USER_ALICE, "own-updater");
    db.setActiveUser(USER_ALICE);
    schemaEnf
        .getTable(ENFORCEMENT_TABLE)
        .insert(new Row().setString("id", "alice-row").setString("val", "orig"));

    assertDoesNotThrow(
        () ->
            schemaEnf
                .getTable(ENFORCEMENT_TABLE)
                .update(new Row().setString("id", "alice-row").setString("val", "changed")),
        "OWN update_scope must allow updating own row");

    int affected =
        schemaEnf
            .getTable(ENFORCEMENT_TABLE)
            .update(new Row().setString("id", "bob-row").setString("val", "hack"));
    db.becomeAdmin();
    assertEquals(0, affected, "OWN update_scope must not update another user's row");
  }

  @Test
  void groupScopeUpdatesOnlyGroupRows() {
    setupEnforcementRole(
        "group-updater", SelectScope.GROUP, UpdateScope.NONE, UpdateScope.GROUP, UpdateScope.NONE);

    db.becomeAdmin();
    insertGroupTaggedRow("row-a", "v", new String[] {GROUP_A});
    insertGroupTaggedRow("row-b", "v", new String[] {GROUP_B});

    roleManager.addGroupMembership(SCHEMA_ENF, GROUP_A, USER_ALICE, "group-updater");
    db.setActiveUser(USER_ALICE);

    assertDoesNotThrow(
        () ->
            schemaEnf
                .getTable(ENFORCEMENT_TABLE)
                .update(new Row().setString("id", "row-a").setString("val", "updated")),
        "GROUP update_scope must allow updating row in user's group");

    int affected =
        schemaEnf
            .getTable(ENFORCEMENT_TABLE)
            .update(new Row().setString("id", "row-b").setString("val", "hack"));
    db.becomeAdmin();
    assertEquals(
        0, affected, "GROUP update_scope must not update row in a group the user is not member of");
  }

  @Test
  void grant_rlsScopeOnNonRlsTable_throws() {
    roleManager.createRole(SCHEMA_ENF, "grant-rls-guard");
    TablePermission grouped = new TablePermission(ENFORCEMENT_TABLE).setSelect(SelectScope.GROUP);
    assertThrows(
        MolgenisException.class,
        () -> roleManager.grant(SCHEMA_ENF, "grant-rls-guard", grouped),
        "Granting GROUP scope on a non-RLS table must throw");
    TablePermission owned = new TablePermission(ENFORCEMENT_TABLE).setSelect(SelectScope.OWN);
    assertThrows(
        MolgenisException.class,
        () -> roleManager.grant(SCHEMA_ENF, "grant-rls-guard", owned),
        "Granting OWN scope on a non-RLS table must throw");
  }

  @Test
  void revoke_deletesRpmRow() {
    roleManager.createRole(SCHEMA_ENF, "revoke-test-role");
    ((SqlTableMetadata) schemaEnf.getTable(ENFORCEMENT_TABLE).getMetadata()).setRlsEnabled(true);
    TablePermission tp = new TablePermission(ENFORCEMENT_TABLE).setSelect(SelectScope.ALL);
    roleManager.grant(SCHEMA_ENF, "revoke-test-role", tp);

    PermissionSet before = roleManager.getPermissionSet(SCHEMA_ENF, "revoke-test-role");
    assertNotNull(before.getTables().get(ENFORCEMENT_TABLE), "RPM row must exist after grant");

    roleManager.revoke(SCHEMA_ENF, "revoke-test-role", ENFORCEMENT_TABLE);

    PermissionSet after = roleManager.getPermissionSet(SCHEMA_ENF, "revoke-test-role");
    assertNull(after.getTables().get(ENFORCEMENT_TABLE), "RPM row must be deleted after revoke");
  }

  @Test
  void deleteRoleRejectsSystemRoleNames() {
    assertThrows(
        MolgenisException.class,
        () -> roleManager.deleteRole(SCHEMA_ENF, "Owner"),
        "Owner must be rejected by deleteRole");

    assertThrows(
        MolgenisException.class,
        () -> roleManager.deleteRole(SCHEMA_ENF, "Viewer"),
        "Viewer must be rejected by deleteRole");
  }

  // ── invariant: system role cannot be bound to group ──────────────────────

  @Test
  void systemRoleWithGroup_rejected() {
    roleManager.createGroup(schemaEnf, "gInvariant");
    for (String sysRole : new String[] {"Owner", "Manager", "Editor", "Viewer"}) {
      MolgenisException ex =
          assertThrows(
              MolgenisException.class,
              () -> roleManager.addGroupMembership(SCHEMA_ENF, "gInvariant", USER_ALICE, sysRole),
              "System role '" + sysRole + "' must be rejected when bound to a group");
      assertTrue(
          ex.getMessage().contains("cannot be bound to group"),
          "Message must say 'cannot be bound to group', was: " + ex.getMessage());
    }
    roleManager.deleteGroup(schemaEnf, "gInvariant");
  }

  // ── scope: absent RPM row ⇒ access denied ────────────────────────────────

  @Test
  void absentRpmRowMeansNoRowVisible() {
    roleManager.createRole(SCHEMA_ENF, "absent-rpm-role");
    ((SqlTableMetadata) schemaEnf.getTable(ENFORCEMENT_TABLE).getMetadata()).setRlsEnabled(true);

    db.becomeAdmin();
    schemaEnf
        .getTable(ENFORCEMENT_TABLE)
        .insert(new Row().setString("id", "visible-row").setString("val", "v"));

    roleManager.grantRoleToUser(schemaEnf, "absent-rpm-role", USER_ALICE);

    db.setActiveUser(USER_ALICE);
    try {
      assertThrows(
          MolgenisException.class,
          () -> schemaEnf.getTable(ENFORCEMENT_TABLE).retrieveRows(),
          "Custom role with no RPM row must be denied access (no PG SELECT grant)");
    } finally {
      db.becomeAdmin();
    }
  }

  // ── scope: SELECT=ALL returns every row ──────────────────────────────────

  @Test
  void selectScopeAllReturnsEveryRow() {
    setupEnforcementRole(
        "all-reader", SelectScope.ALL, UpdateScope.NONE, UpdateScope.NONE, UpdateScope.NONE);

    db.becomeAdmin();
    schemaEnf.addMember(USER_BOB, "Editor");
    db.setActiveUser(USER_BOB);
    schemaEnf
        .getTable(ENFORCEMENT_TABLE)
        .insert(new Row().setString("id", "row-bob").setString("val", "v1"));
    db.becomeAdmin();

    insertGroupTaggedRow("row-grouped", "v2", new String[] {GROUP_A});

    roleManager.addGroupMembership(SCHEMA_ENF, GROUP_B, USER_ALICE, "all-reader");
    db.setActiveUser(USER_ALICE);
    try {
      List<Row> rows = schemaEnf.getTable(ENFORCEMENT_TABLE).retrieveRows();
      List<String> ids = rows.stream().map(r -> r.getString("id")).toList();
      assertTrue(ids.contains("row-bob"), "SELECT=ALL must return row owned by another user");
      assertTrue(
          ids.contains("row-grouped"), "SELECT=ALL must return row tagged with a different group");
    } finally {
      db.becomeAdmin();
    }
  }

  // ── revokeRoleFromUser: clears all rows regardless of group_name ──────────

  @Test
  void removeMember_withoutGroup_clearsAllRowsAndRevokesPgRole_evenWhenGroupBoundRowsExist() {
    String roleName = "revoke-all-role";
    roleManager.createRole(SCHEMA_ENF, roleName);
    roleManager.createGroup(schemaEnf, "g1");

    roleManager.grantRoleToUser(schemaEnf, roleName, USER_ALICE);
    roleManager.addGroupMembership(SCHEMA_ENF, "g1", USER_ALICE, roleName);

    int rowsBefore =
        jooq.fetchOne(
                "SELECT count(*) FROM \"MOLGENIS\".group_membership_metadata"
                    + " WHERE schema_name = ? AND user_name = ? AND role_name = ?",
                SCHEMA_ENF,
                USER_ALICE,
                roleName)
            .get(0, Integer.class);
    assertEquals(2, rowsBefore, "Setup: must have 2 membership rows before revoke");

    roleManager.revokeRoleFromUser(schemaEnf, roleName, USER_ALICE);

    int rowsAfter =
        jooq.fetchOne(
                "SELECT count(*) FROM \"MOLGENIS\".group_membership_metadata"
                    + " WHERE schema_name = ? AND user_name = ? AND role_name = ?",
                SCHEMA_ENF,
                USER_ALICE,
                roleName)
            .get(0, Integer.class);
    assertEquals(0, rowsAfter, "All membership rows must be deleted after no-group revoke");

    String fullRole = SqlRoleManager.fullRoleName(SCHEMA_ENF, roleName);
    String fullUser = org.molgenis.emx2.Constants.MG_USER_PREFIX + USER_ALICE;
    int pgGrantCount =
        jooq.fetchOne(
                "SELECT count(*) FROM pg_auth_members am"
                    + " JOIN pg_roles r ON r.oid = am.roleid"
                    + " JOIN pg_roles m ON m.oid = am.member"
                    + " WHERE m.rolname = ? AND r.rolname = ?",
                fullUser,
                fullRole)
            .get(0, Integer.class);
    assertEquals(0, pgGrantCount, "PG role must be revoked after no-group drop");

    roleManager.deleteGroup(schemaEnf, "g1");
  }

  // ── grantRoleToUser: schema-wide supersedes group-bound rows ─────────────

  @Test
  void addMember_withoutGroup_supersedesExistingGroupBoundRows() {
    String roleName = "supersede-role";
    roleManager.createRole(SCHEMA_ENF, roleName);
    roleManager.createGroup(schemaEnf, "sg1");

    roleManager.addGroupMembership(SCHEMA_ENF, "sg1", USER_ALICE, roleName);

    int rowsBefore =
        jooq.fetchOne(
                "SELECT count(*) FROM \"MOLGENIS\".group_membership_metadata"
                    + " WHERE schema_name = ? AND user_name = ? AND role_name = ?",
                SCHEMA_ENF,
                USER_ALICE,
                roleName)
            .get(0, Integer.class);
    assertEquals(1, rowsBefore, "Setup: one group-bound row must exist before schema-wide grant");

    roleManager.grantRoleToUser(schemaEnf, roleName, USER_ALICE);

    int nullGroupCount =
        jooq.fetchOne(
                "SELECT count(*) FROM \"MOLGENIS\".group_membership_metadata"
                    + " WHERE schema_name = ? AND user_name = ? AND role_name = ? AND group_name IS NULL",
                SCHEMA_ENF,
                USER_ALICE,
                roleName)
            .get(0, Integer.class);
    assertEquals(
        1, nullGroupCount, "Exactly one NULL-group row must exist after schema-wide grant");

    int sg1Count =
        jooq.fetchOne(
                "SELECT count(*) FROM \"MOLGENIS\".group_membership_metadata"
                    + " WHERE schema_name = ? AND user_name = ? AND role_name = ? AND group_name = ?",
                SCHEMA_ENF,
                USER_ALICE,
                roleName,
                "sg1")
            .get(0, Integer.class);
    assertEquals(0, sg1Count, "Group-bound row must be superseded by schema-wide grant");

    String fullRole = SqlRoleManager.fullRoleName(SCHEMA_ENF, roleName);
    String fullUser = org.molgenis.emx2.Constants.MG_USER_PREFIX + USER_ALICE;
    int pgGrantCount =
        jooq.fetchOne(
                "SELECT count(*) FROM pg_auth_members am"
                    + " JOIN pg_roles r ON r.oid = am.roleid"
                    + " JOIN pg_roles m ON m.oid = am.member"
                    + " WHERE m.rolname = ? AND r.rolname = ?",
                fullUser,
                fullRole)
            .get(0, Integer.class);
    assertEquals(1, pgGrantCount, "PG role must still be granted after schema-wide supersede");

    roleManager.deleteGroup(schemaEnf, "sg1");
  }

  // ── enforcement helpers ───────────────────────────────────────────────────

  private void insertGroupTaggedRow(String id, String val, String[] groups) {
    jooq.execute(
        "INSERT INTO \""
            + SCHEMA_ENF
            + "\".\""
            + ENFORCEMENT_TABLE
            + "\" (id, val, mg_groups) VALUES (?, ?, ?)",
        id,
        val,
        groups);
  }

  private void setupEnforcementRole(
      String roleName,
      SelectScope selectScope,
      UpdateScope insertScope,
      UpdateScope updateScope,
      UpdateScope deleteScope) {
    roleManager.createRole(SCHEMA_ENF, roleName);
    ((SqlTableMetadata) schemaEnf.getTable(ENFORCEMENT_TABLE).getMetadata()).setRlsEnabled(true);
    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(ENFORCEMENT_TABLE);
    tp.setSelect(selectScope);
    tp.setInsert(insertScope);
    tp.setUpdate(updateScope);
    tp.setDelete(deleteScope);
    ps.putTable(ENFORCEMENT_TABLE, tp);
    roleManager.setPermissions(schemaEnf, roleName, ps);
  }
}
