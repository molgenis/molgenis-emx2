package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;

class TestSchemaWideCustomGrants {

  private static final String SCHEMA_NAME = "TestSchemaWideCustomGrants";
  private static final String TABLE_NAME = "Books";
  private static final String ROLE_ALL = "all-reader";
  private static final String ROLE_OWN = "own-reader";
  private static final String ROLE_GROUP = "group-reader";
  private static final String ROLE_STAFF = "staff";
  private static final String GROUP_DEPT1 = "dept1";
  private static final String USER_ALICE = "SwcgAlice";
  private static final String USER_BOB = "SwcgBob";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final SqlRoleManager roleManager = ((SqlDatabase) db).getRoleManager();

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(TABLE_NAME).add(column("id").setPkey()).add(column("val")));
    if (!db.hasUser(USER_ALICE)) db.addUser(USER_ALICE);
    if (!db.hasUser(USER_BOB)) db.addUser(USER_BOB);
    roleManager.createGroup(schema, GROUP_DEPT1);
  }

  @Test
  void nullGroupGrant_allScope_userReadsAllRows() {
    setupRole(ROLE_ALL, SelectScope.ALL, UpdateScope.NONE, UpdateScope.NONE, UpdateScope.NONE);
    db.becomeAdmin();
    schema.getTable(TABLE_NAME).insert(new Row().setString("id", "r1").setString("val", "v1"));

    roleManager.grantRoleToUser(schema, ROLE_ALL, USER_ALICE);

    db.setActiveUser(USER_ALICE);
    try {
      List<Row> rows = schema.getTable(TABLE_NAME).retrieveRows();
      List<String> ids = rows.stream().map(r -> r.getString("id")).toList();
      assertTrue(ids.contains("r1"), "Schema-wide ALL-scope grant must show all rows");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void nullGroupGrant_ownScope_userReadsOwnRowsOnly() {
    setupRole(ROLE_OWN, SelectScope.OWN, UpdateScope.OWN, UpdateScope.NONE, UpdateScope.NONE);
    db.becomeAdmin();
    schema.addMember(USER_BOB, "Editor");
    db.setActiveUser(USER_BOB);
    schema.getTable(TABLE_NAME).insert(new Row().setString("id", "bob-row").setString("val", "v"));
    db.becomeAdmin();

    roleManager.grantRoleToUser(schema, ROLE_OWN, USER_ALICE);
    db.setActiveUser(USER_ALICE);
    schema
        .getTable(TABLE_NAME)
        .insert(new Row().setString("id", "alice-row").setString("val", "v"));

    List<Row> rows = schema.getTable(TABLE_NAME).retrieveRows();
    List<String> ids = rows.stream().map(r -> r.getString("id")).toList();
    db.becomeAdmin();

    assertTrue(ids.contains("alice-row"), "Schema-wide OWN-scope grant must show own rows");
    assertFalse(ids.contains("bob-row"), "Schema-wide OWN-scope grant must not show other rows");
  }

  @Test
  void nullGroupGrant_groupScope_userSeesNothing() {
    setupRole(ROLE_GROUP, SelectScope.GROUP, UpdateScope.NONE, UpdateScope.NONE, UpdateScope.NONE);
    db.becomeAdmin();
    schema
        .getTable(TABLE_NAME)
        .insert(
            new Row()
                .setString("id", "tagged-row")
                .setString("val", "v")
                .setStringArray("mg_groups", GROUP_DEPT1));

    roleManager.grantRoleToUser(schema, ROLE_GROUP, USER_ALICE);

    db.setActiveUser(USER_ALICE);
    try {
      List<Row> rows = schema.getTable(TABLE_NAME).retrieveRows();
      assertTrue(
          rows.isEmpty(),
          "Schema-wide GROUP-scope grant (no real group membership) must show no rows");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void dropSchemaWideGrant_alsoRemovesGroupScopedGrant() {
    setupRole(ROLE_STAFF, SelectScope.ALL, UpdateScope.NONE, UpdateScope.NONE, UpdateScope.NONE);
    db.becomeAdmin();
    schema
        .getTable(TABLE_NAME)
        .insert(new Row().setString("id", "shared-row").setString("val", "v"));

    roleManager.grantRoleToUser(schema, ROLE_STAFF, USER_ALICE);
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_DEPT1, USER_ALICE, ROLE_STAFF);

    roleManager.revokeRoleFromUser(schema, ROLE_STAFF, USER_ALICE);

    int rowCount =
        ((SqlDatabase) db)
            .getJooq()
            .fetchOne(
                "SELECT count(*) FROM \"MOLGENIS\".group_membership_metadata"
                    + " WHERE schema_name = ? AND user_name = ? AND role_name = ?",
                SCHEMA_NAME,
                USER_ALICE,
                ROLE_STAFF)
            .get(0, Integer.class);
    assertEquals(0, rowCount, "All membership rows must be gone after no-group revoke");

    String fullRole = SqlRoleManager.fullRoleName(SCHEMA_NAME, ROLE_STAFF);
    String fullUser = org.molgenis.emx2.Constants.MG_USER_PREFIX + USER_ALICE;
    int pgGrantCount =
        ((SqlDatabase) db)
            .getJooq()
            .fetchOne(
                "SELECT count(*) FROM pg_auth_members am"
                    + " JOIN pg_roles r ON r.oid = am.roleid"
                    + " JOIN pg_roles m ON m.oid = am.member"
                    + " WHERE m.rolname = ? AND r.rolname = ?",
                fullUser,
                fullRole)
            .get(0, Integer.class);
    assertEquals(0, pgGrantCount, "PG role must be revoked after no-group drop");
  }

  @Test
  void grantSchemaWide_supersedesExistingGroupScopedGrants() {
    setupRole(ROLE_STAFF, SelectScope.ALL, UpdateScope.NONE, UpdateScope.NONE, UpdateScope.NONE);
    db.becomeAdmin();

    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_DEPT1, USER_ALICE, ROLE_STAFF);

    int rowsBefore =
        ((SqlDatabase) db)
            .getJooq()
            .fetchOne(
                "SELECT count(*) FROM \"MOLGENIS\".group_membership_metadata"
                    + " WHERE schema_name = ? AND user_name = ? AND role_name = ?",
                SCHEMA_NAME,
                USER_ALICE,
                ROLE_STAFF)
            .get(0, Integer.class);
    assertEquals(1, rowsBefore, "Setup: one group-bound row must exist before schema-wide grant");

    roleManager.grantRoleToUser(schema, ROLE_STAFF, USER_ALICE);

    int nullGroupCount =
        ((SqlDatabase) db)
            .getJooq()
            .fetchOne(
                "SELECT count(*) FROM \"MOLGENIS\".group_membership_metadata"
                    + " WHERE schema_name = ? AND user_name = ? AND role_name = ? AND group_name IS NULL",
                SCHEMA_NAME,
                USER_ALICE,
                ROLE_STAFF)
            .get(0, Integer.class);
    assertEquals(
        1, nullGroupCount, "Exactly one NULL-group row must exist after schema-wide grant");

    int dept1Count =
        ((SqlDatabase) db)
            .getJooq()
            .fetchOne(
                "SELECT count(*) FROM \"MOLGENIS\".group_membership_metadata"
                    + " WHERE schema_name = ? AND user_name = ? AND role_name = ? AND group_name = ?",
                SCHEMA_NAME,
                USER_ALICE,
                ROLE_STAFF,
                GROUP_DEPT1)
            .get(0, Integer.class);
    assertEquals(0, dept1Count, "DEPT1-bound row must be gone after schema-wide grant supersedes");

    String fullRole = SqlRoleManager.fullRoleName(SCHEMA_NAME, ROLE_STAFF);
    String fullUser = org.molgenis.emx2.Constants.MG_USER_PREFIX + USER_ALICE;
    int pgGrantCount =
        ((SqlDatabase) db)
            .getJooq()
            .fetchOne(
                "SELECT count(*) FROM pg_auth_members am"
                    + " JOIN pg_roles r ON r.oid = am.roleid"
                    + " JOIN pg_roles m ON m.oid = am.member"
                    + " WHERE m.rolname = ? AND r.rolname = ?",
                fullUser,
                fullRole)
            .get(0, Integer.class);
    assertEquals(1, pgGrantCount, "PG role must still be granted after schema-wide supersede");
  }

  @Test
  void dropGroupGrant_preservesSchemaWideGrant() {
    setupRole(ROLE_STAFF, SelectScope.ALL, UpdateScope.NONE, UpdateScope.NONE, UpdateScope.NONE);
    db.becomeAdmin();
    schema
        .getTable(TABLE_NAME)
        .insert(new Row().setString("id", "shared-row").setString("val", "v"));

    roleManager.grantRoleToUser(schema, ROLE_STAFF, USER_ALICE);
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_DEPT1, USER_ALICE, ROLE_STAFF);

    roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_DEPT1, USER_ALICE, ROLE_STAFF);

    db.setActiveUser(USER_ALICE);
    try {
      List<Row> rows = schema.getTable(TABLE_NAME).retrieveRows();
      assertFalse(
          rows.isEmpty(), "Schema-wide grant must survive revocation of group-scoped grant");
    } finally {
      db.becomeAdmin();
    }
  }

  private void setupRole(
      String roleName,
      SelectScope selectScope,
      UpdateScope insertScope,
      UpdateScope updateScope,
      UpdateScope deleteScope) {
    roleManager.createRole(SCHEMA_NAME, roleName);
    schema.getTable(TABLE_NAME).getMetadata().setRlsEnabled(true);
    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_NAME);
    tp.select(selectScope);
    tp.insert(insertScope);
    tp.update(updateScope);
    tp.delete(deleteScope);
    ps.putTable(TABLE_NAME, tp);
    roleManager.setPermissions(schema, roleName, ps);
  }
}
