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
import org.molgenis.emx2.TablePermission.SelectScope;

class SqlTableMetadataRlsTest {

  private static final String TEST_SCHEMA = "testrls";
  private static final String TEST_TABLE = "t1";
  private static final String TEST_ROLE = "rls test role";
  private static final String PG_ROLE = "MG_ROLE_" + TEST_ROLE;

  private static final SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();

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
    cleanupTestRole();
  }

  @AfterEach
  void teardown() {
    database.becomeAdmin();
    cleanupTestRole();
    database.dropSchemaIfExists(TEST_SCHEMA);
  }

  private void cleanupTestRole() {
    boolean exists =
        jooq.fetchExists(
            jooq.select().from(name("pg_roles")).where(field("rolname").eq(inline(PG_ROLE))));
    if (exists) {
      roleManager.deleteRole(TEST_ROLE);
    }
  }

  @Test
  void firstOwnGrantInstallsRls() {
    roleManager.createRole(TEST_ROLE, "rls test role");

    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.singletonSelect(SelectScope.OWN),
            TablePermission.UpdateScope.NONE,
            TablePermission.UpdateScope.NONE,
            TablePermission.UpdateScope.NONE,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, ps);

    assertTrue(columnExists("mg_owner"), "mg_owner column should exist after OWN grant");
    assertTrue(columnExists("mg_roles"), "mg_roles column should exist after OWN grant");

    Record pgClass = fetchPgClass();
    assertNotNull(pgClass, "pg_class row should exist");
    assertTrue(
        Boolean.TRUE.equals(pgClass.get("relrowsecurity", Boolean.class)),
        "relrowsecurity should be true after first OWN grant");
    assertTrue(
        Boolean.TRUE.equals(pgClass.get("relforcerowsecurity", Boolean.class)),
        "relforcerowsecurity should be true after first OWN grant");

    assertTrue(ginIndexExists(), "GIN index on mg_roles should exist after OWN grant");
    assertTrue(guardTriggerExists(), "guard trigger mg_enforce_row_authorisation should exist");
  }

  @Test
  void firstGroupGrantInstallsRls() {
    roleManager.createRole(TEST_ROLE, "rls test role");

    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.singletonSelect(SelectScope.GROUP),
            TablePermission.UpdateScope.NONE,
            TablePermission.UpdateScope.NONE,
            TablePermission.UpdateScope.NONE,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, ps);

    assertTrue(columnExists("mg_owner"), "mg_owner column should exist after GROUP grant");
    assertTrue(columnExists("mg_roles"), "mg_roles column should exist after GROUP grant");

    Record pgClass = fetchPgClass();
    assertNotNull(pgClass);
    assertTrue(
        Boolean.TRUE.equals(pgClass.get("relrowsecurity", Boolean.class)),
        "relrowsecurity should be true after first GROUP grant");
    assertTrue(ginIndexExists(), "GIN index should exist after GROUP grant");
    assertTrue(guardTriggerExists(), "guard trigger should exist after GROUP grant");
  }

  @Test
  void rlsInstallIdempotent() {
    roleManager.createRole(TEST_ROLE, "rls test role");

    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.singletonSelect(SelectScope.OWN),
            TablePermission.UpdateScope.NONE,
            TablePermission.UpdateScope.NONE,
            TablePermission.UpdateScope.NONE,
            false,
            false));
    assertDoesNotThrow(() -> roleManager.setPermissions(TEST_ROLE, ps), "first OWN grant");
    assertDoesNotThrow(
        () -> roleManager.setPermissions(TEST_ROLE, ps), "second OWN grant idempotent");

    assertTrue(columnExists("mg_owner"));
    assertTrue(ginIndexExists());
  }

  @Test
  void lastOwnGroupRemovedDropsPoliciesKeepsRls() {
    roleManager.createRole(TEST_ROLE, "rls test role");

    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.singletonSelect(SelectScope.OWN),
            TablePermission.UpdateScope.NONE,
            TablePermission.UpdateScope.NONE,
            TablePermission.UpdateScope.NONE,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, ps);
    assertTrue(policiesExistForRole(), "OWN policies should exist after grant");

    PermissionSet empty = new PermissionSet();
    roleManager.setPermissions(TEST_ROLE, empty);

    assertFalse(policiesExistForRole(), "Policies for role should be gone after clearing");

    Record pgClass = fetchPgClass();
    assertNotNull(pgClass);
    assertTrue(
        Boolean.TRUE.equals(pgClass.get("relrowsecurity", Boolean.class)),
        "relrowsecurity should remain true (infrastructure stays after last policy removed)");
    assertTrue(guardTriggerExists(), "guard trigger should remain after last policy removed");
    assertTrue(columnExists("mg_owner"), "mg_owner column should remain");
    assertTrue(columnExists("mg_roles"), "mg_roles column should remain");
    assertTrue(ginIndexExists(), "GIN index should remain");
  }

  @Test
  void allScopeGrantDoesNotInstallRls() {
    roleManager.createRole(TEST_ROLE, "rls test role");

    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.singletonSelect(SelectScope.ALL),
            TablePermission.UpdateScope.NONE,
            TablePermission.UpdateScope.NONE,
            TablePermission.UpdateScope.NONE,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, ps);

    Record pgClass = fetchPgClass();
    assertNotNull(pgClass);
    assertFalse(
        Boolean.TRUE.equals(pgClass.get("relrowsecurity", Boolean.class)),
        "ALL scope should not trigger RLS install");
    assertFalse(columnExists("mg_owner"), "mg_owner should not exist for ALL-only grant");
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
                    .and(field("t.tgname").eq(inline("mg_enforce_row_authorisation")))));
  }

  private boolean policiesExistForRole() {
    return jooq.fetchExists(
        jooq.select()
            .from("pg_policies")
            .where(
                field("schemaname")
                    .eq(inline(TEST_SCHEMA))
                    .and(field("tablename").eq(inline(TEST_TABLE)))
                    .and(field("policyname").like(inline("MG_P_" + TEST_ROLE + "_%")))));
  }
}
