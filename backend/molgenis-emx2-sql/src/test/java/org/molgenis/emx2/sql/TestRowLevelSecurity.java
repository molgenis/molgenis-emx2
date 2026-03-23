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
}
