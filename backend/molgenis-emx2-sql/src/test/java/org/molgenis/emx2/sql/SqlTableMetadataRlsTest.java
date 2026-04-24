package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class SqlTableMetadataRlsTest {

  private static final String TEST_SCHEMA = "testrls";
  private static final String TEST_TABLE = "t1";
  private static final String TEST_ROLE = "rls_test_role";
  private static final String PG_ROLE = "MG_ROLE_" + TEST_ROLE;

  private static final SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();

  private SqlTableMetadata tm;
  private SqlRoleManager roleManager;
  private DSLContext jooq;

  @BeforeEach
  void setup() {
    database.becomeAdmin();
    jooq = database.getJooq();
    roleManager = new SqlRoleManager(database);
    database.dropSchemaIfExists(TEST_SCHEMA);
    Schema schema = database.createSchema(TEST_SCHEMA);
    schema.create(table(TEST_TABLE).add(column("id").setType(ColumnType.INT).setKey(1)));
    tm = (SqlTableMetadata) schema.getMetadata().getTableMetadata(TEST_TABLE);
    cleanupTestRole();
  }

  @AfterEach
  void teardown() {
    database.becomeAdmin();
    cleanupTestRole();
    database.dropSchemaIfExists(TEST_SCHEMA);
  }

  private void cleanupTestRole() {
    jooq.execute("DROP ROLE IF EXISTS {0}", name(PG_ROLE));
    jooq.execute("DELETE FROM \"MOLGENIS\".role_metadata WHERE role_name = {0}", inline(TEST_ROLE));
  }

  @Test
  void enableBackfillsAndInstalls() {
    tm.setRowLevelSecurity(true);

    assertTrue(columnExists("mg_owner"), "mg_owner column should exist");
    assertTrue(columnExists("mg_roles"), "mg_roles column should exist");

    Record pgClass = fetchPgClass();
    assertNotNull(pgClass, "pg_class row should exist");
    assertTrue(
        Boolean.TRUE.equals(pgClass.get("relrowsecurity", Boolean.class)),
        "relrowsecurity should be true");
    assertTrue(
        Boolean.TRUE.equals(pgClass.get("relforcerowsecurity", Boolean.class)),
        "relforcerowsecurity should be true");

    assertTrue(ginIndexExists(), "GIN index on mg_roles should exist");
    assertTrue(guardTriggerExists(), "guard trigger mg_reserved_column_guard should exist");
    assertTrue(rlsPersistedInMetadata(), "row_level_security should be true in table_metadata");
    assertTrue(tm.getRowLevelSecurity(), "in-memory flag should be true");
  }

  @Test
  void disableBlockedWhenOwnGroupUsed() {
    tm.setRowLevelSecurity(true);
    roleManager.createRole(TEST_ROLE, "rls test role");

    PermissionSet ps = new PermissionSet();
    ps.put(
        new Permission(
            TEST_SCHEMA,
            TEST_TABLE,
            Permission.ViewScope.OWN,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, ps);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> tm.setRowLevelSecurity(false));
    assertTrue(
        ex.getMessage().toLowerCase().contains("own")
            || ex.getMessage().toLowerCase().contains("group")
            || ex.getMessage().toLowerCase().contains("rls"),
        "Exception should mention own/group or rls, got: " + ex.getMessage());

    PermissionSet empty = new PermissionSet();
    roleManager.setPermissions(TEST_ROLE, empty);

    assertDoesNotThrow(() -> tm.setRowLevelSecurity(false));
  }

  @Test
  void disableSafeDropsPolicies() {
    tm.setRowLevelSecurity(true);
    roleManager.createRole(TEST_ROLE, "rls test role");

    PermissionSet ps = new PermissionSet();
    ps.put(
        new Permission(
            TEST_SCHEMA,
            TEST_TABLE,
            Permission.ViewScope.ALL,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            Permission.EditScope.NONE,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, ps);

    PermissionSet empty = new PermissionSet();
    roleManager.setPermissions(TEST_ROLE, empty);

    tm.setRowLevelSecurity(false);

    Record pgClass = fetchPgClass();
    assertNotNull(pgClass);
    assertFalse(
        Boolean.TRUE.equals(pgClass.get("relrowsecurity", Boolean.class)),
        "relrowsecurity should be false after disable");

    assertFalse(guardTriggerExists(), "guard trigger should be gone after disable");
    assertFalse(policiesExistForTable(), "no MG_P policies should remain after disable");
    assertFalse(ginIndexExists(), "GIN index should be dropped after disable");

    assertTrue(columnExists("mg_owner"), "mg_owner column should still exist (dropColumns=false)");
    assertTrue(columnExists("mg_roles"), "mg_roles column should still exist (dropColumns=false)");
  }

  @Test
  void idempotent() {
    assertDoesNotThrow(() -> tm.setRowLevelSecurity(true));
    assertDoesNotThrow(() -> tm.setRowLevelSecurity(true));
    assertTrue(tm.getRowLevelSecurity());

    assertDoesNotThrow(() -> tm.setRowLevelSecurity(false));
    assertDoesNotThrow(() -> tm.setRowLevelSecurity(false));
    assertFalse(tm.getRowLevelSecurity());
  }

  private boolean columnExists(String columnName) {
    return jooq.fetchExists(
        jooq.select()
            .from(name("information_schema", "columns"))
            .where(
                field("table_schema")
                    .eq(inline(TEST_SCHEMA))
                    .and(field("table_name").eq(inline(TEST_TABLE)))
                    .and(field("column_name").eq(inline(columnName)))));
  }

  private Record fetchPgClass() {
    return jooq.fetchOne(
        "SELECT relrowsecurity, relforcerowsecurity FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = {0} AND c.relname = {1}",
        inline(TEST_SCHEMA), inline(TEST_TABLE));
  }

  private boolean ginIndexExists() {
    return jooq.fetchExists(
        jooq.select()
            .from(name("pg_indexes"))
            .where(
                field("schemaname")
                    .eq(inline(TEST_SCHEMA))
                    .and(field("tablename").eq(inline(TEST_TABLE)))
                    .and(field("indexname").eq(inline(TEST_TABLE + "_mg_roles_gin")))));
  }

  private boolean guardTriggerExists() {
    return jooq.fetchExists(
        jooq.select()
            .from("pg_trigger t")
            .join("pg_class c")
            .on("c.oid = t.tgrelid")
            .join("pg_namespace n")
            .on("n.oid = c.relnamespace")
            .where(
                field("n.nspname")
                    .eq(inline(TEST_SCHEMA))
                    .and(field("c.relname").eq(inline(TEST_TABLE)))
                    .and(field("t.tgname").eq(inline("mg_reserved_column_guard")))));
  }

  private boolean policiesExistForTable() {
    return jooq.fetchExists(
        jooq.select()
            .from("pg_policies")
            .where(
                field("schemaname")
                    .eq(inline(TEST_SCHEMA))
                    .and(field("tablename").eq(inline(TEST_TABLE)))
                    .and(field("policyname").like(inline("MG_P_%")))));
  }

  private boolean rlsPersistedInMetadata() {
    Record row =
        jooq.fetchOne(
            "SELECT row_level_security FROM \"MOLGENIS\".table_metadata WHERE table_schema = {0} AND table_name = {1}",
            inline(TEST_SCHEMA), inline(TEST_TABLE));
    return row != null && Boolean.TRUE.equals(row.get("row_level_security", Boolean.class));
  }
}
