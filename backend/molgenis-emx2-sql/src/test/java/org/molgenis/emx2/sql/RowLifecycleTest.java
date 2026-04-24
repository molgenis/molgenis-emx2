package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.TablePermission;

class RowLifecycleTest {

  private static final String SCHEMA = "rlc_schema";
  private static final String TABLE = "rlc_table";
  private static final String ROLE = "rlc_role";
  private static final String TEST_USER = "rlc_testuser";

  private static final SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  private SqlRoleManager roleManager;
  private DSLContext jooq;

  @BeforeEach
  void setup() {
    database.becomeAdmin();
    jooq = database.getJooq();
    roleManager = new SqlRoleManager(database);

    jooq.execute("DROP SCHEMA IF EXISTS \"" + SCHEMA + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + SCHEMA + "\"");
    jooq.execute(
        "CREATE TABLE \""
            + SCHEMA
            + "\".\""
            + TABLE
            + "\" (id int PRIMARY KEY, payload text"
            + ", mg_owner text NOT NULL DEFAULT current_user"
            + ", mg_roles text[] NOT NULL DEFAULT '{}'"
            + ")");
    jooq.execute("ALTER TABLE \"" + SCHEMA + "\".\"" + TABLE + "\" ENABLE ROW LEVEL SECURITY");
    jooq.execute("ALTER TABLE \"" + SCHEMA + "\".\"" + TABLE + "\" FORCE ROW LEVEL SECURITY");
    jooq.execute("GRANT USAGE ON SCHEMA \"" + SCHEMA + "\" TO PUBLIC");

    org.molgenis.emx2.sql.rls.SqlPermissionExecutor.installGuardTrigger(jooq, SCHEMA, TABLE);

    jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_" + ROLE + "\"");
    jooq.execute("DELETE FROM \"MOLGENIS\".permission_attributes WHERE role_name = '" + ROLE + "'");

    roleManager.createRole(ROLE, "lifecycle test role");

    if (!database.hasUser(TEST_USER)) {
      database.addUser(TEST_USER);
    }
    jooq.execute("GRANT \"MG_ROLE_" + ROLE + "\" TO \"MG_USER_" + TEST_USER + "\"");
  }

  @AfterEach
  void teardown() {
    database.becomeAdmin();
    jooq.execute("DROP SCHEMA IF EXISTS \"" + SCHEMA + "\" CASCADE");
    try {
      jooq.execute("REVOKE \"MG_ROLE_" + ROLE + "\" FROM \"MG_USER_" + TEST_USER + "\"");
    } catch (Exception ignored) {
      // best effort
    }
    jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_" + ROLE + "\"");
    jooq.execute("DELETE FROM \"MOLGENIS\".permission_attributes WHERE role_name = '" + ROLE + "'");
  }

  @Test
  void insertDefaultsOwner() {
    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            SCHEMA,
            TABLE,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(ROLE, ps);

    database.setActiveUser(TEST_USER);
    jooq.execute("INSERT INTO \"" + SCHEMA + "\".\"" + TABLE + "\" (id) VALUES (1)");
    database.becomeAdmin();

    String owner =
        jooq.fetchOne("SELECT mg_owner FROM \"" + SCHEMA + "\".\"" + TABLE + "\" WHERE id = 1")
            .get(0, String.class);
    assertEquals("MG_USER_" + TEST_USER, owner, "mg_owner should default to session_user");
  }

  @Test
  void insertOwnBlocksForeignOwner() {
    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            SCHEMA,
            TABLE,
            TablePermission.Scope.ALL,
            TablePermission.Scope.OWN,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(ROLE, ps);

    database.setActiveUser(TEST_USER);
    assertThrows(
        Exception.class,
        () ->
            jooq.execute(
                "INSERT INTO \""
                    + SCHEMA
                    + "\".\""
                    + TABLE
                    + "\" (id, mg_owner) VALUES (2, 'someoneelse')"),
        "OWN insert with foreign owner should be rejected");
    database.becomeAdmin();
  }

  @Test
  void insertGroupValidatesRoles() {
    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            SCHEMA,
            TABLE,
            TablePermission.Scope.ALL,
            TablePermission.Scope.GROUP,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(ROLE, ps);

    database.setActiveUser(TEST_USER);
    assertDoesNotThrow(
        () ->
            jooq.execute(
                "INSERT INTO \""
                    + SCHEMA
                    + "\".\""
                    + TABLE
                    + "\" (id, mg_roles) VALUES (3, ARRAY['"
                    + ROLE
                    + "'])"),
        "GROUP insert with own role should succeed");

    assertThrows(
        Exception.class,
        () ->
            jooq.execute(
                "INSERT INTO \""
                    + SCHEMA
                    + "\".\""
                    + TABLE
                    + "\" (id, mg_roles) VALUES (4, ARRAY['not_my_role'])"),
        "GROUP insert with role caller does not hold should fail");
    database.becomeAdmin();
  }

  @Test
  void insertAllDefaults() {
    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            SCHEMA,
            TABLE,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.NONE,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(ROLE, ps);

    database.setActiveUser(TEST_USER);
    jooq.execute("INSERT INTO \"" + SCHEMA + "\".\"" + TABLE + "\" (id) VALUES (5)");
    database.becomeAdmin();

    org.jooq.Record row =
        jooq.fetchOne(
            "SELECT mg_owner, mg_roles FROM \"" + SCHEMA + "\".\"" + TABLE + "\" WHERE id = 5");
    assertEquals("MG_USER_" + TEST_USER, row.get("mg_owner", String.class));
    String[] roles = row.get("mg_roles", String[].class);
    assertNotNull(roles);
    assertEquals(0, roles.length, "mg_roles should default to empty array");
  }

  @Test
  void updateWithoutChangeOwnerRejected() {
    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            SCHEMA,
            TABLE,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(ROLE, ps);

    database.becomeAdmin();
    jooq.execute(
        "INSERT INTO \""
            + SCHEMA
            + "\".\""
            + TABLE
            + "\" (id, mg_owner) VALUES (10, 'MG_USER_"
            + TEST_USER
            + "')");

    database.setActiveUser(TEST_USER);
    assertThrows(
        Exception.class,
        () ->
            jooq.execute(
                "UPDATE \"" + SCHEMA + "\".\"" + TABLE + "\" SET mg_owner = 'other' WHERE id = 10"),
        "Should be rejected: changeOwner=false");
    database.becomeAdmin();
  }

  @Test
  void updateWithoutShareRejected() {
    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            SCHEMA,
            TABLE,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.NONE,
            false,
            false));
    roleManager.setPermissions(ROLE, ps);

    database.becomeAdmin();
    jooq.execute(
        "INSERT INTO \""
            + SCHEMA
            + "\".\""
            + TABLE
            + "\" (id, mg_owner) VALUES (11, 'MG_USER_"
            + TEST_USER
            + "')");

    database.setActiveUser(TEST_USER);
    assertThrows(
        Exception.class,
        () ->
            jooq.execute(
                "UPDATE \""
                    + SCHEMA
                    + "\".\""
                    + TABLE
                    + "\" SET mg_roles = ARRAY['"
                    + ROLE
                    + "'] WHERE id = 11"),
        "Should be rejected: share=false");
    database.becomeAdmin();
  }

  @Test
  void changeOwnerAllScope() {
    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            SCHEMA,
            TABLE,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.NONE,
            true,
            false));
    roleManager.setPermissions(ROLE, ps);

    database.becomeAdmin();
    jooq.execute(
        "INSERT INTO \""
            + SCHEMA
            + "\".\""
            + TABLE
            + "\" (id, mg_owner) VALUES (20, 'MG_USER_"
            + TEST_USER
            + "')");

    database.setActiveUser(TEST_USER);
    assertDoesNotThrow(
        () ->
            jooq.execute(
                "UPDATE \"" + SCHEMA + "\".\"" + TABLE + "\" SET mg_owner = 'other' WHERE id = 20"),
        "changeOwner=true with update=ALL should succeed");
    database.becomeAdmin();
  }

  @Test
  void changeOwnerOwnScope() {
    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            SCHEMA,
            TABLE,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.OWN,
            TablePermission.Scope.NONE,
            true,
            false));
    roleManager.setPermissions(ROLE, ps);

    database.setActiveUser(TEST_USER);
    jooq.execute("INSERT INTO \"" + SCHEMA + "\".\"" + TABLE + "\" (id) VALUES (30)");

    assertDoesNotThrow(
        () ->
            jooq.execute(
                "UPDATE \"" + SCHEMA + "\".\"" + TABLE + "\" SET mg_owner = 'other' WHERE id = 30"),
        "changeOwner=true on own row should succeed");

    database.becomeAdmin();
    jooq.execute(
        "INSERT INTO \""
            + SCHEMA
            + "\".\""
            + TABLE
            + "\" (id, mg_owner) VALUES (31, 'someone_else')");

    database.setActiveUser(TEST_USER);
    int updated =
        jooq.execute(
            "UPDATE \"" + SCHEMA + "\".\"" + TABLE + "\" SET mg_owner = 'changed' WHERE id = 31");
    assertEquals(0, updated, "Row owned by someone_else cannot be updated under OWN update scope");
    database.becomeAdmin();
  }

  @Test
  void changeOwnerGroupScope() {
    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            SCHEMA,
            TABLE,
            TablePermission.Scope.GROUP,
            TablePermission.Scope.GROUP,
            TablePermission.Scope.GROUP,
            TablePermission.Scope.NONE,
            true,
            false));
    roleManager.setPermissions(ROLE, ps);

    database.becomeAdmin();
    jooq.execute(
        "INSERT INTO \""
            + SCHEMA
            + "\".\""
            + TABLE
            + "\" (id, mg_owner, mg_roles) VALUES (40, 'MG_USER_"
            + TEST_USER
            + "', ARRAY['"
            + ROLE
            + "'])");

    database.setActiveUser(TEST_USER);
    assertDoesNotThrow(
        () ->
            jooq.execute(
                "UPDATE \""
                    + SCHEMA
                    + "\".\""
                    + TABLE
                    + "\" SET mg_owner = 'new_owner' WHERE id = 40"),
        "changeOwner=true with GROUP scope on group-visible row should succeed");
    database.becomeAdmin();
  }

  @Test
  void shareLimitedToGrantedRoles() {
    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            SCHEMA,
            TABLE,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.ALL,
            TablePermission.Scope.NONE,
            false,
            true));
    roleManager.setPermissions(ROLE, ps);

    database.becomeAdmin();
    jooq.execute(
        "INSERT INTO \""
            + SCHEMA
            + "\".\""
            + TABLE
            + "\" (id, mg_owner) VALUES (50, 'MG_USER_"
            + TEST_USER
            + "')");

    database.setActiveUser(TEST_USER);
    assertThrows(
        Exception.class,
        () ->
            jooq.execute(
                "UPDATE \""
                    + SCHEMA
                    + "\".\""
                    + TABLE
                    + "\" SET mg_roles = ARRAY['someotherrole'] WHERE id = 50"),
        "share should fail when role is not held by caller");
    database.becomeAdmin();
  }
}
