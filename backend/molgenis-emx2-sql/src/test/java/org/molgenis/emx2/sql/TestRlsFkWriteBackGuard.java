package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;

/**
 * Tests the RLS FK write-back guard introduced in L.7. When a user reads a row containing a FK to
 * an RLS-restricted target they cannot see, the FK is returned as NULL (read-side clamp). If they
 * re-save that row with the clamped NULL, the guard must reject the write to prevent silently
 * clearing the real FK.
 */
class TestRlsFkWriteBackGuard {

  private static final String SCHEMA_TARGET = "TRlsFkWbTarget";
  private static final String SCHEMA_CHILD = "TRlsFkWbChild";

  private static final String TABLE_TARGET = "Target";
  private static final String TABLE_CHILD = "Child";

  private static final String ROLE_OWN_READER = "own-reader";
  private static final String GROUP_ALPHA = "groupAlpha";

  private static final String USER_U1 = "TrlsFkWbU1";
  private static final String USER_ADMIN_LIKE = "TrlsFkWbAdmin";

  private static final String ROW_T1 = "T1";
  private static final String ROW_T2 = "T2";
  private static final String ROW_C1 = "C1";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final SqlRoleManager roleManager = new SqlRoleManager((SqlDatabase) db);

  private Schema schemaTarget;
  private Schema schemaChild;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_CHILD);
    db.dropSchemaIfExists(SCHEMA_TARGET);

    schemaTarget = db.createSchema(SCHEMA_TARGET);
    schemaChild = db.createSchema(SCHEMA_CHILD);

    if (!db.hasUser(USER_U1)) db.addUser(USER_U1);
    if (!db.hasUser(USER_ADMIN_LIKE)) db.addUser(USER_ADMIN_LIKE);

    schemaTarget.create(table(TABLE_TARGET).add(column("id").setPkey()).add(column("label")));

    SqlTableMetadata targetMeta =
        (SqlTableMetadata) schemaTarget.getTable(TABLE_TARGET).getMetadata();
    targetMeta.setRlsEnabled(true);

    schemaChild.create(
        table(TABLE_CHILD)
            .add(column("id").setPkey())
            .add(column("label"))
            .add(
                column("target")
                    .setType(REF)
                    .setRefSchemaName(SCHEMA_TARGET)
                    .setRefTable(TABLE_TARGET)));

    roleManager.createGroup(schemaTarget, GROUP_ALPHA);
    roleManager.createRole(SCHEMA_TARGET, ROLE_OWN_READER);

    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_TARGET);
    tp.setSelect(SelectScope.OWN);
    tp.setInsert(UpdateScope.NONE);
    tp.setUpdate(UpdateScope.NONE);
    tp.setDelete(UpdateScope.NONE);
    ps.putTable(TABLE_TARGET, tp);
    roleManager.setPermissions(schemaTarget, ROLE_OWN_READER, ps);

    roleManager.addGroupMembership(SCHEMA_TARGET, GROUP_ALPHA, USER_U1, ROLE_OWN_READER);
    roleManager.addGroupMembership(SCHEMA_TARGET, GROUP_ALPHA, USER_ADMIN_LIKE, ROLE_OWN_READER);

    schemaChild.addMember(USER_U1, "Editor");
    schemaChild.addMember(USER_ADMIN_LIKE, "Editor");

    db.becomeAdmin();
    schemaTarget
        .getTable(TABLE_TARGET)
        .insert(
            new Row()
                .setString("id", ROW_T1)
                .setString("label", "target-1")
                .setString("mg_owner", USER_ADMIN_LIKE));

    schemaTarget
        .getTable(TABLE_TARGET)
        .insert(
            new Row()
                .setString("id", ROW_T2)
                .setString("label", "target-2")
                .setString("mg_owner", USER_U1));

    schemaChild
        .getTable(TABLE_CHILD)
        .insert(
            new Row().setString("id", ROW_C1).setString("label", "c1").setString("target", ROW_T1));
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_CHILD);
    db.dropSchemaIfExists(SCHEMA_TARGET);
  }

  @Test
  void readClamp_producesNullFk_forInvisibleTarget() {
    db.setActiveUser(USER_U1);
    try {
      List<Row> rows = schemaChild.getTable(TABLE_CHILD).retrieveRows();
      Row c1 = rows.stream().filter(r -> ROW_C1.equals(r.getString("id"))).findFirst().orElse(null);
      assertNotNull(c1, "C1 must be returned");
      assertNull(c1.getString("target"), "FK to invisible target must be clamped to NULL on read");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void resave_withClampedNull_isRejected() {
    db.setActiveUser(USER_U1);
    try {
      List<Row> rows = schemaChild.getTable(TABLE_CHILD).retrieveRows();
      Row c1 = rows.stream().filter(r -> ROW_C1.equals(r.getString("id"))).findFirst().orElse(null);
      assertNotNull(c1, "C1 must be returned");
      assertNull(c1.getString("target"), "pre-condition: FK clamped to null");

      MolgenisException ex =
          assertThrows(
              MolgenisException.class,
              () -> schemaChild.getTable(TABLE_CHILD).update(c1),
              "Re-saving clamped-null FK must throw");
      assertTrue(
          ex.getMessage().contains("Cannot null FK 'target'"),
          "Error must name the FK column; got: " + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("outside your read scope"),
          "Error must mention read scope; got: " + ex.getMessage());
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void update_withNewVisibleTarget_succeeds() {
    db.setActiveUser(USER_U1);
    try {
      Row update = new Row().setString("id", ROW_C1).setString("target", ROW_T2);
      assertDoesNotThrow(
          () -> schemaChild.getTable(TABLE_CHILD).update(update),
          "Updating FK to a visible target must succeed");

      List<Row> rows = schemaChild.getTable(TABLE_CHILD).retrieveRows();
      Row c1 = rows.stream().filter(r -> ROW_C1.equals(r.getString("id"))).findFirst().orElse(null);
      assertNotNull(c1, "C1 must be returned after update");
      assertEquals(ROW_T2, c1.getString("target"), "FK must point to new visible target");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void update_omittingFkColumn_preservesFk() {
    db.setActiveUser(USER_U1);
    try {
      Row update = new Row().setString("id", ROW_C1).setString("label", "updated-label");
      assertDoesNotThrow(
          () -> schemaChild.getTable(TABLE_CHILD).update(update),
          "Updating non-FK column with FK omitted must succeed");

      db.becomeAdmin();
      List<Row> rows = schemaChild.getTable(TABLE_CHILD).retrieveRows();
      Row c1 = rows.stream().filter(r -> ROW_C1.equals(r.getString("id"))).findFirst().orElse(null);
      assertNotNull(c1, "C1 must be returned");
      assertEquals(ROW_T1, c1.getString("target"), "FK must be preserved when column is omitted");
      assertEquals("updated-label", c1.getString("label"), "Label must be updated");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void adminUser_resave_withNullFk_succeeds() {
    db.setActiveUser(USER_ADMIN_LIKE);
    try {
      List<Row> rows = schemaChild.getTable(TABLE_CHILD).retrieveRows();
      Row c1 = rows.stream().filter(r -> ROW_C1.equals(r.getString("id"))).findFirst().orElse(null);
      assertNotNull(c1, "C1 must be returned for admin-like user");
      assertEquals(ROW_T1, c1.getString("target"), "Admin-like user owns T1 so FK must be visible");

      assertDoesNotThrow(
          () -> schemaChild.getTable(TABLE_CHILD).update(c1),
          "Admin-like user (target visible) re-saving same row must not trigger guard");
    } finally {
      db.becomeAdmin();
    }
  }
}
