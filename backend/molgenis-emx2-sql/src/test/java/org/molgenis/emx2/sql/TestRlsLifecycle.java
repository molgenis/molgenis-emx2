package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;

class TestRlsLifecycle {

  private static final String SCHEMA_NAME = "TestRlsLifecycle";
  private static final String TABLE_ONE = "TableOne";
  private static final String GROUP_ALPHA = "groupAlpha";
  private static final String ROLE_READER = "reader";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final DSLContext jooq = ((SqlDatabase) db).getJooq();
  private static final SqlRoleManager roleManager = new SqlRoleManager((SqlDatabase) db);

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(TABLE_ONE).add(column("id").setPkey()).add(column("val")));
    roleManager.createGroup(schema, GROUP_ALPHA);
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  private boolean tableHasRls(String tableName) {
    Boolean result =
        jooq.fetchOne(
                "SELECT c.relrowsecurity FROM pg_class c"
                    + " JOIN pg_namespace n ON n.oid = c.relnamespace"
                    + " WHERE n.nspname = ? AND c.relname = ?",
                SCHEMA_NAME,
                tableName)
            .get(0, Boolean.class);
    return Boolean.TRUE.equals(result);
  }

  private long countPoliciesForTable(String tableName) {
    return jooq.fetchOne(
            "SELECT count(*) FROM pg_policies" + " WHERE schemaname = ? AND tablename = ?",
            SCHEMA_NAME,
            tableName)
        .get(0, Long.class);
  }

  private boolean memberRoleHasVerbGrantOnTable(String tableName) {
    String memberRole = SqlRoleManager.memberRoleName(SCHEMA_NAME);
    Long count =
        jooq.fetchOne(
                "SELECT count(*) FROM information_schema.role_table_grants"
                    + " WHERE grantee = ? AND table_schema = ? AND table_name = ?"
                    + " AND privilege_type IN ('SELECT','INSERT','UPDATE','DELETE')",
                memberRole,
                SCHEMA_NAME,
                tableName)
            .get(0, Long.class);
    return count != null && count > 0L;
  }

  @Test
  void tableStartsNonRls() {
    assertFalse(tableHasRls(TABLE_ONE), "Table must not have RLS before any custom-role grant");
  }

  @Test
  void firstNonNoneRowEnablesRls() {
    roleManager.createRole(SCHEMA_NAME, ROLE_READER);
    PermissionSet ps = new PermissionSet();
    PermissionSet.TablePermissions tp = new PermissionSet.TablePermissions();
    tp.setSelect(SelectScope.GROUP);
    tp.setInsert(UpdateScope.NONE);
    tp.setUpdate(UpdateScope.NONE);
    tp.setDelete(UpdateScope.NONE);
    ps.putTable(TABLE_ONE, tp);
    roleManager.setPermissions(schema, ROLE_READER, ps);

    assertTrue(tableHasRls(TABLE_ONE), "RLS must be enabled after first non-NONE exact-table row");
    assertEquals(
        4L, countPoliciesForTable(TABLE_ONE), "Exactly 4 policies must exist per RLS table");
  }

  @Test
  void lastNoneRowDisablesRls() {
    roleManager.createRole(SCHEMA_NAME, ROLE_READER);
    PermissionSet psOn = new PermissionSet();
    PermissionSet.TablePermissions tpOn = new PermissionSet.TablePermissions();
    tpOn.setSelect(SelectScope.ALL);
    tpOn.setInsert(UpdateScope.NONE);
    tpOn.setUpdate(UpdateScope.NONE);
    tpOn.setDelete(UpdateScope.NONE);
    psOn.putTable(TABLE_ONE, tpOn);
    roleManager.setPermissions(schema, ROLE_READER, psOn);
    assertTrue(tableHasRls(TABLE_ONE), "RLS must be on after grant");

    PermissionSet psOff = new PermissionSet();
    PermissionSet.TablePermissions tpOff = new PermissionSet.TablePermissions();
    tpOff.setSelect(SelectScope.NONE);
    tpOff.setInsert(UpdateScope.NONE);
    tpOff.setUpdate(UpdateScope.NONE);
    tpOff.setDelete(UpdateScope.NONE);
    psOff.putTable(TABLE_ONE, tpOff);
    roleManager.setPermissions(schema, ROLE_READER, psOff);

    assertFalse(tableHasRls(TABLE_ONE), "RLS must be disabled after last non-NONE scope removed");
    assertEquals(
        0L, countPoliciesForTable(TABLE_ONE), "Policies must be dropped when RLS disabled");
  }

  @Test
  void wildcardDoesNotEnableRls() {
    assertFalse(
        tableHasRls(TABLE_ONE), "Wildcard system-role rows must not enable RLS on any table");
    assertEquals(
        0L,
        countPoliciesForTable(TABLE_ONE),
        "No per-table policies must exist from wildcard system-role rows");
  }

  @Test
  void memberGrantToggles() {
    roleManager.createRole(SCHEMA_NAME, ROLE_READER);
    PermissionSet psOn = new PermissionSet();
    PermissionSet.TablePermissions tpOn = new PermissionSet.TablePermissions();
    tpOn.setSelect(SelectScope.ALL);
    tpOn.setInsert(UpdateScope.NONE);
    tpOn.setUpdate(UpdateScope.NONE);
    tpOn.setDelete(UpdateScope.NONE);
    psOn.putTable(TABLE_ONE, tpOn);
    roleManager.setPermissions(schema, ROLE_READER, psOn);

    assertTrue(
        memberRoleHasVerbGrantOnTable(TABLE_ONE),
        "MEMBER role must have verb GRANTs after RLS-enable");

    PermissionSet psOff = new PermissionSet();
    PermissionSet.TablePermissions tpOff = new PermissionSet.TablePermissions();
    tpOff.setSelect(SelectScope.NONE);
    tpOff.setInsert(UpdateScope.NONE);
    tpOff.setUpdate(UpdateScope.NONE);
    tpOff.setDelete(UpdateScope.NONE);
    psOff.putTable(TABLE_ONE, tpOff);
    roleManager.setPermissions(schema, ROLE_READER, psOff);

    assertFalse(
        memberRoleHasVerbGrantOnTable(TABLE_ONE),
        "MEMBER role must lose verb GRANTs after RLS-disable");
  }
}
