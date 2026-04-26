package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Constants.MG_ROLE_PREFIX;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.Role;
import org.molgenis.emx2.TablePermission;

class SqlRoleManagerTest {

  private static final String TEST_USER = "rmt_test_user";
  private static final String THIRTY_TWO_CHAR_NAME = "a".repeat(32);

  private static final SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();

  private SqlRoleManager roleManager;

  @BeforeEach
  void setup() {
    database.becomeAdmin();
    roleManager = new SqlRoleManager(database);
    cleanupTestPgRoles();
    ensureTestUserExists();
  }

  @AfterEach
  void teardown() {
    database.becomeAdmin();
    cleanupTestPgRoles();
  }

  private void cleanupTestPgRoles() {
    DSLContext jooq = database.getJooq();
    List<String> toClean =
        jooq.fetch(
                "SELECT rolname FROM pg_roles WHERE rolname LIKE 'MG_ROLE_rmt_%' OR rolname LIKE 'MG_ROLE_rmt %' OR rolname = {0}",
                org.jooq.impl.DSL.inline("MG_ROLE_" + THIRTY_TWO_CHAR_NAME))
            .map(r -> r.get(0, String.class));
    for (String rolName : toClean) {
      try {
        jooq.execute("DROP OWNED BY {0}", name(rolName));
      } catch (Exception ignored) {
        // best effort
      }
      try {
        jooq.execute("DROP ROLE IF EXISTS {0}", name(rolName));
      } catch (Exception ignored) {
        // best effort
      }
    }
  }

  private void dropRoleIfExists(String name) {
    try {
      database.getJooq().execute("DROP ROLE IF EXISTS {0}", name("MG_ROLE_" + name));
    } catch (Exception ignored) {
      // best effort cleanup
    }
  }

  @Test
  void createRoleRejectsNamesLongerThan32Chars() {
    String name = "a".repeat(33);
    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> roleManager.createRole(name, "too long"));
    assertTrue(
        ex.getMessage().contains("32"),
        "Exception must mention the 32-char limit, got: " + ex.getMessage());
  }

  @Test
  void createRoleAcceptsNamesUpTo32Chars() {
    String name = "rmt " + "a".repeat(28);
    try {
      assertDoesNotThrow(() -> roleManager.createRole(name, "exactly 32 chars"));
    } finally {
      dropRoleIfExists(name);
    }
  }

  @Test
  void createRoleRejectsUnderscore() {
    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () -> roleManager.createRole("data_manager", "has underscore"));
    assertTrue(
        ex.getMessage().toLowerCase().contains("invalid")
            || ex.getMessage().toLowerCase().contains("character")
            || ex.getMessage().toLowerCase().contains("allowed"),
        "Exception must mention invalid characters, got: " + ex.getMessage());
  }

  @Test
  void createRoleRejectsLeadingSpace() {
    MolgenisException ex =
        assertThrows(
            MolgenisException.class, () -> roleManager.createRole(" manager", "leading space"));
    assertTrue(
        ex.getMessage().toLowerCase().contains("space")
            || ex.getMessage().toLowerCase().contains("leading")
            || ex.getMessage().toLowerCase().contains("invalid"),
        "Exception must mention leading/trailing space or invalid name, got: " + ex.getMessage());
  }

  @Test
  void createRoleRejectsTrailingSpace() {
    MolgenisException ex =
        assertThrows(
            MolgenisException.class, () -> roleManager.createRole("manager ", "trailing space"));
    assertTrue(
        ex.getMessage().toLowerCase().contains("space")
            || ex.getMessage().toLowerCase().contains("trailing")
            || ex.getMessage().toLowerCase().contains("invalid"),
        "Exception must mention leading/trailing space or invalid name, got: " + ex.getMessage());
  }

  @Test
  void createRoleRejectsEmptyName() {
    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> roleManager.createRole("", "empty"));
    assertTrue(
        ex.getMessage().toLowerCase().contains("empty")
            || ex.getMessage().toLowerCase().contains("blank")
            || ex.getMessage().toLowerCase().contains("invalid"),
        "Exception must mention empty/blank/invalid name, got: " + ex.getMessage());
  }

  @Test
  void createRoleRejectsSpecialChars() {
    MolgenisException ex =
        assertThrows(
            MolgenisException.class, () -> roleManager.createRole("data-manager", "has hyphen"));
    assertTrue(
        ex.getMessage().toLowerCase().contains("invalid")
            || ex.getMessage().toLowerCase().contains("character")
            || ex.getMessage().toLowerCase().contains("allowed"),
        "Exception must mention invalid characters, got: " + ex.getMessage());
  }

  @Test
  void createRoleAcceptsLettersDigitsAndSpace() {
    String name = "rmt Data Manager 1";
    try {
      assertDoesNotThrow(() -> roleManager.createRole(name, "valid charset"));
    } finally {
      dropRoleIfExists(name);
    }
  }

  private void ensureTestUserExists() {
    if (!database.hasUser(TEST_USER)) {
      database.addUser(TEST_USER);
    }
  }

  @Test
  void roleNameLengthCap() {
    assertDoesNotThrow(() -> roleManager.createRole(THIRTY_TWO_CHAR_NAME, "ok"));

    String thirtyThree = "a".repeat(33);
    MolgenisException ex =
        assertThrows(
            MolgenisException.class, () -> roleManager.createRole(thirtyThree, "too long"));
    assertTrue(ex.getMessage().contains("32"));
  }

  @Test
  void rejectBuiltinNameCollision() {
    assertThrows(MolgenisException.class, () -> roleManager.createRole("Viewer", "desc"));
    assertThrows(MolgenisException.class, () -> roleManager.createRole("viewer", "desc"));
    assertThrows(MolgenisException.class, () -> roleManager.createRole("EDITOR", "desc"));
  }

  @Test
  void createPersistsRoleAndPgRole() {
    roleManager.createRole("rmt alpha", "desc");

    boolean pgRoleExists =
        database
            .getJooq()
            .fetchExists(
                database
                    .getJooq()
                    .select()
                    .from("pg_roles")
                    .where(field("rolname").eq(MG_ROLE_PREFIX + "rmt alpha")));
    assertTrue(pgRoleExists, "PG role MG_ROLE_rmt alpha must exist");

    String description =
        database
            .getJooq()
            .fetchOne(
                "SELECT d.description FROM pg_authid a "
                    + "LEFT JOIN pg_shdescription d ON d.objoid = a.oid AND d.classoid = 'pg_authid'::regclass "
                    + "WHERE a.rolname = {0}",
                inline(MG_ROLE_PREFIX + "rmt alpha"))
            .get(0, String.class);
    assertEquals("desc", description, "Description should be stored as role comment");
  }

  @Test
  void immutableBuiltinsRejected() {
    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> roleManager.deleteRole("viewer"));
    assertTrue(
        ex.getMessage().toLowerCase().contains("immutable")
            || ex.getMessage().toLowerCase().contains("built-in"),
        "Exception message should mention immutability");
  }

  @Test
  void grantMembership() {
    roleManager.createRole("rmt grant", "for grant test");
    roleManager.grantRoleToUser("rmt grant", TEST_USER);

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
                    .where(field("r.rolname").eq("MG_ROLE_rmt grant"))
                    .and(field("m.rolname").eq("MG_USER_" + TEST_USER)));
    assertTrue(granted, "TEST_USER should be a member of MG_ROLE_rmt grant");
  }

  @Test
  void revokeMembership() {
    roleManager.createRole("rmt revoke", "for revoke test");
    roleManager.grantRoleToUser("rmt revoke", TEST_USER);
    roleManager.revokeRoleFromUser("rmt revoke", TEST_USER);

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
                    .where(field("r.rolname").eq("MG_ROLE_rmt revoke"))
                    .and(field("m.rolname").eq("MG_USER_" + TEST_USER)));
    assertFalse(stillGranted, "TEST_USER should no longer be a member of MG_ROLE_rmt revoke");
  }

  @Test
  void deleteCascadesPoliciesAndMembers() {
    roleManager.createRole("rmt delete", "for delete test");

    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_del_schema";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schemaName + "\".rmt_test_table (id int PRIMARY KEY)");
      jooq.execute("ALTER TABLE \"" + schemaName + "\".rmt_test_table ENABLE ROW LEVEL SECURITY");
      jooq.execute("ALTER TABLE \"" + schemaName + "\".rmt_test_table FORCE ROW LEVEL SECURITY");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new TablePermission(schemaName, "rmt_test_table")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.AGGREGATE)));
      roleManager.setPermissions("rmt delete", ps);

      roleManager.grantRoleToUser("rmt delete", TEST_USER);

      boolean policyExists =
          jooq.fetchExists(
              jooq.select()
                  .from("pg_policies")
                  .where(
                      field("policyname")
                          .like("MG_P_rmt delete_%")
                          .and(field("schemaname").eq(schemaName))));
      assertTrue(
          policyExists, "At least one MG_P_rmt delete_% policy must exist before deleteRole");

      boolean memberExists =
          jooq.fetchExists(
              jooq.select()
                  .from("pg_auth_members am")
                  .join("pg_roles r")
                  .on("r.oid = am.roleid")
                  .join("pg_roles m")
                  .on("m.oid = am.member")
                  .where(field("r.rolname").eq("MG_ROLE_rmt delete"))
                  .and(field("m.rolname").eq("MG_USER_" + TEST_USER)));
      assertTrue(
          memberExists, "TEST_USER must be a member of MG_ROLE_rmt delete before deleteRole");

      roleManager.deleteRole("rmt delete");

      boolean policyGone =
          !jooq.fetchExists(
              jooq.select()
                  .from("pg_policies")
                  .where(
                      field("policyname")
                          .like("MG_P_rmt delete_%")
                          .and(field("schemaname").eq(schemaName))));
      assertTrue(policyGone, "All MG_P_rmt delete_% policies should be dropped after deleteRole");

      boolean memberGone =
          !jooq.fetchExists(
              jooq.select()
                  .from("pg_auth_members am")
                  .join("pg_roles r")
                  .on("r.oid = am.roleid")
                  .join("pg_roles m")
                  .on("m.oid = am.member")
                  .where(field("r.rolname").eq("MG_ROLE_rmt delete"))
                  .and(field("m.rolname").eq("MG_USER_" + TEST_USER)));
      assertTrue(memberGone, "Role membership should be removed after deleteRole");

      boolean pgRoleGone =
          !jooq.fetchExists(
              jooq.select().from("pg_roles").where(field("rolname").eq("MG_ROLE_rmt delete")));
      assertTrue(pgRoleGone, "PG role should be gone after deleteRole");
    } finally {
      database.becomeAdmin();
      database.getJooq().execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    }
  }

  @Test
  void deleteAllowsNameReuse() {
    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_reuse_schema";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schemaName + "\".t1 (id int PRIMARY KEY)");
      jooq.execute("ALTER TABLE \"" + schemaName + "\".t1 ENABLE ROW LEVEL SECURITY");
      jooq.execute("ALTER TABLE \"" + schemaName + "\".t1 FORCE ROW LEVEL SECURITY");

      roleManager.createRole("rmt reuse", "first");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new TablePermission(schemaName, "t1")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.AGGREGATE)));
      roleManager.setPermissions("rmt reuse", ps);

      roleManager.grantRoleToUser("rmt reuse", TEST_USER);

      roleManager.deleteRole("rmt reuse");

      assertDoesNotThrow(
          () -> roleManager.createRole("rmt reuse", "second"),
          "Name reuse should be allowed after role is dropped");

      boolean noPolicies =
          !jooq.fetchExists(
              jooq.select()
                  .from("pg_policies")
                  .where(
                      field("policyname")
                          .like("MG_P_rmt reuse_%")
                          .and(field("schemaname").eq(schemaName))));
      assertTrue(noPolicies, "No leftover MG_P_rmt reuse_% policies after delete+recreate");

      boolean noMembers =
          !jooq.fetchExists(
              jooq.select()
                  .from("pg_auth_members am")
                  .join("pg_roles r")
                  .on("r.oid = am.roleid")
                  .where(field("r.rolname").eq("MG_ROLE_rmt reuse"))
                  .and(field("am.member").ne(field("am.roleid"))));
      assertTrue(
          noMembers,
          "No leftover pg_auth_members grants from prior incarnation after delete+recreate");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt reuse\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt reuse\"");
    }
  }

  @Test
  void listRolesReflectsCreatedRole() {
    roleManager.createRole("rmt list", "listed role");

    List<Role> roles = roleManager.listRoles();
    boolean found = roles.stream().anyMatch(r -> "rmt list".equals(r.getRoleName()));
    assertTrue(found, "Created role should appear in listRoles()");
  }

  @Test
  void listRolesExcludesDroppedRole() {
    roleManager.createRole("rmt dropped", "desc");
    roleManager.deleteRole("rmt dropped");

    List<Role> roles = roleManager.listRoles();
    boolean found = roles.stream().anyMatch(r -> "rmt dropped".equals(r.getRoleName()));
    assertFalse(found, "Dropped role should not appear in listRoles()");
  }

  @Test
  void nonAdminRejected() {
    database.becomeAdmin();
    database.addUser("rmt_nonadmin_user");
    try {
      database.setActiveUser("rmt_nonadmin_user");
      assertThrows(MolgenisException.class, () -> roleManager.createRole("rmt nonadmin role", "x"));
    } finally {
      database.becomeAdmin();
      database.removeUser("rmt_nonadmin_user");
    }
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

      roleManager.createRole("rmt replace role", "replace test");

      PermissionSet first = new PermissionSet();
      first.put(
          new TablePermission(schemaName, "t1")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      roleManager.setPermissions("rmt replace role", first);

      PermissionSet second = new PermissionSet();
      second.put(
          new TablePermission(schemaName, "t2")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      roleManager.setPermissions("rmt replace role", second);

      TablePermission resolved =
          roleManager.getPermissions("rmt replace role").resolveFor(schemaName, "t1");
      assertTrue(resolved.select().isEmpty(), "t1 select should be NONE after replace-all with t2");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt replace role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt replace role\"");
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

      roleManager.createRole("rmt roundtrip role", "roundtrip test");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new TablePermission(schemaName, "t1")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL))
              .insert(TablePermission.UpdateScope.ALL)
              .update(TablePermission.UpdateScope.ALL)
              .setChangeOwner(true)
              .setChangeGroup(true));
      roleManager.setPermissions("rmt roundtrip role", ps);

      TablePermission got =
          roleManager.getPermissions("rmt roundtrip role").resolveFor(schemaName, "t1");
      assertTrue(got.changeOwner(), "changeOwner should survive round-trip");
      assertTrue(got.changeGroup(), "changeGroup should survive round-trip");
      assertTrue(
          got.select().contains(TablePermission.SelectScope.ALL),
          "select must contain ALL after round-trip");
      assertEquals(TablePermission.UpdateScope.ALL, got.update());
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt roundtrip role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt roundtrip role\"");
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

      roleManager.createRole("rmt wc role", "wildcard test");

      PermissionSet ps = new PermissionSet();
      ps.put(new TablePermission("*", "*").select(TablePermission.SelectScope.ALL));
      roleManager.setPermissions("rmt wc role", ps);

      boolean grantSchema1 =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt wc role"))
                          .and(field("table_schema").eq(inline(schema1)))
                          .and(field("table_name").eq(inline("t1")))));
      assertTrue(grantSchema1, "Wildcard *:* should materialise GRANT on schema1.t1");

      boolean grantSchema2 =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt wc role"))
                          .and(field("table_schema").eq(inline(schema2)))
                          .and(field("table_name").eq(inline("t1")))));
      assertTrue(grantSchema2, "Wildcard *:* should materialise GRANT on schema2.t1");

      PermissionSet got = roleManager.getPermissions("rmt wc role");
      TablePermission schema1Perm = got.resolveFor(schema1, "t1");
      assertTrue(
          schema1Perm.select().contains(TablePermission.SelectScope.ALL),
          "Wildcard materialised grant should be visible for schema1.t1");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt wc role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt wc role\"");
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

      roleManager.createRole("rmt drop role", "drop cascade test");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new TablePermission(schemaName, "t1")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      roleManager.setPermissions("rmt drop role", ps);

      jooq.execute("DROP SCHEMA \"" + schemaName + "\" CASCADE");

      PermissionSet got = roleManager.getPermissions("rmt drop role");
      TablePermission resolved = got.resolveFor(schemaName, "t1");
      assertTrue(resolved.select().isEmpty(), "After schema drop, permissions should appear NONE");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt drop role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt drop role\"");
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

      roleManager.createRole("rmt nodrop role", "no error on drop");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new TablePermission("*", "*")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      roleManager.setPermissions("rmt nodrop role", ps);

      jooq.execute("DROP SCHEMA \"" + schema1 + "\" CASCADE");

      assertDoesNotThrow(
          () -> roleManager.getPermissions("rmt nodrop role"),
          "getPermissions should not throw after schema with wildcard policies is dropped");

      boolean hasSchema2Grant =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt nodrop role"))
                          .and(field("table_schema").eq(inline(schema2)))
                          .and(field("table_name").eq(inline("t1")))));
      assertTrue(hasSchema2Grant, "schema2 grant should remain after schema1 is dropped");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt nodrop role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt nodrop role\"");
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

      roleManager.createRole("rmt custom role", "custom role test");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new TablePermission(schema1, "t1")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      roleManager.setPermissions("rmt custom role", ps);

      boolean grantSchema1 =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt custom role"))
                          .and(field("table_schema").eq(inline(schema1)))
                          .and(field("table_name").eq(inline("t1")))));
      assertTrue(grantSchema1, "Custom role should have grant on schema1.t1");

      boolean grantSchema2 =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt custom role"))
                          .and(field("table_schema").eq(inline(schema2)))
                          .and(field("table_name").eq(inline("t1")))));
      assertFalse(
          grantSchema2, "Custom role should NOT have grant on schema2.t1 (not in permissions)");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt custom role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt custom role\"");
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

      roleManager.createRole("rmt wm role", "wildcard materialisation test");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new TablePermission("*", "*")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      roleManager.setPermissions("rmt wm role", ps);

      boolean grantOnSchema1 =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt wm role"))
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
                          .eq(inline("MG_ROLE_rmt wm role"))
                          .and(field("table_schema").eq(inline(schema2)))
                          .and(field("table_name").eq(inline("studies")))
                          .and(field("privilege_type").eq(inline("SELECT")))));
      assertTrue(grantOnSchema2, "Wildcard *:* must materialise SELECT GRANT on schema2.studies");

      PermissionSet got = roleManager.getPermissions("rmt wm role");
      TablePermission schema1Perm = got.resolveFor(schema1, "studies");
      assertTrue(
          schema1Perm.select().contains(TablePermission.SelectScope.ALL),
          "Materialised grant must be present in getPermissions for schema1");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt wm role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt wm role\"");
    }
  }

  @Test
  void setPermissionsPersistsSelectAggregate() {
    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_ss_schema";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schemaName + "\".t1 (id int PRIMARY KEY)");

      roleManager.createRole("rmt ss role", "select persist test");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new TablePermission(schemaName, "t1")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.AGGREGATE)));
      roleManager.setPermissions("rmt ss role", ps);

      boolean selectPolicyExists =
          jooq.fetchExists(
              jooq.select()
                  .from("pg_policies")
                  .where(
                      field("schemaname")
                          .eq(inline(schemaName))
                          .and(field("tablename").eq(inline("t1")))
                          .and(
                              field("policyname")
                                  .eq(inline("MG_P_rmt ss role_SELECT_AGGREGATE")))));
      assertTrue(
          selectPolicyExists,
          "MG_P_rmt ss role_SELECT_AGGREGATE policy must exist in pg_policies after setPermissions with select=AGGREGATE");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt ss role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt ss role\"");
    }
  }

  @Test
  void setPermissionsPersistsSelectAll() {
    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_ssa_schema";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schemaName + "\".t1 (id int PRIMARY KEY)");

      roleManager.createRole("rmt ssa role", "select all persist test");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new TablePermission(schemaName, "t1")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      roleManager.setPermissions("rmt ssa role", ps);

      boolean selectAllPolicyExists =
          jooq.fetchExists(
              jooq.select()
                  .from("pg_policies")
                  .where(
                      field("schemaname")
                          .eq(inline(schemaName))
                          .and(field("tablename").eq(inline("t1")))
                          .and(field("policyname").eq(inline("MG_P_rmt ssa role_SELECT_ALL")))));
      assertTrue(
          selectAllPolicyExists,
          "MG_P_rmt ssa role_SELECT_ALL policy must exist in pg_policies after setPermissions with select=ALL");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt ssa role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt ssa role\"");
    }
  }

  @Test
  void getPermissionsReadsSelectAggregate() {
    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_gss_schema";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schemaName + "\".t1 (id int PRIMARY KEY)");
      jooq.execute("ALTER TABLE \"" + schemaName + "\".t1 ENABLE ROW LEVEL SECURITY");

      roleManager.createRole("rmt gss role", "select read test");

      jooq.execute(
          "CREATE POLICY \"MG_P_rmt gss role_SELECT_AGGREGATE\" ON \""
              + schemaName
              + "\".t1 AS PERMISSIVE FOR SELECT TO \"MG_ROLE_rmt gss role\" USING (false)");

      TablePermission got = roleManager.getPermissions("rmt gss role").resolveFor(schemaName, "t1");
      assertTrue(
          got.select().contains(TablePermission.SelectScope.AGGREGATE),
          "getPermissions must hydrate SELECT_AGGREGATE policy into select=AGGREGATE");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt gss role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt gss role\"");
    }
  }

  @ParameterizedTest
  @EnumSource(
      value = TablePermission.SelectScope.class,
      names = {"EXISTS", "COUNT", "RANGE"})
  void selectViewModeRoundTrip(TablePermission.SelectScope selectValue) {
    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_svm_schema";
    String roleName = "rmt svm " + selectValue.name().toLowerCase();
    String pgRoleName = "MG_ROLE_" + roleName;
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schemaName + "\".t1 (id int PRIMARY KEY)");

      roleManager.createRole(roleName, "view mode " + selectValue.name());

      PermissionSet ps = new PermissionSet();
      ps.put(
          new TablePermission(schemaName, "t1")
              .select(TablePermission.singletonSelect(selectValue)));
      roleManager.setPermissions(roleName, ps);

      String expectedPolicy = "MG_P_" + roleName + "_SELECT_" + selectValue.name();
      boolean policyExists =
          jooq.fetchExists(
              jooq.select()
                  .from("pg_policies")
                  .where(
                      field("schemaname")
                          .eq(inline(schemaName))
                          .and(field("tablename").eq(inline("t1")))
                          .and(field("policyname").eq(inline(expectedPolicy)))));
      assertTrue(policyExists, expectedPolicy + " must exist in pg_policies after setPermissions");

      TablePermission got = roleManager.getPermissions(roleName).resolveFor(schemaName, "t1");
      assertTrue(
          got.select().contains(selectValue),
          "getPermissions must round-trip select=" + selectValue.name());
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"" + pgRoleName + "\"");
      jooq.execute("DROP ROLE IF EXISTS \"" + pgRoleName + "\"");
    }
  }

  @Test
  void getPermissionsForActiveUserReflectsSelect() {
    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_gavm_schema";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schemaName + "\".t1 (id int PRIMARY KEY)");
      jooq.execute("ALTER TABLE \"" + schemaName + "\".t1 ENABLE ROW LEVEL SECURITY");

      roleManager.createRole("rmt gavm role", "select active-user test");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new TablePermission(schemaName, "t1")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.AGGREGATE)));
      roleManager.setPermissions("rmt gavm role", ps);
      roleManager.grantRoleToUser("rmt gavm role", TEST_USER);

      database.setActiveUser(TEST_USER);
      PermissionSet userPerms = roleManager.getPermissionsForActiveUser();
      database.becomeAdmin();

      TablePermission resolved = userPerms.resolveFor(schemaName, "t1");
      assertTrue(
          resolved.select().contains(TablePermission.SelectScope.AGGREGATE),
          "getPermissionsForActiveUser must expose select=AGGREGATE for the granted role");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      roleManager.revokeRoleFromUser("rmt gavm role", TEST_USER);
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt gavm role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt gavm role\"");
    }
  }

  @Test
  void getPermissionsForActiveUserMergesSelectMostPermissiveWins() {
    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_gvmm_schema";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schemaName + "\".t1 (id int PRIMARY KEY)");
      jooq.execute("ALTER TABLE \"" + schemaName + "\".t1 ENABLE ROW LEVEL SECURITY");

      roleManager.createRole("rmt gvmm role agg", "aggregate role");
      roleManager.createRole("rmt gvmm role all", "all role");

      PermissionSet psAgg = new PermissionSet();
      psAgg.put(
          new TablePermission(schemaName, "t1")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.AGGREGATE)));
      roleManager.setPermissions("rmt gvmm role agg", psAgg);

      PermissionSet psAll = new PermissionSet();
      psAll.put(
          new TablePermission(schemaName, "t1")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      roleManager.setPermissions("rmt gvmm role all", psAll);

      roleManager.grantRoleToUser("rmt gvmm role agg", TEST_USER);
      roleManager.grantRoleToUser("rmt gvmm role all", TEST_USER);

      database.setActiveUser(TEST_USER);
      PermissionSet userPerms = roleManager.getPermissionsForActiveUser();
      database.becomeAdmin();

      TablePermission resolved = userPerms.resolveFor(schemaName, "t1");
      assertTrue(
          resolved.select().contains(TablePermission.SelectScope.ALL),
          "Both AGGREGATE and ALL must be in union set when user holds both roles");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      roleManager.revokeRoleFromUser("rmt gvmm role agg", TEST_USER);
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt gvmm role agg\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt gvmm role agg\"");
      roleManager.revokeRoleFromUser("rmt gvmm role all", TEST_USER);
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt gvmm role all\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt gvmm role all\"");
    }
  }

  @Test
  void setPermissionsDiffPatchOnlyTouchesChanged() {
    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_dp_schema";
    String tableA = "table_a";
    String tableB = "table_b";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schemaName + "\".\"" + tableA + "\" (id int PRIMARY KEY)");
      jooq.execute(
          "ALTER TABLE \"" + schemaName + "\".\"" + tableA + "\" ENABLE ROW LEVEL SECURITY");
      jooq.execute("CREATE TABLE \"" + schemaName + "\".\"" + tableB + "\" (id int PRIMARY KEY)");
      jooq.execute(
          "ALTER TABLE \"" + schemaName + "\".\"" + tableB + "\" ENABLE ROW LEVEL SECURITY");

      roleManager.createRole("rmt dp role", "diff-patch test");

      PermissionSet first = new PermissionSet();
      first.put(
          new TablePermission(schemaName, tableA)
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      first.put(
          new TablePermission(schemaName, tableB)
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      roleManager.setPermissions("rmt dp role", first);

      Map<String, Long> tableAOidsBefore = policyOids(jooq, schemaName, tableA, "rmt dp role");
      assertFalse(
          tableAOidsBefore.isEmpty(),
          "table_a must have at least one policy after first setPermissions");

      PermissionSet second = new PermissionSet();
      second.put(
          new TablePermission(schemaName, tableA)
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      second.put(
          new TablePermission(schemaName, tableB)
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.AGGREGATE)));
      roleManager.setPermissions("rmt dp role", second);

      Map<String, Long> tableAOidsAfter = policyOids(jooq, schemaName, tableA, "rmt dp role");

      assertEquals(
          tableAOidsBefore,
          tableAOidsAfter,
          "table_a policy OIDs must be unchanged when only table_b's permission changed"
              + " — diff-and-patch must not DROP/CREATE policies for unchanged (schema, table) keys."
              + " OIDs before: "
              + tableAOidsBefore
              + ", after: "
              + tableAOidsAfter);
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt dp role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt dp role\"");
    }
  }

  private Map<String, Long> policyOids(
      DSLContext jooq, String schemaName, String tableName, String roleName) {
    String policyPattern = "MG_P_" + roleName + "_%";
    return jooq
        .fetch(
            "SELECT p.polname, p.oid FROM pg_policy p"
                + " JOIN pg_class c ON c.oid = p.polrelid"
                + " JOIN pg_namespace n ON n.oid = c.relnamespace"
                + " WHERE n.nspname = {0} AND c.relname = {1} AND p.polname LIKE {2}",
            inline(schemaName), inline(tableName), inline(policyPattern))
        .stream()
        .collect(
            Collectors.toMap(r -> r.get("polname", String.class), r -> r.get("oid", Long.class)));
  }

  @Test
  void setPermissionsNoOpForUnchangedWildcard() {
    DSLContext jooq = database.getJooq();
    String schemaName = "rmt_noop_schema";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schemaName + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schemaName + "\".t1 (id int PRIMARY KEY)");
      jooq.execute("ALTER TABLE \"" + schemaName + "\".t1 ENABLE ROW LEVEL SECURITY");
      jooq.execute("CREATE TABLE \"" + schemaName + "\".t2 (id int PRIMARY KEY)");
      jooq.execute("ALTER TABLE \"" + schemaName + "\".t2 ENABLE ROW LEVEL SECURITY");

      roleManager.createRole("rmt noop role", "no-op wildcard test");

      PermissionSet wildcardPs = new PermissionSet();
      wildcardPs.put(
          new TablePermission(schemaName, "*")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      roleManager.setPermissions("rmt noop role", wildcardPs);

      Map<String, Long> oidsBefore = allPolicyOids(jooq, schemaName, "rmt noop role");
      assertFalse(oidsBefore.isEmpty(), "Wildcard must materialise policies before second call");

      roleManager.setPermissions("rmt noop role", wildcardPs);

      Map<String, Long> oidsAfter = allPolicyOids(jooq, schemaName, "rmt noop role");

      assertEquals(
          oidsBefore,
          oidsAfter,
          "No policy OIDs should change when setPermissions is called with the same payload"
              + " — diff-and-patch must emit zero DDL for unchanged entries."
              + " OIDs before: "
              + oidsBefore
              + ", after: "
              + oidsAfter);
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt noop role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt noop role\"");
    }
  }

  private Map<String, Long> allPolicyOids(DSLContext jooq, String schemaName, String roleName) {
    String policyPattern = "MG_P_" + roleName + "_%";
    return jooq
        .fetch(
            "SELECT p.polname || ':' || c.relname AS key, p.oid FROM pg_policy p"
                + " JOIN pg_class c ON c.oid = p.polrelid"
                + " JOIN pg_namespace n ON n.oid = c.relnamespace"
                + " WHERE n.nspname = {0} AND p.polname LIKE {1}",
            inline(schemaName), inline(policyPattern))
        .stream()
        .collect(Collectors.toMap(r -> r.get("key", String.class), r -> r.get("oid", Long.class)));
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

      roleManager.createRole("rmt tx role", "tx test");

      PermissionSet valid = new PermissionSet();
      valid.put(
          new TablePermission(schemaName, "t1")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      roleManager.setPermissions("rmt tx role", valid);

      boolean grantExistsBefore =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt tx role"))
                          .and(field("table_name").eq(inline("t1")))));
      assertTrue(grantExistsBefore, "Grant for t1 should exist after first setPermissions");

      PermissionSet failingSet = new PermissionSet();
      failingSet.put(
          new TablePermission(schemaName, "nonexistent_table")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      assertThrows(Exception.class, () -> roleManager.setPermissions("rmt tx role", failingSet));

      boolean grantStillExists =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt tx role"))
                          .and(field("table_name").eq(inline("t1")))));
      assertTrue(
          grantStillExists, "Transaction should have rolled back; t1 grant should still exist");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt tx role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt tx role\"");
    }
  }
}
