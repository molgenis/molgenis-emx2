package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Constants.MG_ROLE_PREFIX;

import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Permission;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.Role;

class SqlRoleManagerTest {

  private static final String TEST_USER = "rmt_test_user";
  private static final String FORTY_CHAR_NAME = "a".repeat(40);

  private static final SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();

  private SqlRoleManager roleManager;

  @BeforeEach
  void setup() {
    database.becomeAdmin();
    roleManager = new SqlRoleManager(database);
    cleanupRole();
    cleanupTestPgRoles();
    ensureTestUserExists();
  }

  @AfterEach
  void teardown() {
    database.becomeAdmin();
    cleanupRole();
    cleanupTestPgRoles();
  }

  private void cleanupRole() {
    database.getJooq().deleteFrom(table(name(MetadataUtils.MOLGENIS, "role_metadata"))).execute();
  }

  private void cleanupTestPgRoles() {
    DSLContext jooq = database.getJooq();
    List<String> toClean =
        jooq.fetch(
                "SELECT rolname FROM pg_roles WHERE rolname LIKE 'MG_ROLE_rmt_%' OR rolname = {0}",
                org.jooq.impl.DSL.inline("MG_ROLE_" + FORTY_CHAR_NAME))
            .map(r -> r.get(0, String.class));
    for (String rolName : toClean) {
      try {
        jooq.execute("DROP ROLE IF EXISTS {0}", name(rolName));
      } catch (Exception ignored) {
        // best effort
      }
    }
  }

  private void ensureTestUserExists() {
    if (!database.hasUser(TEST_USER)) {
      database.addUser(TEST_USER);
    }
  }

  @Test
  void roleNameLengthCap() {
    assertDoesNotThrow(() -> roleManager.createRole(FORTY_CHAR_NAME, "ok"));

    String fortyOne = "a".repeat(41);
    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> roleManager.createRole(fortyOne, "too long"));
    assertTrue(ex.getMessage().contains("40"));
  }

  @Test
  void rejectBuiltinNameCollision() {
    assertThrows(MolgenisException.class, () -> roleManager.createRole("Viewer", "desc"));
    assertThrows(MolgenisException.class, () -> roleManager.createRole("viewer", "desc"));
    assertThrows(MolgenisException.class, () -> roleManager.createRole("EDITOR", "desc"));
  }

  @Test
  void createPersistsRoleAndPgRole() {
    roleManager.createRole("rmt_alpha", "desc");

    Role found = MetadataUtils.getRole(database.getJooq(), "rmt_alpha");
    assertNotNull(found);
    assertEquals("rmt_alpha", found.getRoleName());
    assertEquals("active", found.getStatus());

    boolean pgRoleExists =
        database
            .getJooq()
            .fetchExists(
                database
                    .getJooq()
                    .select()
                    .from("pg_roles")
                    .where(field("rolname").eq(MG_ROLE_PREFIX + "rmt_alpha")));
    assertTrue(pgRoleExists, "PG role MG_ROLE_rmt_alpha must exist");
  }

  @Test
  void immutableBuiltinsRejected() {
    DSLContext jooq = database.getJooq();
    Role builtIn = new Role("rmt_immutable");
    builtIn.setImmutable(true);
    MetadataUtils.saveRole(jooq, builtIn);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> roleManager.deleteRole("rmt_immutable"));
    assertTrue(
        ex.getMessage().toLowerCase().contains("immutable")
            || ex.getMessage().toLowerCase().contains("built-in"),
        "Exception message should mention immutability");
  }

  @Test
  void grantMembership() {
    roleManager.createRole("rmt_grant", "for grant test");
    roleManager.grantRoleToUser("rmt_grant", TEST_USER);

    boolean granted =
        database
            .getJooq()
            .fetchExists(
                database
                    .getJooq()
                    .select()
                    .from("pg_auth_members am")
                    .join("pg_roles r")
                    .on("r.oid = am.roleid")
                    .join("pg_roles m")
                    .on("m.oid = am.member")
                    .where(field("r.rolname").eq("MG_ROLE_rmt_grant"))
                    .and(field("m.rolname").eq("MG_USER_" + TEST_USER)));
    assertTrue(granted, "TEST_USER should be a member of MG_ROLE_rmt_grant");
  }

  @Test
  void revokeMembership() {
    roleManager.createRole("rmt_revoke", "for revoke test");
    roleManager.grantRoleToUser("rmt_revoke", TEST_USER);
    roleManager.revokeRoleFromUser("rmt_revoke", TEST_USER);

    boolean stillGranted =
        database
            .getJooq()
            .fetchExists(
                database
                    .getJooq()
                    .select()
                    .from("pg_auth_members am")
                    .join("pg_roles r")
                    .on("r.oid = am.roleid")
                    .join("pg_roles m")
                    .on("m.oid = am.member")
                    .where(field("r.rolname").eq("MG_ROLE_rmt_revoke"))
                    .and(field("m.rolname").eq("MG_USER_" + TEST_USER)));
    assertFalse(stillGranted, "TEST_USER should no longer be a member of MG_ROLE_rmt_revoke");
  }

  @Test
  void deleteCascadesPoliciesAndMembers() {
    roleManager.createRole("rmt_delete", "for delete test");

    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_del_schema";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schemaName + "\".rmt_test_table (id int PRIMARY KEY)");
      jooq.execute("ALTER TABLE \"" + schemaName + "\".rmt_test_table ENABLE ROW LEVEL SECURITY");
      jooq.execute(
          "CREATE POLICY \"MG_P_rmt_delete_SELECT_ALL\" ON \""
              + schemaName
              + "\".rmt_test_table TO \"MG_ROLE_rmt_delete\" USING (true)");

      roleManager.deleteRole("rmt_delete");

      boolean policyGone =
          !jooq.fetchExists(
              jooq.select()
                  .from("pg_policies")
                  .where(field("policyname").eq("MG_P_rmt_delete_SELECT_ALL"))
                  .and(field("schemaname").eq(schemaName)));
      assertTrue(policyGone, "Policy should be dropped after deleteRole");

      boolean pgRoleGone =
          !jooq.fetchExists(
              jooq.select().from("pg_roles").where(field("rolname").eq("MG_ROLE_rmt_delete")));
      assertTrue(pgRoleGone, "PG role should be gone after deleteRole");

      Role tombstone = MetadataUtils.getRole(jooq, "rmt_delete");
      assertNotNull(tombstone, "role_metadata row should still exist as tombstone");
      assertEquals("deleted", tombstone.getStatus());
      assertNotNull(tombstone.getDeletedOn());
    } finally {
      database.becomeAdmin();
      database.getJooq().execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    }
  }

  @Test
  void nonAdminRejected() {
    database.becomeAdmin();
    database.addUser("rmt_nonadmin_user");
    try {
      database.setActiveUser("rmt_nonadmin_user");
      assertThrows(MolgenisException.class, () -> roleManager.createRole("rmt_nonadmin_role", "x"));
    } finally {
      database.becomeAdmin();
      database.removeUser("rmt_nonadmin_user");
    }
  }

  @Test
  void createRejectsTombstonedName() {
    roleManager.createRole("rmt_beta", "first");
    roleManager.deleteRole("rmt_beta");

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> roleManager.createRole("rmt_beta", "second"));
    assertTrue(
        ex.getMessage().toLowerCase().contains("tombstone")
            || ex.getMessage().toLowerCase().contains("already")
            || ex.getMessage().toLowerCase().contains("exist"),
        "Should reject reuse of tombstoned name");
  }

  @Test
  void listRolesExcludesTombstonesByDefault() {
    roleManager.createRole("rmt_gamma", "desc");
    roleManager.deleteRole("rmt_gamma");

    List<Role> roles = roleManager.listRoles();
    boolean found = roles.stream().anyMatch(r -> "rmt_gamma".equals(r.getRoleName()));
    assertFalse(found, "Tombstoned role should not appear in listRoles()");
  }

  @Test
  void setPermissionsReplaceAll() {
    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_replace_schema";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schemaName + "\".t1 (id int PRIMARY KEY)");
      jooq.execute("ALTER TABLE \"" + schemaName + "\".t1 ENABLE ROW LEVEL SECURITY");
      jooq.execute("CREATE TABLE \"" + schemaName + "\".t2 (id int PRIMARY KEY)");
      jooq.execute("ALTER TABLE \"" + schemaName + "\".t2 ENABLE ROW LEVEL SECURITY");

      roleManager.createRole("rmt_replace_role", "replace test");

      PermissionSet first = new PermissionSet();
      first.put(
          new Permission(
              schemaName,
              "t1",
              Permission.ViewScope.ALL,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              false,
              false));
      roleManager.setPermissions("rmt_replace_role", first);

      PermissionSet second = new PermissionSet();
      second.put(
          new Permission(
              schemaName,
              "t2",
              Permission.ViewScope.ALL,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              false,
              false));
      roleManager.setPermissions("rmt_replace_role", second);

      Permission resolved =
          roleManager.getPermissions("rmt_replace_role").resolveFor(schemaName, "t1");
      assertEquals(
          Permission.ViewScope.NONE,
          resolved.select(),
          "t1 select should be NONE after replace-all with t2");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt_replace_role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt_replace_role\"");
      jooq.execute("DELETE FROM \"MOLGENIS\".role_metadata WHERE role_name = 'rmt_replace_role'");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".permission_attributes WHERE role_name = 'rmt_replace_role'");
    }
  }

  @Test
  void setPermissionsRoundTrip() {
    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_roundtrip_schema";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute(
          "CREATE TABLE \""
              + schemaName
              + "\".t1 (id int PRIMARY KEY, mg_owner text NOT NULL DEFAULT session_user, mg_roles text[] NOT NULL DEFAULT '{}')");
      jooq.execute("ALTER TABLE \"" + schemaName + "\".t1 ENABLE ROW LEVEL SECURITY");
      jooq.execute("ALTER TABLE \"" + schemaName + "\".t1 FORCE ROW LEVEL SECURITY");

      roleManager.createRole("rmt_roundtrip_role", "roundtrip test");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new Permission(
              schemaName,
              "t1",
              Permission.ViewScope.ALL,
              Permission.EditScope.ALL,
              Permission.EditScope.ALL,
              Permission.EditScope.NONE,
              true,
              true));
      roleManager.setPermissions("rmt_roundtrip_role", ps);

      Permission got =
          roleManager.getPermissions("rmt_roundtrip_role").resolveFor(schemaName, "t1");
      assertTrue(got.changeOwner(), "changeOwner should survive round-trip");
      assertTrue(got.share(), "share should survive round-trip");
      assertEquals(Permission.ViewScope.ALL, got.select());
      assertEquals(Permission.EditScope.ALL, got.update());
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt_roundtrip_role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt_roundtrip_role\"");
      jooq.execute("DELETE FROM \"MOLGENIS\".role_metadata WHERE role_name = 'rmt_roundtrip_role'");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".permission_attributes WHERE role_name = 'rmt_roundtrip_role'");
    }
  }

  @Test
  void wildcardExistingAndFuture() {
    DSLContext jooq = database.getJooq();
    String schema1 = "rmt_wc_schema1";
    String schema2 = "rmt_wc_schema2";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schema1 + "\"");
    jooq.execute("CREATE SCHEMA \"" + schema2 + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schema1 + "\".t1 (id int PRIMARY KEY)");
      jooq.execute("CREATE TABLE \"" + schema2 + "\".t1 (id int PRIMARY KEY)");

      roleManager.createRole("rmt_wc_role", "wildcard test");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new Permission(
              "*",
              "*",
              Permission.ViewScope.ALL,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              false,
              false));
      roleManager.setPermissions("rmt_wc_role", ps);

      boolean grantSchema1 =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt_wc_role"))
                          .and(field("table_schema").eq(inline(schema1)))
                          .and(field("table_name").eq(inline("t1")))));
      assertTrue(grantSchema1, "Wildcard *:* should materialise GRANT on schema1.t1");

      boolean grantSchema2 =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt_wc_role"))
                          .and(field("table_schema").eq(inline(schema2)))
                          .and(field("table_name").eq(inline("t1")))));
      assertTrue(grantSchema2, "Wildcard *:* should materialise GRANT on schema2.t1");

      PermissionSet got = roleManager.getPermissions("rmt_wc_role");
      Permission wildcard = got.resolveFor("*", "*");
      assertEquals(
          Permission.ViewScope.ALL,
          wildcard.select(),
          "Wildcard entry should be preserved in getPermissions");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt_wc_role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt_wc_role\"");
      jooq.execute("DELETE FROM \"MOLGENIS\".role_metadata WHERE role_name = 'rmt_wc_role'");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".permission_attributes WHERE role_name = 'rmt_wc_role'");
      jooq.execute("DELETE FROM \"MOLGENIS\".role_wildcards WHERE role_name = 'rmt_wc_role'");
    }
  }

  @Test
  void schemaDropCascades() {
    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_drop_schema";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schemaName + "\".t1 (id int PRIMARY KEY)");

      roleManager.createRole("rmt_drop_role", "drop cascade test");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new Permission(
              schemaName,
              "t1",
              Permission.ViewScope.ALL,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              false,
              false));
      roleManager.setPermissions("rmt_drop_role", ps);

      jooq.execute("DROP SCHEMA \"" + schemaName + "\" CASCADE");

      PermissionSet got = roleManager.getPermissions("rmt_drop_role");
      Permission resolved = got.resolveFor(schemaName, "t1");
      assertEquals(
          Permission.ViewScope.NONE,
          resolved.select(),
          "After schema drop, permissions should appear NONE");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt_drop_role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt_drop_role\"");
      jooq.execute("DELETE FROM \"MOLGENIS\".role_metadata WHERE role_name = 'rmt_drop_role'");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".permission_attributes WHERE role_name = 'rmt_drop_role'");
      jooq.execute("DELETE FROM \"MOLGENIS\".role_wildcards WHERE role_name = 'rmt_drop_role'");
    }
  }

  @Test
  void schemaDropNoError() {
    DSLContext jooq = database.getJooq();
    String schema1 = "rmt_nodrop_schema1";
    String schema2 = "rmt_nodrop_schema2";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schema1 + "\"");
    jooq.execute("CREATE SCHEMA \"" + schema2 + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schema1 + "\".t1 (id int PRIMARY KEY)");
      jooq.execute("CREATE TABLE \"" + schema2 + "\".t1 (id int PRIMARY KEY)");

      roleManager.createRole("rmt_nodrop_role", "no error on drop");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new Permission(
              "*",
              "*",
              Permission.ViewScope.ALL,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              false,
              false));
      roleManager.setPermissions("rmt_nodrop_role", ps);

      jooq.execute("DROP SCHEMA \"" + schema1 + "\" CASCADE");

      assertDoesNotThrow(
          () -> roleManager.getPermissions("rmt_nodrop_role"),
          "getPermissions should not throw after schema with wildcard policies is dropped");

      PermissionSet got = roleManager.getPermissions("rmt_nodrop_role");
      boolean hasSchema2Grant =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt_nodrop_role"))
                          .and(field("table_schema").eq(inline(schema2)))
                          .and(field("table_name").eq(inline("t1")))));
      assertTrue(hasSchema2Grant, "schema2 grant should remain after schema1 is dropped");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt_nodrop_role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt_nodrop_role\"");
      jooq.execute("DELETE FROM \"MOLGENIS\".role_metadata WHERE role_name = 'rmt_nodrop_role'");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".permission_attributes WHERE role_name = 'rmt_nodrop_role'");
      jooq.execute("DELETE FROM \"MOLGENIS\".role_wildcards WHERE role_name = 'rmt_nodrop_role'");
    }
  }

  @Test
  void customRoleOnAnySchema() {
    DSLContext jooq = database.getJooq();
    String schema1 = "rmt_custom_schema1";
    String schema2 = "rmt_custom_schema2";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schema1 + "\"");
    jooq.execute("CREATE SCHEMA \"" + schema2 + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schema1 + "\".t1 (id int PRIMARY KEY)");
      jooq.execute("CREATE TABLE \"" + schema2 + "\".t1 (id int PRIMARY KEY)");

      roleManager.createRole("rmt_custom_role", "custom role test");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new Permission(
              schema1,
              "t1",
              Permission.ViewScope.ALL,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              false,
              false));
      roleManager.setPermissions("rmt_custom_role", ps);

      boolean grantSchema1 =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt_custom_role"))
                          .and(field("table_schema").eq(inline(schema1)))
                          .and(field("table_name").eq(inline("t1")))));
      assertTrue(grantSchema1, "Custom role should have grant on schema1.t1");

      boolean grantSchema2 =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt_custom_role"))
                          .and(field("table_schema").eq(inline(schema2)))
                          .and(field("table_name").eq(inline("t1")))));
      assertFalse(
          grantSchema2, "Custom role should NOT have grant on schema2.t1 (not in permissions)");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt_custom_role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt_custom_role\"");
      jooq.execute("DELETE FROM \"MOLGENIS\".role_metadata WHERE role_name = 'rmt_custom_role'");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".permission_attributes WHERE role_name = 'rmt_custom_role'");
      jooq.execute("DELETE FROM \"MOLGENIS\".role_wildcards WHERE role_name = 'rmt_custom_role'");
    }
  }

  @Test
  void wildcardMaterialisedOnExistingSchemas() {
    DSLContext jooq = database.getJooq();
    String schema1 = "rmt_wm_schema1";
    String schema2 = "rmt_wm_schema2";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schema1 + "\"");
    jooq.execute("CREATE SCHEMA \"" + schema2 + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schema1 + "\".studies (id int PRIMARY KEY)");
      jooq.execute("CREATE TABLE \"" + schema2 + "\".studies (id int PRIMARY KEY)");

      roleManager.createRole("rmt_wm_role", "wildcard materialisation test");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new Permission(
              "*",
              "*",
              Permission.ViewScope.ALL,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              false,
              false));
      roleManager.setPermissions("rmt_wm_role", ps);

      boolean grantOnSchema1 =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt_wm_role"))
                          .and(field("table_schema").eq(inline(schema1)))
                          .and(field("table_name").eq(inline("studies")))
                          .and(field("privilege_type").eq(inline("SELECT")))));
      assertTrue(grantOnSchema1, "Wildcard *:* must materialise SELECT GRANT on schema1.studies");

      boolean grantOnSchema2 =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt_wm_role"))
                          .and(field("table_schema").eq(inline(schema2)))
                          .and(field("table_name").eq(inline("studies")))
                          .and(field("privilege_type").eq(inline("SELECT")))));
      assertTrue(grantOnSchema2, "Wildcard *:* must materialise SELECT GRANT on schema2.studies");

      PermissionSet got = roleManager.getPermissions("rmt_wm_role");
      Permission wildcard = got.resolveFor("*", "*");
      assertEquals(
          Permission.ViewScope.ALL,
          wildcard.select(),
          "Wildcard entry must be present in getPermissions");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt_wm_role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt_wm_role\"");
      jooq.execute("DELETE FROM \"MOLGENIS\".role_metadata WHERE role_name = 'rmt_wm_role'");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".permission_attributes WHERE role_name = 'rmt_wm_role'");
      jooq.execute("DELETE FROM \"MOLGENIS\".role_wildcards WHERE role_name = 'rmt_wm_role'");
    }
  }

  @Test
  void wildcardPersistsViaRoleWildcardsTable() {
    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_wp_schema";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schemaName + "\".data (id int PRIMARY KEY)");

      roleManager.createRole("rmt_wp_role", "wildcard persist test");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new Permission(
              "*",
              "*",
              Permission.ViewScope.ALL,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              false,
              false));
      roleManager.setPermissions("rmt_wp_role", ps);

      int wildcardRows =
          jooq.fetchOne(
                  "SELECT count(*) FROM \"MOLGENIS\".role_wildcards WHERE role_name = 'rmt_wp_role' AND schema_pattern = '*' AND table_pattern = '*'")
              .get(0, Integer.class);
      assertEquals(
          1, wildcardRows, "role_wildcards must have exactly one row for the wildcard permission");

      String selectScope =
          jooq.fetchOne(
                  "SELECT select_scope FROM \"MOLGENIS\".role_wildcards WHERE role_name = 'rmt_wp_role'")
              .get(0, String.class);
      assertEquals("ALL", selectScope, "select_scope must be stored as 'ALL'");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt_wp_role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt_wp_role\"");
      jooq.execute("DELETE FROM \"MOLGENIS\".role_metadata WHERE role_name = 'rmt_wp_role'");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".permission_attributes WHERE role_name = 'rmt_wp_role'");
      jooq.execute("DELETE FROM \"MOLGENIS\".role_wildcards WHERE role_name = 'rmt_wp_role'");
    }
  }

  @Test
  void setPermissionsTransactional() {
    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_tx_schema";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schemaName + "\".t1 (id int PRIMARY KEY)");
      jooq.execute("ALTER TABLE \"" + schemaName + "\".t1 ENABLE ROW LEVEL SECURITY");

      roleManager.createRole("rmt_tx_role", "tx test");

      PermissionSet valid = new PermissionSet();
      valid.put(
          new Permission(
              schemaName,
              "t1",
              Permission.ViewScope.ALL,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              false,
              false));
      roleManager.setPermissions("rmt_tx_role", valid);

      boolean grantExistsBefore =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt_tx_role"))
                          .and(field("table_name").eq(inline("t1")))));
      assertTrue(grantExistsBefore, "Grant for t1 should exist after first setPermissions");

      PermissionSet failingSet = new PermissionSet();
      failingSet.put(
          new Permission(
              schemaName,
              "nonexistent_table",
              Permission.ViewScope.ALL,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              Permission.EditScope.NONE,
              false,
              false));
      assertThrows(Exception.class, () -> roleManager.setPermissions("rmt_tx_role", failingSet));

      boolean grantStillExists =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt_tx_role"))
                          .and(field("table_name").eq(inline("t1")))));
      assertTrue(
          grantStillExists, "Transaction should have rolled back; t1 grant should still exist");

      int attrCount =
          jooq.fetchOne(
                  "SELECT count(*) FROM \"MOLGENIS\".permission_attributes WHERE role_name = 'rmt_tx_role'")
              .get(0, Integer.class);
      assertEquals(1, attrCount, "permission_attributes should still have t1 row after rollback");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt_tx_role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt_tx_role\"");
      jooq.execute("DELETE FROM \"MOLGENIS\".role_metadata WHERE role_name = 'rmt_tx_role'");
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".permission_attributes WHERE role_name = 'rmt_tx_role'");
    }
  }
}
