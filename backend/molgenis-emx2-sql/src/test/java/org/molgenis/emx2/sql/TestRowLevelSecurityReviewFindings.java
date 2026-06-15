package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.MG_ROLES;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestRowLevelSecurityReviewFindings {

  private static Database database;

  private static final String ARTICLES = "Articles";
  private static final String TEAM_A = "TeamA";
  private static final String TEAM_B = "TeamB";
  private static final String RLS_TEAM_A = "RLS_TeamA";

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
  }

  private static String uniqueUser(String suffix) {
    String user = "rls_findings_" + suffix;
    if (!database.hasUser(user)) database.addUser(user);
    return user;
  }

  @Test
  void plainGrantOnTopOfRowLevelGrantStillFiltersRowsForCustomRole() {
    database.becomeAdmin();
    Schema schema = database.dropCreateSchema("TestRlsFindingMixedGrantBypass");
    String user = uniqueUser("mixed_bypass");

    schema.create(table(ARTICLES).add(column("id").setPkey()).add(column("title")));
    schema.createRole(TEAM_A);
    schema.createRole(TEAM_B);
    schema.grant(TEAM_A, new TablePermission(ARTICLES).select(true).rowLevel(true));
    schema.grant(TEAM_B, new TablePermission(ARTICLES).select(true).rowLevel(true));

    schema
        .getTable(ARTICLES)
        .insert(
            new Row()
                .setString("id", "a1")
                .setString("title", "Team A only")
                .set(MG_ROLES, new String[] {TEAM_A}));
    schema
        .getTable(ARTICLES)
        .insert(
            new Row()
                .setString("id", "b1")
                .setString("title", "Team B only")
                .set(MG_ROLES, new String[] {TEAM_B}));

    schema.addMember(user, TEAM_A);

    database.setActiveUser(user);
    database.tx(
        db -> {
          List<String> ids =
              db
                  .getSchema("TestRlsFindingMixedGrantBypass")
                  .getTable(ARTICLES)
                  .retrieveRows()
                  .stream()
                  .map(r -> r.getString("id"))
                  .toList();
          assertEquals(List.of("a1"), ids, "TeamA user should only see the TeamA-tagged row");
        });

    database.becomeAdmin();
    schema.grant(TEAM_A, new TablePermission(ARTICLES).select(true));

    database.setActiveUser(user);
    database.tx(
        db -> {
          List<String> ids =
              db
                  .getSchema("TestRlsFindingMixedGrantBypass")
                  .getTable(ARTICLES)
                  .retrieveRows()
                  .stream()
                  .map(r -> r.getString("id"))
                  .toList();
          assertEquals(
              List.of("a1"),
              ids,
              "a plain SELECT grant on top of the row-level grant must not bypass row-level"
                  + " filtering");
        });
  }

  @Test
  void addMemberRejectsInternalRlsRoleName() {
    database.becomeAdmin();
    Schema schema = database.dropCreateSchema("TestRlsFindingAddMemberRls");
    String user = uniqueUser("addmember_rls");

    schema.create(table(ARTICLES).add(column("id").setPkey()).add(column("title")));
    schema.createRole(TEAM_A);
    schema.grant(TEAM_A, new TablePermission(ARTICLES).select(true).rowLevel(true));

    assertThrows(
        MolgenisException.class,
        () -> schema.addMember(user, RLS_TEAM_A),
        "assigning an internal RLS_ proxy role directly must be rejected");
  }

  @Test
  void insertRejectsInternalRlsRoleNameInMgRoles() {
    database.becomeAdmin();
    Schema schema = database.dropCreateSchema("TestRlsFindingMgRolesRls");
    String user = uniqueUser("mgroles_rls");

    schema.create(table(ARTICLES).add(column("id").setPkey()).add(column("title")));
    schema.createRole(TEAM_A);
    schema.grant(TEAM_A, new TablePermission(ARTICLES).select(true).insert(true).rowLevel(true));
    schema.addMember(user, TEAM_A);

    database.setActiveUser(user);
    database.tx(
        db ->
            assertThrows(
                MolgenisException.class,
                () ->
                    db.getSchema("TestRlsFindingMgRolesRls")
                        .getTable(ARTICLES)
                        .insert(
                            new Row()
                                .setString("id", "x1")
                                .setString("title", "leak")
                                .set(MG_ROLES, new String[] {RLS_TEAM_A})),
                "mg_roles must reject the internal RLS_ proxy role name"));
    database.becomeAdmin();
  }

  @Test
  void deleteRoleFailsWhenMgRolesStillReferenceItAfterRevoke() {
    database.becomeAdmin();
    Schema schema = database.dropCreateSchema("TestRlsFindingDeleteAfterRevoke");
    String user = uniqueUser("delete_after_revoke");

    schema.create(table(ARTICLES).add(column("id").setPkey()).add(column("title")));
    schema.createRole(TEAM_A);
    schema.grant(TEAM_A, new TablePermission(ARTICLES).select(true).insert(true).rowLevel(true));
    schema.addMember(user, TEAM_A);

    database.setActiveUser(user);
    database.tx(
        db ->
            db.getSchema("TestRlsFindingDeleteAfterRevoke")
                .getTable(ARTICLES)
                .insert(
                    new Row()
                        .setString("id", "owned")
                        .setString("title", "owned by TeamA")
                        .set(MG_ROLES, new String[] {TEAM_A})));

    database.becomeAdmin();
    schema.revoke(TEAM_A, ARTICLES);

    assertThrows(
        MolgenisException.class,
        () -> schema.deleteRole(TEAM_A),
        "deleteRole must fail while rows still reference the role in mg_roles, even after the"
            + " table grant was revoked");
  }
}
