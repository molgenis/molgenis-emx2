package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.TablePermission;
import org.molgenis.emx2.TablePermission.SelectScope;
import org.molgenis.emx2.TablePermission.UpdateScope;
import org.molgenis.emx2.sql.rls.SqlPermissionExecutor;

class FineGrainedPermissionsIT {

  private static final String SCHEMA = "fgp_catalog";
  private static final String TABLE = "studies";
  private static final String ROLE = "analyst";
  private static final String ALICE = "fgp_alice";

  private static final SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  private SqlRoleManager roleManager;
  private DSLContext jooq;

  @BeforeEach
  void setup() {
    database.becomeAdmin();
    jooq = database.getJooq();
    roleManager = new SqlRoleManager(database);

    jooq.execute("DROP SCHEMA IF EXISTS \"" + SCHEMA + "\" CASCADE");
    jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_" + ROLE + "\"");

    if (!database.hasUser(ALICE)) {
      database.addUser(ALICE);
    }

    roleManager.createRole(ROLE, "analyst role for integration test");
    roleManager.grantRoleToUser(ROLE, ALICE);

    jooq.execute("CREATE SCHEMA \"" + SCHEMA + "\"");
    jooq.execute(
        "CREATE TABLE \""
            + SCHEMA
            + "\".\""
            + TABLE
            + "\" ("
            + "id int PRIMARY KEY, "
            + "title text, "
            + "mg_owner text NOT NULL DEFAULT current_user, "
            + "mg_roles text[] NOT NULL DEFAULT '{}'"
            + ")");
    jooq.execute("ALTER TABLE \"" + SCHEMA + "\".\"" + TABLE + "\" ENABLE ROW LEVEL SECURITY");
    jooq.execute("ALTER TABLE \"" + SCHEMA + "\".\"" + TABLE + "\" FORCE ROW LEVEL SECURITY");
    jooq.execute("GRANT USAGE ON SCHEMA \"" + SCHEMA + "\" TO PUBLIC");
    SqlPermissionExecutor.installGuardTrigger(jooq, SCHEMA, TABLE);
  }

  @AfterEach
  void teardown() {
    database.becomeAdmin();
    jooq.execute("DROP SCHEMA IF EXISTS \"" + SCHEMA + "\" CASCADE");
    try {
      jooq.execute("REVOKE \"MG_ROLE_" + ROLE + "\" FROM \"MG_USER_" + ALICE + "\"");
    } catch (Exception ignored) {
    }
    jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_" + ROLE + "\"");
  }

  @Test
  void fullScenario() {
    String alicePg = "MG_USER_" + ALICE;
    String analystPg = "MG_ROLE_" + ROLE;

    jooq.execute(
        "INSERT INTO \""
            + SCHEMA
            + "\".\""
            + TABLE
            + "\" (id, title, mg_owner, mg_roles) VALUES "
            + "(1, 'alice-own', '"
            + alicePg
            + "', ARRAY['"
            + ROLE
            + "']), "
            + "(2, 'alice-noshare', '"
            + alicePg
            + "', '{}'), "
            + "(3, 'foreign-shared', 'MG_USER_someone_else', ARRAY['"
            + ROLE
            + "'])");

    PermissionSet ps = new PermissionSet();
    ps.put(
        new TablePermission(
            SCHEMA,
            TABLE,
            TablePermission.singletonSelect(SelectScope.GROUP),
            UpdateScope.OWN,
            UpdateScope.OWN,
            UpdateScope.NONE,
            false,
            false));
    roleManager.setPermissions(ROLE, ps);

    database.setActiveUser(ALICE);

    int visibleCount =
        jooq.fetchOne("SELECT count(*) FROM \"" + SCHEMA + "\".\"" + TABLE + "\"")
            .get(0, Integer.class);
    assertEquals(
        2,
        visibleCount,
        "GROUP select should show rows where mg_roles overlaps current_user_roles: rows 1 and 3");

    assertDoesNotThrow(
        () ->
            jooq.execute(
                "UPDATE \"" + SCHEMA + "\".\"" + TABLE + "\" SET title = 'updated' WHERE id = 1"),
        "Alice should be able to update her own row");

    int foreignUpdated =
        jooq.execute(
            "UPDATE \"" + SCHEMA + "\".\"" + TABLE + "\" SET title = 'hacked' WHERE id = 3");
    assertEquals(0, foreignUpdated, "Alice cannot update row 3 (mg_owner != alice, OWN scope)");

    assertDoesNotThrow(
        () ->
            jooq.execute(
                "INSERT INTO \""
                    + SCHEMA
                    + "\".\""
                    + TABLE
                    + "\" (id, title, mg_owner) VALUES (10, 'new-alice', '"
                    + alicePg
                    + "')"),
        "Alice can insert a row she owns (OWN insert)");

    assertThrows(
        Exception.class,
        () ->
            jooq.execute(
                "INSERT INTO \""
                    + SCHEMA
                    + "\".\""
                    + TABLE
                    + "\" (id, title, mg_owner) VALUES (11, 'hijack', 'MG_USER_bob')"),
        "OWN insert with foreign owner must be rejected");

    assertThrows(
        Exception.class,
        () ->
            jooq.execute(
                "UPDATE \""
                    + SCHEMA
                    + "\".\""
                    + TABLE
                    + "\" SET mg_owner = 'MG_USER_bob' WHERE id = 1"),
        "Changing mg_owner without change_owner permission must be rejected");

    database.becomeAdmin();

    PermissionSet none = new PermissionSet();
    none.put(
        new TablePermission(
            SCHEMA,
            TABLE,
            TablePermission.emptySelect(),
            UpdateScope.NONE,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false));
    roleManager.setPermissions(ROLE, none);

    database.setActiveUser(ALICE);

    boolean accessDenied = false;
    int afterReset = 0;
    try {
      afterReset =
          jooq.fetchOne("SELECT count(*) FROM \"" + SCHEMA + "\".\"" + TABLE + "\"")
              .get(0, Integer.class);
    } catch (Exception ex) {
      accessDenied = true;
    }
    assertTrue(
        accessDenied || afterReset == 0,
        "After clearing permissions, alice should see 0 rows or get permission denied");

    database.becomeAdmin();

    int adminCount =
        jooq.fetchOne("SELECT count(*) FROM \"" + SCHEMA + "\".\"" + TABLE + "\"")
            .get(0, Integer.class);
    assertEquals(
        4, adminCount, "Admin (BYPASSRLS) must see all 4 rows (original 3 + alice insert)");
  }
}
