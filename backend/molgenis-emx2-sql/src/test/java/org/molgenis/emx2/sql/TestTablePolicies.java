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
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;

class TestTablePolicies {

  private static final String SCHEMA_NAME = "TestTablePolicies";
  private static final String TABLE_SECURE = "SecureTable";
  private static final String GROUP_RED = "groupRed";
  private static final String GROUP_BLUE = "groupBlue";
  private static final String ROLE_ALL = "roleAll";
  private static final String ROLE_GROUP = "roleGroup";
  private static final String ROLE_OWN = "roleOwn";
  private static final String USER_ALICE = "TtpAlice";
  private static final String USER_BOB = "TtpBob";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final DSLContext jooq = ((SqlDatabase) db).getJooq();
  private static final SqlRoleManager roleManager = new SqlRoleManager((SqlDatabase) db);

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(TABLE_SECURE).add(column("id").setPkey()).add(column("val")));
    roleManager.createGroup(schema, GROUP_RED);
    roleManager.createGroup(schema, GROUP_BLUE);
    if (!db.hasUser(USER_ALICE)) db.addUser(USER_ALICE);
    if (!db.hasUser(USER_BOB)) db.addUser(USER_BOB);

    roleManager.createRole(schema, ROLE_ALL, "");
    roleManager.createRole(schema, ROLE_GROUP, "");
    roleManager.createRole(schema, ROLE_OWN, "");
    schema.getTable(TABLE_SECURE).getMetadata().setRlsEnabled(true);

    enableAllScope();
    enableGroupScope();
    enableOwnScope();

    insertRow("row-all", "visible-all", USER_ALICE, new String[] {GROUP_RED});
    insertRow("row-red", "visible-red", USER_BOB, new String[] {GROUP_RED});
    insertRow("row-blue", "visible-blue", USER_BOB, new String[] {GROUP_BLUE});
  }

  private void enableAllScope() {
    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_SECURE);
    tp.select(SelectScope.ALL);
    tp.insert(UpdateScope.ALL);
    tp.update(UpdateScope.ALL);
    tp.delete(UpdateScope.ALL);
    ps.putTable(TABLE_SECURE, tp);
    roleManager.setPermissions(schema, ROLE_ALL, ps);
  }

  private void enableGroupScope() {
    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_SECURE);
    tp.select(SelectScope.GROUP);
    tp.insert(UpdateScope.GROUP);
    tp.update(UpdateScope.GROUP);
    tp.delete(UpdateScope.GROUP);
    ps.putTable(TABLE_SECURE, tp);
    roleManager.setPermissions(schema, ROLE_GROUP, ps);
  }

  private void enableOwnScope() {
    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_SECURE);
    tp.select(SelectScope.OWN);
    tp.insert(UpdateScope.OWN);
    tp.update(UpdateScope.OWN);
    tp.delete(UpdateScope.OWN);
    ps.putTable(TABLE_SECURE, tp);
    roleManager.setPermissions(schema, ROLE_OWN, ps);
  }

  private void insertRow(String id, String val, String owner, String[] groups) {
    String sql =
        "INSERT INTO \""
            + SCHEMA_NAME
            + "\".\""
            + TABLE_SECURE
            + "\" (id, val, mg_owner, mg_groups) VALUES (?, ?, ?, ?)";
    jooq.execute(sql, id, val, owner, groups);
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  private long countPoliciesForTable(String tableName) {
    return jooq.fetchOne(
            "SELECT count(*) FROM pg_policies" + " WHERE schemaname = ? AND tablename = ?",
            SCHEMA_NAME,
            tableName)
        .get(0, Long.class);
  }

  private List<Row> selectAs(String userName) {
    db.setActiveUser(userName);
    try {
      return db.getSchema(SCHEMA_NAME).getTable(TABLE_SECURE).retrieveRows();
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void fourPoliciesPerRlsTable() {
    assertEquals(4L, countPoliciesForTable(TABLE_SECURE), "Exactly 4 policies per RLS table");
  }

  @Test
  void allScopeSeesAllRows() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_ALL);
    try {
      List<Row> rows = selectAs(USER_ALICE);
      assertEquals(3, rows.size(), "ALL scope must see all rows");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_ALL);
    }
  }

  @Test
  void groupScopeFiltersByMembership() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_GROUP);
    try {
      List<Row> rows = selectAs(USER_ALICE);
      assertTrue(
          rows.stream().anyMatch(r -> "row-red".equals(r.getString("id"))),
          "GROUP scope must see row tagged with own group");
      assertFalse(
          rows.stream().anyMatch(r -> "row-blue".equals(r.getString("id"))),
          "GROUP scope must not see row tagged with other group only");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_GROUP);
    }
  }

  @Test
  void ownScopeSeesOnlyOwnedRows() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_OWN);
    try {
      List<Row> rows = selectAs(USER_ALICE);
      assertTrue(
          rows.stream().anyMatch(r -> "row-all".equals(r.getString("id"))),
          "OWN scope must see own row");
      assertFalse(
          rows.stream().anyMatch(r -> "row-red".equals(r.getString("id"))),
          "OWN scope must not see rows owned by others");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_OWN);
    }
  }

  @Test
  void noneScopeIsRejectedBeforeRls() {
    roleManager.createRole(schema, "roleNone", "");
    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_SECURE);
    tp.select(SelectScope.NONE);
    tp.insert(UpdateScope.NONE);
    tp.update(UpdateScope.NONE);
    tp.delete(UpdateScope.NONE);
    ps.putTable(TABLE_SECURE, tp);
    roleManager.setPermissions(schema, "roleNone", ps);
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, "roleNone");
    try {
      assertThrows(
          MolgenisException.class,
          () -> selectAs(USER_ALICE),
          "NONE scope must be rejected by the Java permission layer");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, "roleNone");
      roleManager.deleteRole(SCHEMA_NAME, "roleNone");
    }
  }

  @Test
  void withCheckRejectsShareIntoForeignGroup() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_GROUP);
    try {
      db.setActiveUser(USER_ALICE);
      assertThrows(
          Exception.class,
          () -> insertRow("row-foreign", "val", USER_ALICE, new String[] {GROUP_BLUE}),
          "Sharing into a group the user is not a member of must be rejected");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_RED, USER_ALICE, ROLE_GROUP);
    }
  }
}
