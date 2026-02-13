package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.MG_ROLE_PREFIX;
import static org.molgenis.emx2.Constants.MG_ROWLEVEL;
import static org.molgenis.emx2.TableMetadata.table;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;

@Tag("rowlevel")
public class TestSqlRoleManager {
  private static Database database;

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  private DSLContext jooq(Database db) {
    return ((SqlDatabase) db).getJooq();
  }

  private SqlRoleManager roleManager(Database db) {
    return ((SqlDatabase) db).getRoleManager();
  }

  @Test
  public void testCreateCustomRole() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_createRole");
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "Analysts", false);

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/Analysts";
          Result<Record> result =
              jooq(db).fetch("SELECT 1 FROM pg_roles WHERE rolname = {0}", fullRoleName);
          assertEquals(1, result.size(), "Role should exist in pg_roles");
        });
  }

  @Test
  public void testCreateRowLevelRole() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_rowLevelRole");
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "DataGroup1", true);

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/DataGroup1";
          Result<Record> result =
              jooq(db).fetch("SELECT 1 FROM pg_roles WHERE rolname = {0}", fullRoleName);
          assertEquals(1, result.size(), "Role should exist in pg_roles");

          Boolean isRowLevel =
              jooq(db)
                  .fetchOne("SELECT pg_has_role({0}, {1}, 'member')", fullRoleName, MG_ROWLEVEL)
                  .into(Boolean.class);
          assertTrue(isRowLevel, "Role should be member of MG_ROWLEVEL");
        });
  }

  @Test
  public void testAddRemoveMember() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_addRemoveMember");
          db.addUser("rm_user1");
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "TestRole", false);

          rm.addMember(schema.getName(), "TestRole", "rm_user1");

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/TestRole";
          String fullUserName = "MG_USER_rm_user1";
          Boolean isMember =
              jooq(db)
                  .fetchOne("SELECT pg_has_role({0}, {1}, 'member')", fullUserName, fullRoleName)
                  .into(Boolean.class);
          assertTrue(isMember, "User should be member of role after add");

          rm.removeMember(schema.getName(), "TestRole", "rm_user1");

          Boolean isMemberAfter =
              jooq(db)
                  .fetchOne("SELECT pg_has_role({0}, {1}, 'member')", fullUserName, fullRoleName)
                  .into(Boolean.class);
          assertFalse(isMemberAfter, "User should not be member after remove");
        });
  }

  @Test
  public void testSetTablePermission() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_tablePermission");
          schema.create(table("TestTable").add(column("id").setPkey()));
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "DataViewer", false);

          rm.grantTablePermission(schema.getName(), "DataViewer", "TestTable", "SELECT");

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/DataViewer";
          Boolean hasSelect =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'SELECT')",
                      fullRoleName, "\"" + schema.getName() + "\".\"TestTable\"")
                  .into(Boolean.class);
          assertTrue(hasSelect, "Role should have SELECT on table");

          rm.revokeTablePermission(schema.getName(), "DataViewer", "TestTable", "SELECT");

          Boolean hasSelectAfter =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'SELECT')",
                      fullRoleName, "\"" + schema.getName() + "\".\"TestTable\"")
                  .into(Boolean.class);
          assertFalse(hasSelectAfter, "Role should not have SELECT after revoke");
        });
  }

  @Test
  public void testSystemRoleProtection() {
    database.tx(
        db -> {
          db.dropCreateSchema("TestRM_systemProtection");
          SqlRoleManager rm = roleManager(db);

          assertThrows(
              MolgenisException.class,
              () -> rm.deleteRole("TestRM_systemProtection", "Viewer"),
              "Should not allow deletion of system role Viewer");
          assertThrows(
              MolgenisException.class,
              () -> rm.deleteRole("TestRM_systemProtection", "Editor"),
              "Should not allow deletion of system role Editor");
        });
  }

  @Test
  public void testGlobalRoleCreation() {
    database.tx(
        db -> {
          SqlRoleManager rm = roleManager(db);
          rm.createRole("mg_global", "GlobalAnalyst", false);

          String fullRoleName = MG_ROLE_PREFIX + "mg_global/GlobalAnalyst";
          Result<Record> result =
              jooq(db).fetch("SELECT 1 FROM pg_roles WHERE rolname = {0}", fullRoleName);
          assertEquals(1, result.size(), "Global role should exist in pg_roles");
        });
  }

  @Test
  public void testRoleDescription() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_description");
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "DescribedRole", false);

          String description = "This role is for testing descriptions";
          rm.setDescription(schema.getName(), "DescribedRole", description);

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/DescribedRole";
          String fetched =
              jooq(db)
                  .fetchOne(
                      "SELECT shobj_description(oid, 'pg_authid') FROM pg_authid WHERE rolname = {0}",
                      fullRoleName)
                  .into(String.class);
          assertEquals(description, fetched, "Role description should match");
        });
  }

  @Test
  public void testDuplicateRoleCreation() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_duplicate");
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "DuplicateRole", false);

          assertDoesNotThrow(
              () -> rm.createRole(schema.getName(), "DuplicateRole", false),
              "Creating duplicate role should be idempotent");
        });
  }

  @Test
  public void testDeleteNonExistentRole() {
    database.tx(
        db -> {
          db.dropCreateSchema("TestRM_deleteNonExistent");
          SqlRoleManager rm = roleManager(db);

          assertThrows(
              MolgenisException.class,
              () -> rm.deleteRole("TestRM_deleteNonExistent", "NonExistent"),
              "Should throw when deleting non-existent role");
        });
  }

  @Test
  public void testGrantToNonExistentTable() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_grantNonExistent");
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "TestRole", false);

          assertThrows(
              MolgenisException.class,
              () ->
                  rm.grantTablePermission(
                      schema.getName(), "TestRole", "NonExistentTable", "SELECT"),
              "Should throw when granting on non-existent table");
        });
  }
}
