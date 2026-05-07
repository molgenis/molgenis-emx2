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
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;
import org.molgenis.emx2.Schema;

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

  // ── createRole: validation only, no PG role ────────────────────────────────

  @Test
  void createRole_doesNotCreatePgRole() {
    roleManager.createRole(SCHEMA_A, "myRole");

    boolean pgRoleExists =
        jooq.fetchExists(
            jooq.select()
                .from("pg_roles")
                .where(field("rolname").eq(inline("MG_ROLE_" + SCHEMA_A + "/myRole"))));
    assertFalse(pgRoleExists, "createRole must not create a PG role for custom roles");
  }

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
  void createRole_rejectsEmptyName() {
    assertThrows(MolgenisException.class, () -> roleManager.createRole(SCHEMA_A, ""));
  }

  @Test
  void createRole_rejectsMgPrefix() {
    assertThrows(MolgenisException.class, () -> roleManager.createRole(SCHEMA_A, "MG_custom"));
  }

  @Test
  void createRole_rejectsTooLongName() {
    String schemaPrefix = "MG_ROLE_" + SCHEMA_A + "/";
    int prefixBytes = schemaPrefix.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    String tooLong = "a".repeat(SqlRoleManager.PG_MAX_ID_LENGTH - prefixBytes + 1);
    assertThrows(MolgenisException.class, () -> roleManager.createRole(SCHEMA_A, tooLong));
  }

  // ── listRoles: driven by role_permission_metadata rows ────────────────────

  @Test
  void listRoles_emptyBeforeAnyGrant() {
    assertTrue(roleManager.listRoles(SCHEMA_A).isEmpty());
  }

  @Test
  void listRoles_appearsAfterPermissionRowInserted() {
    roleManager.createRole(SCHEMA_A, "analyst");
    assertTrue(
        roleManager.listRoles(SCHEMA_A).isEmpty(),
        "listRoles must be empty before any permission row is inserted");

    roleManager.setPermissions(
        schemaA,
        "analyst",
        new PermissionSet()
            .putTable("anyTable", new PermissionSet.TablePermissions().setSelect(SelectScope.ALL)));

    assertTrue(roleManager.listRoles(SCHEMA_A).contains("analyst"));
  }

  @Test
  void listRoles_excludesSystemRoles() {
    for (String name : roleManager.listRoles(SCHEMA_A)) {
      assertFalse(
          roleManager.isSystemRole(name), "listRoles must never include system role: " + name);
    }
  }

  @Test
  void listRoles_isolatedPerSchema() {
    roleManager.createRole(SCHEMA_A, "onlyInA");
    roleManager.setPermissions(
        schemaA,
        "onlyInA",
        new PermissionSet()
            .putTable("t", new PermissionSet.TablePermissions().setSelect(SelectScope.ALL)));

    assertFalse(roleManager.listRoles(SCHEMA_B).contains("onlyInA"));
  }

  // ── deleteRole: removes rows from role_permission_metadata ────────────────

  @Test
  void deleteRole_removesPermissionRows() {
    roleManager.createRole(SCHEMA_A, "todelete");
    roleManager.setPermissions(
        schemaA,
        "todelete",
        new PermissionSet()
            .putTable("t", new PermissionSet.TablePermissions().setSelect(SelectScope.ALL)));
    assertTrue(roleManager.listRoles(SCHEMA_A).contains("todelete"));

    roleManager.deleteRole(SCHEMA_A, "todelete");

    assertFalse(roleManager.listRoles(SCHEMA_A).contains("todelete"));
  }

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
                new PermissionSet.TablePermissions()
                    .setSelect(SelectScope.ALL)
                    .setInsert(UpdateScope.OWN)
                    .setUpdate(UpdateScope.GROUP)
                    .setDelete(UpdateScope.NONE));

    roleManager.setPermissions(schemaA, "scoped", permissions);
    PermissionSet result = roleManager.getPermissionSet(SCHEMA_A, "scoped");

    PermissionSet.TablePermissions tp = result.getTables().get("myTable");
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
        permissions.putTable("t", new PermissionSet.TablePermissions().setSelect(SelectScope.ALL)));

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
            .putTable("tableA", new PermissionSet.TablePermissions().setSelect(SelectScope.OWN));
    PermissionSet second =
        new PermissionSet()
            .putTable("tableB", new PermissionSet.TablePermissions().setSelect(SelectScope.GROUP));

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

  // ── addGroupMembership / removeGroupMembership + MEMBER PG role lifecycle ──

  @Test
  void addGroupMembership_grantsMemberPgRole() {
    roleManager.createGroup(schemaA, "groupX");
    roleManager.createRole(SCHEMA_A, "roleX");

    roleManager.addGroupMembership(SCHEMA_A, "groupX", TEST_USER_ALICE, "roleX");

    String memberRole = SqlRoleManager.memberRoleName(SCHEMA_A);
    String pgUser = "MG_USER_" + TEST_USER_ALICE;
    boolean hasMembership =
        jooq.fetchExists(
            jooq.select()
                .from("pg_auth_members am")
                .join("pg_roles r")
                .on("r.oid = am.roleid")
                .join("pg_roles m")
                .on("m.oid = am.member")
                .where(field("r.rolname").eq(inline(memberRole)))
                .and(field("m.rolname").eq(inline(pgUser))));
    assertTrue(hasMembership, "MEMBER PG role must be granted after first addGroupMembership");

    roleManager.removeGroupMembership(SCHEMA_A, "groupX", TEST_USER_ALICE, "roleX");
    roleManager.deleteGroup(schemaA, "groupX");
  }

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

  @Test
  void removeGroupMembership_revokesWhenLastRow() {
    roleManager.createGroup(schemaA, "groupZ");
    roleManager.createRole(SCHEMA_A, "roleZ");

    roleManager.addGroupMembership(SCHEMA_A, "groupZ", TEST_USER_BOB, "roleZ");
    roleManager.removeGroupMembership(SCHEMA_A, "groupZ", TEST_USER_BOB, "roleZ");

    String memberRole = SqlRoleManager.memberRoleName(SCHEMA_A);
    String pgUser = "MG_USER_" + TEST_USER_BOB;
    boolean stillHasMembership =
        jooq.fetchExists(
            jooq.select()
                .from("pg_auth_members am")
                .join("pg_roles r")
                .on("r.oid = am.roleid")
                .join("pg_roles m")
                .on("m.oid = am.member")
                .where(field("r.rolname").eq(inline(memberRole)))
                .and(field("m.rolname").eq(inline(pgUser))));
    assertFalse(
        stillHasMembership,
        "MEMBER PG role must be revoked when user has no remaining group_membership_metadata rows");

    roleManager.deleteGroup(schemaA, "groupZ");
  }

  @Test
  void deleteRole_revokesMemberRoleFromAffectedUsers() {
    roleManager.createGroup(schemaA, "grpDel");
    roleManager.createRole(SCHEMA_A, "roleDel");

    roleManager.addGroupMembership(SCHEMA_A, "grpDel", TEST_USER_ALICE, "roleDel");
    roleManager.deleteRole(SCHEMA_A, "roleDel");

    String memberRole = SqlRoleManager.memberRoleName(SCHEMA_A);
    String pgUser = "MG_USER_" + TEST_USER_ALICE;
    boolean hasMembership =
        jooq.fetchExists(
            jooq.select()
                .from("pg_auth_members am")
                .join("pg_roles r")
                .on("r.oid = am.roleid")
                .join("pg_roles m")
                .on("m.oid = am.member")
                .where(field("r.rolname").eq(inline(memberRole)))
                .and(field("m.rolname").eq(inline(pgUser))));
    assertFalse(
        hasMembership,
        "MEMBER PG role must be revoked from user after deleteRole removes last row");

    roleManager.deleteGroup(schemaA, "grpDel");
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

      int updated =
          schemaEnf
              .getTable(ENFORCEMENT_TABLE)
              .update(new Row().setString("id", "r1").setString("val", "changed"));
      assertEquals(0, updated, "Viewer must not update (0 rows affected)");

      int deleted = schemaEnf.getTable(ENFORCEMENT_TABLE).delete(new Row().setString("id", "r1"));
      assertEquals(0, deleted, "Viewer must not delete (0 rows affected)");
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
    PermissionSet ps = new PermissionSet();
    PermissionSet.TablePermissions tp = new PermissionSet.TablePermissions();
    tp.setSelect(selectScope);
    tp.setInsert(insertScope);
    tp.setUpdate(updateScope);
    tp.setDelete(deleteScope);
    ps.putTable(ENFORCEMENT_TABLE, tp);
    roleManager.setPermissions(schemaEnf, roleName, ps);
    roleManager.enableRlsForTable(schemaEnf, ENFORCEMENT_TABLE);
  }
}
