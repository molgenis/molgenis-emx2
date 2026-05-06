package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.MG_ROLES;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestRowLevelSecurity {

  private static Database database;
  private static final String SCHEMA = "TestRowLevelSecurity";
  private static final String ARTICLES = "Articles";

  private static final String USER_TEAM_A = "rls_user_team_a";
  private static final String USER_TEAM_B = "rls_user_team_b";
  private static final String USER_VIEWER = "rls_user_viewer";
  private static final String USER_NO_ACCESS = "rls_user_noaccess";

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();

    for (String user : List.of(USER_TEAM_A, USER_TEAM_B, USER_VIEWER, USER_NO_ACCESS)) {
      if (!database.hasUser(user)) database.addUser(user);
    }

    Schema schema = database.dropCreateSchema(SCHEMA);
    schema.create(table(ARTICLES).add(column("id").setPkey()).add(column("title")));

    schema.createRole("TeamA");
    schema.createRole("TeamB");
    schema.grant(
        "TeamA",
        new TablePermission(ARTICLES)
            .select(true)
            .insert(true)
            .update(true)
            .delete(true)
            .rowLevel(true));
    schema.grant(
        "TeamB",
        new TablePermission(ARTICLES)
            .select(true)
            .insert(true)
            .update(true)
            .delete(true)
            .rowLevel(true));

    // Row visible only to TeamA
    schema
        .getTable(ARTICLES)
        .insert(
            new Row()
                .setString("id", "a1")
                .setString("title", "Team A only")
                .set(MG_ROLES, new String[] {"TeamA"}));
    // Row visible only to TeamB
    schema
        .getTable(ARTICLES)
        .insert(
            new Row()
                .setString("id", "b1")
                .setString("title", "Team B only")
                .set(MG_ROLES, new String[] {"TeamB"}));
    // Row visible to both teams
    schema
        .getTable(ARTICLES)
        .insert(
            new Row()
                .setString("id", "ab1")
                .setString("title", "Both teams")
                .set(MG_ROLES, new String[] {"TeamA", "TeamB"}));
    // Row with no mg_roles assigned (visible only to VIEWER and above, not to custom role users)
    schema
        .getTable(ARTICLES)
        .insert(new Row().setString("id", "open").setString("title", "Public"));

    schema.addMember(USER_TEAM_A, "TeamA");
    schema.addMember(USER_TEAM_B, "TeamB");
    schema.addMember(USER_VIEWER, Privileges.VIEWER.toString());
  }

  @Test
  void mgRolesColumnIsCreatedOnTable() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    assertNotNull(
        schema.getMetadata().getTableMetadata(ARTICLES).getColumn(MG_ROLES),
        "mg_roles column should exist after row-level grant");
  }

  @Test
  void isRowLevelReportedInPermissions() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    Role teamA = schema.getRoleInfo("TeamA");
    TablePermission perm =
        teamA.permissions().stream()
            .filter(p -> ARTICLES.equals(p.table()))
            .findFirst()
            .orElseThrow();
    assertEquals(Boolean.TRUE, perm.isRowLevel());
  }

  @Test
  void teamAUserSeesOnlyTeamARows() {
    database.setActiveUser(USER_TEAM_A);
    database.tx(
        db -> {
          List<Row> rows = db.getSchema(SCHEMA).getTable(ARTICLES).retrieveRows();
          List<String> ids = rows.stream().map(r -> r.getString("id")).toList();
          assertTrue(ids.contains("a1"), "should see TeamA row");
          assertTrue(ids.contains("ab1"), "should see row shared with TeamB");
          assertFalse(ids.contains("open"), "should NOT see row with no mg_roles assigned");
          assertFalse(ids.contains("b1"), "should NOT see TeamB-only row");
        });
  }

  @Test
  void teamBUserSeesOnlyTeamBRows() {
    database.setActiveUser(USER_TEAM_B);
    database.tx(
        db -> {
          List<Row> rows = db.getSchema(SCHEMA).getTable(ARTICLES).retrieveRows();
          List<String> ids = rows.stream().map(r -> r.getString("id")).toList();
          assertTrue(ids.contains("b1"), "should see TeamB row");
          assertTrue(ids.contains("ab1"), "should see row shared with TeamA");
          assertFalse(ids.contains("open"), "should NOT see row with no mg_roles assigned");
          assertFalse(ids.contains("a1"), "should NOT see TeamA-only row");
        });
  }

  @Test
  void userWithNoGrantCannotSelectTable() {
    database.setActiveUser(USER_NO_ACCESS);
    database.tx(
        db ->
            assertThrows(
                Exception.class, () -> db.getSchema(SCHEMA).getTable(ARTICLES).retrieveRows()));
  }

  @Test
  void adminSeesAllRows() {
    database.becomeAdmin();
    List<Row> rows = database.getSchema(SCHEMA).getTable(ARTICLES).retrieveRows();
    assertEquals(4, rows.size());
  }

  @Test
  void grantingRowLevelTwiceIsIdempotent() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    assertDoesNotThrow(
        () -> schema.grant("TeamA", new TablePermission(ARTICLES).select(true).rowLevel(true)));
  }

  @Test
  void teamAUserCanInsertRowVisibleToOwnTeam() {
    database.setActiveUser(USER_TEAM_A);
    database.tx(
        db -> {
          db.getSchema(SCHEMA)
              .getTable(ARTICLES)
              .insert(
                  new Row()
                      .setString("id", "a2")
                      .setString("title", "Inserted by TeamA")
                      .set(MG_ROLES, new String[] {"TeamA"}));
          List<Row> rows = db.getSchema(SCHEMA).getTable(ARTICLES).retrieveRows();
          assertTrue(rows.stream().anyMatch(r -> "a2".equals(r.getString("id"))));
        });

    // Cleanup
    database.becomeAdmin();
    database.getSchema(SCHEMA).getTable(ARTICLES).delete(new Row().setString("id", "a2"));
  }

  @Test
  void teamAUserCannotUpdateTeamBRow() {
    database.setActiveUser(USER_TEAM_A);
    database.tx(
        db ->
            db.getSchema(SCHEMA)
                .getTable(ARTICLES)
                .update(new Row().setString("id", "b1").setString("title", "hacked")));

    database.becomeAdmin();
    Row b1 =
        database.getSchema(SCHEMA).getTable(ARTICLES).retrieveRows().stream()
            .filter(r -> "b1".equals(r.getString("id")))
            .findFirst()
            .orElseThrow();
    assertEquals("Team B only", b1.getString("title"), "TeamB row should be unchanged");
  }

  @Test
  void teamAUserCannotDeleteTeamBRow() {
    database.setActiveUser(USER_TEAM_A);
    database.tx(
        db -> db.getSchema(SCHEMA).getTable(ARTICLES).delete(new Row().setString("id", "b1")));

    database.becomeAdmin();
    boolean b1StillExists =
        database.getSchema(SCHEMA).getTable(ARTICLES).retrieveRows().stream()
            .anyMatch(r -> "b1".equals(r.getString("id")));
    assertTrue(b1StillExists, "TeamB row should still exist after TeamA delete attempt");
  }

  @Test
  void revokingLastRowLevelGrantDisablesRls() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("TempRole");
    schema.grant("TempRole", new TablePermission(ARTICLES).select(true).rowLevel(true));

    assertTrue(
        schema.getRoleInfo("TempRole").permissions().stream()
            .anyMatch(p -> ARTICLES.equals(p.table()) && Boolean.TRUE.equals(p.isRowLevel())));

    schema.revoke("TempRole", ARTICLES);
    schema.deleteRole("TempRole");

    assertTrue(
        schema.getRoleInfo("TeamA").permissions().stream()
            .anyMatch(p -> ARTICLES.equals(p.table()) && Boolean.TRUE.equals(p.isRowLevel())));
  }

  @Test
  void viewerPlusCustomRoleSeesAllRowsButCanOnlyMutateOwned() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    String table = "WriteOnlyTable";
    String user = "rls_user_write_only";
    if (!database.hasUser(user)) database.addUser(user);

    schema.create(table(table).add(column("id").setPkey()).add(column("title")));
    schema.createRole("WriteOnlyRole");
    schema.grant(
        "WriteOnlyRole",
        new TablePermission(table)
            .select(true)
            .insert(true)
            .update(true)
            .delete(true)
            .rowLevel(true));
    // Assigning VIEWER gives the user visibility of all rows; the custom role restricts DML to
    // owned rows only.
    schema.addMember(user, "WriteOnlyRole");
    schema.addMember(user, Privileges.VIEWER.toString());

    // Admin inserts rows owned by different roles
    schema
        .getTable(table)
        .insert(
            new Row()
                .setString("id", "wo1")
                .setString("title", "owned")
                .set(MG_ROLES, new String[] {"WriteOnlyRole"}));
    schema
        .getTable(table)
        .insert(new Row().setString("id", "wo2").setString("title", "other role's row"));

    // User should see ALL rows (VIEWER bypass in SELECT policy)
    database.setActiveUser(user);
    database.tx(
        db -> {
          List<Row> rows = db.getSchema(SCHEMA).getTable(table).retrieveRows();
          List<String> ids = rows.stream().map(r -> r.getString("id")).toList();
          assertTrue(ids.contains("wo1"), "should see own row");
          assertTrue(ids.contains("wo2"), "should see row owned by no-one");
        });

    // User can update their own row
    database.setActiveUser(user);
    database.tx(
        db ->
            db.getSchema(SCHEMA)
                .getTable(table)
                .update(new Row().setString("id", "wo1").setString("title", "updated")));
    database.becomeAdmin();
    Row wo1 =
        database.getSchema(SCHEMA).getTable(table).retrieveRows().stream()
            .filter(r -> "wo1".equals(r.getString("id")))
            .findFirst()
            .orElseThrow();
    assertEquals("updated", wo1.getString("title"), "own row should be updated");

    // User cannot update a row they don't own (DML policy: no EDITOR, no mg_roles match)
    database.setActiveUser(user);
    database.tx(
        db ->
            db.getSchema(SCHEMA)
                .getTable(table)
                .update(new Row().setString("id", "wo2").setString("title", "hacked")));
    database.becomeAdmin();
    Row wo2 =
        database.getSchema(SCHEMA).getTable(table).retrieveRows().stream()
            .filter(r -> "wo2".equals(r.getString("id")))
            .findFirst()
            .orElseThrow();
    assertEquals("other role's row", wo2.getString("title"), "unowned row should be unchanged");

    // Cleanup
    database.becomeAdmin();
    schema.revoke("WriteOnlyRole", table);
    schema.deleteRole("WriteOnlyRole");
  }

  @Test
  void rlsIsDisabledWhenAllRowLevelGrantsAreRevoked() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    String table = "Solo";
    schema.create(table(table).add(column("id").setPkey()));
    schema.createRole("SoloRole");
    schema.grant("SoloRole", new TablePermission(table).select(true).rowLevel(true));

    assertTrue(
        schema.getRoleInfo("SoloRole").permissions().stream()
            .anyMatch(p -> table.equals(p.table()) && Boolean.TRUE.equals(p.isRowLevel())));

    schema.revoke("SoloRole", table);

    assertTrue(
        schema.getRoleInfo("SoloRole").permissions().stream()
            .noneMatch(p -> table.equals(p.table())));

    schema.deleteRole("SoloRole");
  }

  @Test
  void nonRlsRoleSeesAllRowsWhenAnotherRoleEnablesRls() {
    // Regression: granting RLS to one role must not restrict another role that has a plain
    // (non-RLS) grant on the same table.
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    String table = "MixedAccessTable";
    String rlsUser = "rls_user_mixed_rls";
    String plainUser = "rls_user_mixed_plain";
    for (String u : List.of(rlsUser, plainUser)) {
      if (!database.hasUser(u)) database.addUser(u);
    }

    schema.create(table(table).add(column("id").setPkey()).add(column("title")));
    schema.createRole("MixedRlsRole");
    schema.createRole("MixedPlainRole");

    // Plain (non-RLS) grant first
    schema.grant("MixedPlainRole", new TablePermission(table).select(true).insert(true));
    // RLS grant second — this is what used to break the plain grant
    schema.grant(
        "MixedRlsRole", new TablePermission(table).select(true).insert(true).rowLevel(true));

    schema
        .getTable(table)
        .insert(
            new Row()
                .setString("id", "m1")
                .setString("title", "rls row")
                .set(MG_ROLES, new String[] {"MixedRlsRole"}));
    schema.getTable(table).insert(new Row().setString("id", "m2").setString("title", "plain row"));

    schema.addMember(rlsUser, "MixedRlsRole");
    schema.addMember(plainUser, "MixedPlainRole");

    // RLS user sees only their row
    database.setActiveUser(rlsUser);
    database.tx(
        db -> {
          List<Row> rows = db.getSchema(SCHEMA).getTable(table).retrieveRows();
          List<String> ids = rows.stream().map(r -> r.getString("id")).toList();
          assertTrue(ids.contains("m1"), "RLS user should see their row");
          assertFalse(ids.contains("m2"), "RLS user should NOT see unowned row");
        });

    // Plain user sees ALL rows — the RLS grant for MixedRlsRole must not restrict MixedPlainRole
    database.setActiveUser(plainUser);
    database.tx(
        db -> {
          List<Row> rows = db.getSchema(SCHEMA).getTable(table).retrieveRows();
          List<String> ids = rows.stream().map(r -> r.getString("id")).toList();
          assertTrue(ids.contains("m1"), "plain user should see RLS row");
          assertTrue(ids.contains("m2"), "plain user should see plain row");
        });

    // Cleanup
    database.becomeAdmin();
    schema.revoke("MixedRlsRole", table);
    schema.revoke("MixedPlainRole", table);
    schema.deleteRole("MixedRlsRole");
    schema.deleteRole("MixedPlainRole");
  }

  @Test
  void tablePermissionsReportsIsRowLevelTrueForUserWithViewerAndRlsRole() {
    // Regression: getTablePermissionsForActiveUser must not return isRowLevel=false when the user
    // holds both VIEWER and a custom RLS role, caused by the internal RLS_ role appearing in the
    // inherited-roles list and overwriting is_row_level with false.
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    String user = "rls_user_viewer_and_rls";
    if (!database.hasUser(user)) database.addUser(user);
    schema.addMember(user, "TeamA");
    schema.addMember(user, Privileges.VIEWER.toString());

    database.setActiveUser(user);
    database.tx(
        db -> {
          List<TablePermission> perms = db.getSchema(SCHEMA).getPermissionsForActiveUser();
          TablePermission articlesPerm =
              perms.stream()
                  .filter(p -> ARTICLES.equals(p.table()))
                  .findFirst()
                  .orElseThrow(() -> new AssertionError("No permission found for " + ARTICLES));
          assertEquals(
              Boolean.TRUE,
              articlesPerm.isRowLevel(),
              "isRowLevel should be true for a user whose access comes from an RLS role");
        });

    database.becomeAdmin();
    schema.removeMember(user);
  }

  @Test
  void viewerSeesAllRowsDespiteRls() {
    database.setActiveUser(USER_VIEWER);
    database.tx(
        db -> {
          List<Row> rows = db.getSchema(SCHEMA).getTable(ARTICLES).retrieveRows();
          List<String> ids = rows.stream().map(r -> r.getString("id")).toList();
          assertTrue(ids.contains("a1"), "Viewer should see TeamA row");
          assertTrue(ids.contains("b1"), "Viewer should see TeamB row");
          assertTrue(ids.contains("ab1"), "Viewer should see shared row");
          assertTrue(ids.contains("open"), "Viewer should see public row");
        });
  }

  @Test
  void grantOnInheritedTableIsRejected() {
    database.becomeAdmin();
    Schema schema = database.dropCreateSchema("TestRlsInheritReject");
    schema.create(table("Root").add(column("id").setPkey()));
    schema.create(table("Sub").setInheritName("Root"));
    schema.createRole("RejectRole");

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () -> schema.grant("RejectRole", new TablePermission("Sub").select(true)));
    assertTrue(
        ex.getMessage().contains("inherited table"),
        "error should mention that the table is inherited: " + ex.getMessage());

    // Same restriction applies to row-level grants.
    assertThrows(
        MolgenisException.class,
        () -> schema.grant("RejectRole", new TablePermission("Sub").select(true).rowLevel(true)));
  }

  @Test
  void rlsGrantOnRootCrudWorksOnSubclassAcrossInheritanceTree() {
    database.becomeAdmin();
    String root = "InheritParent";
    String child = "InheritChild";
    String roleA = "InheritTeamA";
    String roleB = "InheritTeamB";
    String userA = "rls_inherit_a";
    String userB = "rls_inherit_b";
    Schema schema = database.dropCreateSchema("TestRlsInheritSubclass");
    for (String u : List.of(userA, userB)) {
      if (!database.hasUser(u)) database.addUser(u);
    }

    schema.create(table(root).add(column("id").setPkey()).add(column("title")));
    schema.create(table(child).setInheritName(root).add(column("childField")));

    schema.createRole(roleA);
    schema.createRole(roleB);
    // RLS grant on root propagates to every descendant in the tree.
    schema.grant(
        roleA,
        new TablePermission(root)
            .select(true)
            .insert(true)
            .update(true)
            .delete(true)
            .rowLevel(true));
    schema.grant(roleB, new TablePermission(root).select(true).rowLevel(true));

    schema.addMember(userA, roleA);
    schema.addMember(userB, roleB);

    // INSERT on subclass via TeamA
    database.setActiveUser(userA);
    database.tx(
        db ->
            db.getSchema(schema.getName())
                .getTable(child)
                .insert(
                    new Row()
                        .setString("id", "c1")
                        .setString("title", "team a child")
                        .setString("childField", "x")
                        .set(MG_ROLES, new String[] {roleA})));

    // SELECT on subclass: TeamA sees their row
    database.setActiveUser(userA);
    database.tx(
        db -> {
          List<String> ids =
              db.getSchema(schema.getName()).getTable(child).retrieveRows().stream()
                  .map(r -> r.getString("id"))
                  .toList();
          assertTrue(ids.contains("c1"), "TeamA should see their inserted row on the subclass");
        });

    // SELECT on root: TeamA can see the row through the root table too
    database.setActiveUser(userA);
    database.tx(
        db -> {
          List<String> ids =
              db.getSchema(schema.getName()).getTable(root).retrieveRows().stream()
                  .map(r -> r.getString("id"))
                  .toList();
          assertTrue(ids.contains("c1"), "TeamA should see the row when querying root");
        });

    // TeamB has no insert grant: should not see TeamA's row
    database.setActiveUser(userB);
    database.tx(
        db -> {
          List<String> ids =
              db.getSchema(schema.getName()).getTable(child).retrieveRows().stream()
                  .map(r -> r.getString("id"))
                  .toList();
          assertFalse(ids.contains("c1"), "TeamB should NOT see TeamA's row");
        });

    // UPDATE through subclass
    database.setActiveUser(userA);
    database.tx(
        db ->
            db.getSchema(schema.getName())
                .getTable(child)
                .update(new Row().setString("id", "c1").setString("title", "updated")));
    database.becomeAdmin();
    Row updated =
        database.getSchema(schema.getName()).getTable(child).retrieveRows().stream()
            .filter(r -> "c1".equals(r.getString("id")))
            .findFirst()
            .orElseThrow();
    assertEquals("updated", updated.getString("title"));

    // DELETE through subclass
    database.setActiveUser(userA);
    database.tx(
        db ->
            db.getSchema(schema.getName()).getTable(child).delete(new Row().setString("id", "c1")));
    database.becomeAdmin();
    assertTrue(
        database.getSchema(schema.getName()).getTable(child).retrieveRows().isEmpty(),
        "subclass row should be deleted");
    assertTrue(
        database.getSchema(schema.getName()).getTable(root).retrieveRows().isEmpty(),
        "root row should be deleted by cascade");
  }

  @Test
  void rlsGrantOnRootAllowsAccessIntoSubclasses() {
    database.becomeAdmin();
    String root = "RootParent";
    String child = "RootChild";
    String roleC = "InheritTeamC";
    String userC = "rls_inherit_c";
    Schema schema = database.dropCreateSchema("TestRlsInheritRoot");
    if (!database.hasUser(userC)) database.addUser(userC);

    schema.create(table(root).add(column("id").setPkey()).add(column("title")));
    schema.create(table(child).setInheritName(root).add(column("childField")));

    schema.createRole(roleC);
    // grant on root — must also propagate down so the root-side LEFT JOIN
    // to subclasses can read them.
    schema.grant(
        roleC,
        new TablePermission(root)
            .select(true)
            .insert(true)
            .update(true)
            .delete(true)
            .rowLevel(true));
    schema.addMember(userC, roleC);

    // Insert via subclass as TeamC: root insert must satisfy root's RLS
    database.setActiveUser(userC);
    database.tx(
        db ->
            db.getSchema(schema.getName())
                .getTable(child)
                .insert(
                    new Row()
                        .setString("id", "k1")
                        .setString("title", "via subclass")
                        .setString("childField", "y")
                        .set(MG_ROLES, new String[] {roleC})));

    // SELECT on root joins all subclasses (LEFT JOIN); user must have grant on subclasses
    database.setActiveUser(userC);
    database.tx(
        db -> {
          List<Row> rows = db.getSchema(schema.getName()).getTable(root).retrieveRows();
          assertTrue(rows.stream().anyMatch(r -> "k1".equals(r.getString("id"))));
        });
  }

  @Test
  void rlsGrantPropagatesAcrossMultiLevelChain() {
    database.becomeAdmin();
    String grand = "Grand";
    String mid = "Mid";
    String leaf = "Leaf";
    String roleD = "InheritTeamD";
    String userD = "rls_inherit_d";
    Schema schema = database.dropCreateSchema("TestRlsInheritMulti");
    if (!database.hasUser(userD)) database.addUser(userD);

    schema.create(table(grand).add(column("id").setPkey()).add(column("title")));
    schema.create(table(mid).setInheritName(grand).add(column("midField")));
    schema.create(table(leaf).setInheritName(mid).add(column("leafField")));

    schema.createRole(roleD);
    // Grant on the root only — propagation covers mid + leaf.
    schema.grant(
        roleD,
        new TablePermission(grand)
            .select(true)
            .insert(true)
            .update(true)
            .delete(true)
            .rowLevel(true));
    schema.addMember(userD, roleD);

    // Insert on the leaf — three physical inserts must each satisfy RLS on
    // grand, mid, and leaf.
    database.setActiveUser(userD);
    database.tx(
        db ->
            db.getSchema(schema.getName())
                .getTable(leaf)
                .insert(
                    new Row()
                        .setString("id", "l1")
                        .setString("title", "deep")
                        .setString("midField", "m")
                        .setString("leafField", "l")
                        .set(MG_ROLES, new String[] {roleD})));

    database.setActiveUser(userD);
    database.tx(
        db -> {
          assertEquals(1, db.getSchema(schema.getName()).getTable(leaf).retrieveRows().size());
          assertEquals(1, db.getSchema(schema.getName()).getTable(mid).retrieveRows().size());
          assertEquals(1, db.getSchema(schema.getName()).getTable(grand).retrieveRows().size());
        });
  }

  @Test
  void revokingRlsOnRootRemovesAccessAcrossTree() {
    database.becomeAdmin();
    String root = "RevokeParent";
    String child = "RevokeChild";
    String roleE = "InheritTeamE";
    String userE = "rls_inherit_e";
    Schema schema = database.dropCreateSchema("TestRlsInheritRevoke");
    if (!database.hasUser(userE)) database.addUser(userE);

    schema.create(table(root).add(column("id").setPkey()).add(column("title")));
    schema.create(table(child).setInheritName(root).add(column("childField")));

    schema.createRole(roleE);
    schema.grant(
        roleE,
        new TablePermission(root)
            .select(true)
            .insert(true)
            .update(true)
            .delete(true)
            .rowLevel(true));
    schema.addMember(userE, roleE);

    // Sanity: grant propagated — can read the subclass.
    database.setActiveUser(userE);
    database.tx(
        db ->
            assertDoesNotThrow(
                () -> db.getSchema(schema.getName()).getTable(child).retrieveRows()));

    database.becomeAdmin();
    schema.revoke(roleE, root);

    // getRoleInfo only surfaces root-table grants; after revoke, none remain.
    Role info = schema.getRoleInfo(roleE);
    assertTrue(
        info.permissions().stream().noneMatch(p -> root.equals(p.table())),
        "revoke should remove the root permission");

    // Subclass access must also be gone — the user should no longer be able to read it.
    database.setActiveUser(userE);
    database.tx(
        db ->
            assertThrows(
                Exception.class,
                () -> db.getSchema(schema.getName()).getTable(child).retrieveRows()));
  }
}
