package org.molgenis.emx2.sql.rls;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.junit.jupiter.api.Assertions.*;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.TablePermission;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlRoleManager;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class SqlRoleManagerEmissionTest {

  private static final String TEST_SCHEMA = "emission_test_schema";
  private static final String TEST_TABLE = "t1";
  private static final String TEST_ROLE = "emission_test_role";
  private static final String PG_ROLE = "MG_ROLE_" + TEST_ROLE;

  private static final SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();

  private SqlRoleManager roleManager;
  private DSLContext jooq;

  @BeforeEach
  void setup() {
    database.becomeAdmin();
    jooq = database.getJooq();
    roleManager = new SqlRoleManager(database);
    dropTestPgRole();
    setupTestSchemaAndTable();
    createTestRole();
  }

  @AfterEach
  void teardown() {
    database.becomeAdmin();
    jooq = database.getJooq();
    dropTestSchemaAndTable();
    dropTestPgRole();
  }

  private void setupTestSchemaAndTable() {
    jooq.execute("CREATE SCHEMA IF NOT EXISTS \"" + TEST_SCHEMA + "\"");
    jooq.execute(
        "CREATE TABLE IF NOT EXISTS \""
            + TEST_SCHEMA
            + "\".\""
            + TEST_TABLE
            + "\" (id int PRIMARY KEY)");
  }

  private void enableRlsOnTestTable(DSLContext ctx, String schema, String table) {
    ctx.execute(
        "ALTER TABLE \""
            + schema
            + "\".\""
            + table
            + "\" ADD COLUMN IF NOT EXISTS mg_owner text DEFAULT current_user");
    ctx.execute(
        "ALTER TABLE \""
            + schema
            + "\".\""
            + table
            + "\" ADD COLUMN IF NOT EXISTS mg_roles text[] DEFAULT '{}'");
    ctx.execute("ALTER TABLE \"" + schema + "\".\"" + table + "\" ENABLE ROW LEVEL SECURITY");
    ctx.execute("ALTER TABLE \"" + schema + "\".\"" + table + "\" FORCE ROW LEVEL SECURITY");
  }

  private void dropTestSchemaAndTable() {
    jooq.execute("DROP SCHEMA IF EXISTS \"" + TEST_SCHEMA + "\" CASCADE");
  }

  private void createTestRole() {
    roleManager.createRole(TEST_ROLE, "emission test role");
  }

  private void dropTestPgRole() {
    jooq.execute("DROP ROLE IF EXISTS \"" + PG_ROLE + "\"");
  }

  private String fetchPolicyQual(String policyName) {
    Record row =
        jooq.fetchOne(
            "SELECT qual, with_check FROM pg_policies WHERE schemaname = {0} AND tablename = {1} AND policyname = {2}",
            inline(TEST_SCHEMA), inline(TEST_TABLE), inline(policyName));
    return row == null ? null : row.get("qual", String.class);
  }

  private String fetchPolicyWithCheck(String policyName) {
    Record row =
        jooq.fetchOne(
            "SELECT qual, with_check FROM pg_policies WHERE schemaname = {0} AND tablename = {1} AND policyname = {2}",
            inline(TEST_SCHEMA), inline(TEST_TABLE), inline(policyName));
    return row == null ? null : row.get("with_check", String.class);
  }

  @Test
  void allScopeUsesGrantOnly() {
    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.Scope.ALL,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));

    roleManager.setPermissions(TEST_ROLE, ps);

    boolean hasSelectGrant =
        jooq.fetchExists(
            jooq.select()
                .from("information_schema.role_table_grants")
                .where(field("grantee").eq(inline(PG_ROLE)))
                .and(field("table_schema").eq(inline(TEST_SCHEMA)))
                .and(field("table_name").eq(inline(TEST_TABLE)))
                .and(field("privilege_type").eq(inline("SELECT"))));
    assertTrue(hasSelectGrant, "SELECT grant should exist for scope=ALL");

    boolean hasPolicies =
        jooq.fetchExists(
            jooq.select()
                .from("pg_policies")
                .where(field("schemaname").eq(inline(TEST_SCHEMA)))
                .and(field("tablename").eq(inline(TEST_TABLE)))
                .and(field("roles").cast(String.class).contains(PG_ROLE)));
    assertFalse(hasPolicies, "No policies should exist for scope=ALL (GRANT only)");
  }

  @Test
  void clearScoped() {
    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, ps);

    boolean hasSelect =
        jooq.fetchExists(
            jooq.select()
                .from("information_schema.role_table_grants")
                .where(field("grantee").eq(inline(PG_ROLE)))
                .and(field("table_schema").eq(inline(TEST_SCHEMA)))
                .and(field("table_name").eq(inline(TEST_TABLE)))
                .and(field("privilege_type").eq(inline("SELECT"))));
    assertTrue(hasSelect, "SELECT grant should exist after first setPermissions");

    boolean hasInsert =
        jooq.fetchExists(
            jooq.select()
                .from("information_schema.role_table_grants")
                .where(field("grantee").eq(inline(PG_ROLE)))
                .and(field("table_schema").eq(inline(TEST_SCHEMA)))
                .and(field("table_name").eq(inline(TEST_TABLE)))
                .and(field("privilege_type").eq(inline("INSERT"))));
    assertTrue(hasInsert, "INSERT grant should exist after first setPermissions");

    PermissionSet empty = new PermissionSet();
    roleManager.setPermissions(TEST_ROLE, empty);

    boolean anyGrantRemains =
        jooq.fetchExists(
            jooq.select()
                .from("information_schema.role_table_grants")
                .where(field("grantee").eq(inline(PG_ROLE)))
                .and(field("table_schema").eq(inline(TEST_SCHEMA)))
                .and(field("table_name").eq(inline(TEST_TABLE))));
    assertFalse(
        anyGrantRemains, "All grants should be cleared after setPermissions with empty set");
  }

  @Test
  void selectOwn() {
    enableRlsOnTestTable(jooq, TEST_SCHEMA, TEST_TABLE);

    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.Scope.OWN,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, ps);

    String policyName = "MG_P_" + TEST_ROLE + "_SELECT_OWN";
    String qual = fetchPolicyQual(policyName);
    assertNotNull(qual, "Policy MG_P_<role>_SELECT_OWN should exist");
    String qualLower = qual.toLowerCase();
    assertTrue(
        qualLower.contains("mg_owner") && qualLower.contains("current_user"),
        "USING clause should reference mg_owner and current_user, got: " + qual);

    String withCheck = fetchPolicyWithCheck(policyName);
    assertNull(withCheck, "SELECT policy should have no WITH CHECK clause");
  }

  @Test
  void selectGroup() {
    enableRlsOnTestTable(jooq, TEST_SCHEMA, TEST_TABLE);

    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.Scope.GROUP,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, ps);

    String policyName = "MG_P_" + TEST_ROLE + "_SELECT_GROUP";
    String qual = fetchPolicyQual(policyName);
    assertNotNull(qual, "Policy MG_P_<role>_SELECT_GROUP should exist");
    assertTrue(
        qual.contains("mg_roles") && qual.contains("current_user_roles"),
        "USING clause should reference mg_roles and current_user_roles, got: " + qual);
  }

  @Test
  void insertPolicies() {
    enableRlsOnTestTable(jooq, TEST_SCHEMA, TEST_TABLE);

    PermissionSet ownPs = new PermissionSet();
    ownPs.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.Scope.OWN,
            TablePermission.Scope.OWN,
            TablePermission.Scope.OWN,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, ownPs);

    String ownPolicyName = "MG_P_" + TEST_ROLE + "_INSERT_OWN";
    String ownWithCheck = fetchPolicyWithCheck(ownPolicyName);
    assertNotNull(ownWithCheck, "INSERT OWN policy should exist");
    String ownWithCheckLower = ownWithCheck.toLowerCase();
    assertTrue(
        ownWithCheckLower.contains("mg_owner") && ownWithCheckLower.contains("current_user"),
        "INSERT OWN WITH CHECK should reference mg_owner and current_user, got: " + ownWithCheck);
    assertNull(fetchPolicyQual(ownPolicyName), "INSERT policy should have no USING clause");

    PermissionSet groupPs = new PermissionSet();
    groupPs.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.Scope.GROUP,
            TablePermission.Scope.GROUP,
            TablePermission.Scope.GROUP,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, groupPs);

    String groupPolicyName = "MG_P_" + TEST_ROLE + "_INSERT_GROUP";
    String groupWithCheck = fetchPolicyWithCheck(groupPolicyName);
    assertNotNull(groupWithCheck, "INSERT GROUP policy should exist");
    assertTrue(
        groupWithCheck.contains("mg_roles") && groupWithCheck.contains("current_user_roles"),
        "INSERT GROUP WITH CHECK should reference mg_roles and current_user_roles, got: "
            + groupWithCheck);
    assertTrue(
        groupWithCheck.contains("cardinality"),
        "INSERT GROUP WITH CHECK should reference cardinality, got: " + groupWithCheck);

    PermissionSet allPs = new PermissionSet();
    allPs.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, allPs);

    boolean hasSelectGrant =
        jooq.fetchExists(
            jooq.select()
                .from("information_schema.role_table_grants")
                .where(field("grantee").eq(inline(PG_ROLE)))
                .and(field("table_schema").eq(inline(TEST_SCHEMA)))
                .and(field("table_name").eq(inline(TEST_TABLE)))
                .and(field("privilege_type").eq(inline("INSERT"))));
    assertTrue(hasSelectGrant, "INSERT ALL scope should produce a GRANT");
  }

  @Test
  void updatePoliciesUsingAndWithCheck() {
    enableRlsOnTestTable(jooq, TEST_SCHEMA, TEST_TABLE);

    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.Scope.OWN,
            TablePermission.Scope.NONE,
            TablePermission.Scope.OWN,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, ps);

    String policyName = "MG_P_" + TEST_ROLE + "_UPDATE_OWN";
    String qual = fetchPolicyQual(policyName);
    String withCheck = fetchPolicyWithCheck(policyName);

    assertNotNull(qual, "UPDATE OWN policy should have a USING clause");
    assertNotNull(withCheck, "UPDATE OWN policy should have a WITH CHECK clause");
    String qualLower = qual.toLowerCase();
    assertTrue(
        qualLower.contains("mg_owner") && qualLower.contains("current_user"),
        "UPDATE USING should reference mg_owner and current_user, got: " + qual);
    assertTrue(
        withCheck.contains("true"),
        "UPDATE OWN WITH CHECK should be (true) to allow changeOwner, got: " + withCheck);
  }

  @Test
  void deletePolicy() {
    enableRlsOnTestTable(jooq, TEST_SCHEMA, TEST_TABLE);

    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.Scope.OWN,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.OWN,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, ps);

    String policyName = "MG_P_" + TEST_ROLE + "_DELETE_OWN";
    String qual = fetchPolicyQual(policyName);
    assertNotNull(qual, "DELETE OWN policy should have a USING clause");
    String qualLower = qual.toLowerCase();
    assertTrue(
        qualLower.contains("mg_owner") && qualLower.contains("current_user"),
        "DELETE USING should reference mg_owner and current_user, got: " + qual);

    String withCheck = fetchPolicyWithCheck(policyName);
    assertNull(withCheck, "DELETE policy should have no WITH CHECK clause");
  }

  @Test
  void roundTrip() {
    enableRlsOnTestTable(jooq, TEST_SCHEMA, TEST_TABLE);

    TablePermission input =
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.Scope.ALL,
            TablePermission.Scope.OWN,
            TablePermission.Scope.GROUP,
            TablePermission.Scope.NONE,
            false,
            false);
    PermissionSet ps = new PermissionSet();
    ps.put(input);
    roleManager.setPermissions(TEST_ROLE, ps);

    PermissionSet retrieved = roleManager.getPermissions(TEST_ROLE);
    TablePermission resolved = retrieved.resolveFor(TEST_SCHEMA, TEST_TABLE);

    assertEquals(input.select(), resolved.select(), "select scope should round-trip");
    assertEquals(input.insert(), resolved.insert(), "insert scope should round-trip");
    assertEquals(input.update(), resolved.update(), "update scope should round-trip");
    assertEquals(input.delete(), resolved.delete(), "delete scope should round-trip");
  }

  @Test
  void revokeTablePrivilegeRemovesGrant() {
    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, ps);

    boolean hasSelectBefore =
        jooq.fetchExists(
            jooq.select()
                .from("information_schema.role_table_grants")
                .where(field("grantee").eq(inline(PG_ROLE)))
                .and(field("table_schema").eq(inline(TEST_SCHEMA)))
                .and(field("table_name").eq(inline(TEST_TABLE)))
                .and(field("privilege_type").eq(inline("SELECT"))));
    assertTrue(hasSelectBefore, "SELECT grant must exist before revoke");

    SqlPermissionExecutor.revokeTablePrivilege(jooq, PG_ROLE, TEST_SCHEMA, TEST_TABLE, "SELECT");

    boolean hasSelectAfter =
        jooq.fetchExists(
            jooq.select()
                .from("information_schema.role_table_grants")
                .where(field("grantee").eq(inline(PG_ROLE)))
                .and(field("table_schema").eq(inline(TEST_SCHEMA)))
                .and(field("table_name").eq(inline(TEST_TABLE)))
                .and(field("privilege_type").eq(inline("SELECT"))));
    assertFalse(hasSelectAfter, "SELECT grant must be gone after revoke");

    boolean hasInsert =
        jooq.fetchExists(
            jooq.select()
                .from("information_schema.role_table_grants")
                .where(field("grantee").eq(inline(PG_ROLE)))
                .and(field("table_schema").eq(inline(TEST_SCHEMA)))
                .and(field("table_name").eq(inline(TEST_TABLE)))
                .and(field("privilege_type").eq(inline("INSERT"))));
    assertTrue(hasInsert, "INSERT grant must remain after revoking only SELECT");
  }

  @Test
  void revokeTablePrivilegeNeverGrantedIsIdempotent() {
    boolean hasGrantBefore =
        jooq.fetchExists(
            jooq.select()
                .from("information_schema.role_table_grants")
                .where(field("grantee").eq(inline(PG_ROLE)))
                .and(field("table_schema").eq(inline(TEST_SCHEMA)))
                .and(field("table_name").eq(inline(TEST_TABLE))));
    assertFalse(hasGrantBefore, "No grants should exist yet");

    assertDoesNotThrow(
        () ->
            SqlPermissionExecutor.revokeTablePrivilege(
                jooq, PG_ROLE, TEST_SCHEMA, TEST_TABLE, "SELECT"),
        "Revoking a never-granted privilege must not throw");
  }

  @Test
  void allScopeAddsBypassWhenRlsEnabled() {
    enableRlsOnTestTable(jooq, TEST_SCHEMA, TEST_TABLE);

    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            TEST_SCHEMA,
            TEST_TABLE,
            TablePermission.Scope.ALL,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(TEST_ROLE, ps);

    String policyName = "MG_P_" + TEST_ROLE + "_SELECT_ALL";
    String qual = fetchPolicyQual(policyName);
    assertNotNull(qual, "SELECT ALL policy should exist when RLS is enabled on the table");
    assertTrue(qual.contains("true"), "SELECT ALL policy USING should be (true), got: " + qual);

    boolean hasSelectGrant =
        jooq.fetchExists(
            jooq.select()
                .from("information_schema.role_table_grants")
                .where(field("grantee").eq(inline(PG_ROLE)))
                .and(field("table_schema").eq(inline(TEST_SCHEMA)))
                .and(field("table_name").eq(inline(TEST_TABLE)))
                .and(field("privilege_type").eq(inline("SELECT"))));
    assertTrue(hasSelectGrant, "SELECT GRANT should also exist for scope=ALL");
  }
}
