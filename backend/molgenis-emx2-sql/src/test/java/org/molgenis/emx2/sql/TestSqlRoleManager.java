package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.MG_ROLE_PREFIX;
import static org.molgenis.emx2.Constants.MG_ROWLEVEL;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Permission;
import org.molgenis.emx2.PermissionLevel;
import org.molgenis.emx2.RoleInfo;
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
  public void testCreateRole() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_createRole");
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "Analysts");

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/Analysts";
          Result<Record> result =
              jooq(db).fetch("SELECT 1 FROM pg_roles WHERE rolname = {0}", fullRoleName);
          assertEquals(1, result.size(), "Role should exist in pg_roles");

          assertTrue(rm.roleExists(schema.getName(), "Analysts"), "roleExists should return true");
        });
  }

  @Test
  public void testAddRemoveMember() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_addRemoveMember");
          db.addUser("rm_user1");
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "TestRole");

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
          rm.createRole(schema.getName(), "DataEditor");

          Permission perm = new Permission();
          perm.setTable("TestTable");
          perm.setInsert(PermissionLevel.TABLE);
          perm.setUpdate(PermissionLevel.TABLE);
          rm.setPermission(schema.getName(), "DataEditor", perm);

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/DataEditor";
          Boolean hasInsert =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'INSERT')",
                      fullRoleName, "\"" + schema.getName() + "\".\"TestTable\"")
                  .into(Boolean.class);
          assertTrue(hasInsert, "Role should have INSERT on table");

          rm.revokePermission(schema.getName(), "DataEditor", "TestTable");

          Boolean hasInsertAfter =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'INSERT')",
                      fullRoleName, "\"" + schema.getName() + "\".\"TestTable\"")
                  .into(Boolean.class);
          assertFalse(hasInsertAfter, "Role should not have INSERT after revoke");
        });
  }

  @Test
  public void testRowLevelPermission() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_rowLevel");
          schema.create(table("People").add(column("id").setPkey()).add(column("name")));
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "RowRestricted");

          Permission perm = new Permission();
          perm.setTable("People");
          perm.setSelect(PermissionLevel.ROW);
          rm.setPermission(schema.getName(), "RowRestricted", perm);

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/RowRestricted";
          Boolean isMgRowLevel =
              jooq(db)
                  .fetchOne("SELECT pg_has_role({0}, {1}, 'member')", fullRoleName, MG_ROWLEVEL)
                  .into(Boolean.class);
          assertTrue(isMgRowLevel, "Row-level role should be member of MG_ROWLEVEL");

          Integer hasColumn =
              jooq(db)
                  .selectCount()
                  .from("information_schema.columns")
                  .where(field("table_schema").eq(inline(schema.getName())))
                  .and(field("table_name").eq(inline("People")))
                  .and(field("column_name").eq(inline("mg_roles")))
                  .fetchOne(0, Integer.class);

          assertTrue(hasColumn != null && hasColumn > 0, "mg_roles column should be added");
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
              () -> rm.createRole("TestRM_systemProtection", "Viewer"),
              "Should not allow creation of system role Viewer");
          assertThrows(
              MolgenisException.class,
              () -> rm.deleteRole("TestRM_systemProtection", "Viewer"),
              "Should not allow deletion of system role Viewer");
        });
  }

  @Test
  public void testRoleDescription() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_description");
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "DescribedRole");

          String description = "This role is for testing descriptions";
          rm.setDescription(schema.getName(), "DescribedRole", description);

          String fetched = rm.getDescription(schema.getName(), "DescribedRole");
          assertEquals(description, fetched, "Role description should match");
        });
  }

  @Test
  public void testDuplicateRoleCreation() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_duplicate");
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "DuplicateRole");

          assertDoesNotThrow(
              () -> rm.createRole(schema.getName(), "DuplicateRole"),
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
          rm.createRole(schema.getName(), "TestRole");

          Permission perm = new Permission();
          perm.setTable("NonExistentTable");
          perm.setSelect(PermissionLevel.TABLE);

          assertThrows(
              MolgenisException.class,
              () -> rm.setPermission(schema.getName(), "TestRole", perm),
              "Should throw when granting on non-existent table");
        });
  }

  @Test
  public void testGetPermissions() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_getPermissions");
          schema.create(table("Table1").add(column("id").setPkey()));
          schema.create(table("Table2").add(column("id").setPkey()));
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "MultiPermRole");

          Permission perm1 = new Permission();
          perm1.setTable("Table1");
          perm1.setSelect(PermissionLevel.ROW);
          perm1.setInsert(PermissionLevel.ROW);
          rm.setPermission(schema.getName(), "MultiPermRole", perm1);

          Permission perm2 = new Permission();
          perm2.setTable("Table2");
          perm2.setSelect(PermissionLevel.ROW);
          rm.setPermission(schema.getName(), "MultiPermRole", perm2);

          List<Permission> permissions = rm.getPermissions(schema.getName(), "MultiPermRole");
          assertEquals(2, permissions.size(), "Should have 2 permissions");

          Permission foundPerm1 =
              permissions.stream()
                  .filter(p -> "Table1".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          assertNotNull(foundPerm1, "Should find Table1 permission");
          assertEquals(PermissionLevel.ROW, foundPerm1.getSelect());
          assertEquals(PermissionLevel.ROW, foundPerm1.getInsert());
          assertTrue(foundPerm1.hasRowLevelPermissions());

          Permission foundPerm2 =
              permissions.stream()
                  .filter(p -> "Table2".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          assertNotNull(foundPerm2, "Should find Table2 permission");
          assertEquals(PermissionLevel.ROW, foundPerm2.getSelect());
          assertTrue(foundPerm2.hasRowLevelPermissions());
        });
  }

  @Test
  public void testGetRoleInfos() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_getRoleInfos");
          schema.create(table("Table1").add(column("id").setPkey()));
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "InfoRole");
          rm.setDescription(schema.getName(), "InfoRole", "Test description");

          Permission perm = new Permission();
          perm.setTable("Table1");
          perm.setSelect(PermissionLevel.TABLE);
          rm.setPermission(schema.getName(), "InfoRole", perm);

          List<RoleInfo> roleInfos = rm.getRoleInfos(schema.getName());
          assertFalse(roleInfos.isEmpty(), "Should have roles");

          RoleInfo infoRole =
              roleInfos.stream()
                  .filter(r -> "InfoRole".equals(r.getName()))
                  .findFirst()
                  .orElse(null);
          assertNotNull(infoRole, "Should find InfoRole");
          assertEquals("Test description", infoRole.getDescription());
          assertFalse(infoRole.isSystem(), "InfoRole is not a system role");
          assertEquals(1, infoRole.getPermissions().size(), "Should have 1 permission");
        });
  }

  @Test
  public void testDeleteRoleWithPermissions() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_deleteWithPerms");
          schema.create(table("Table1").add(column("id").setPkey()));
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "ToDelete");

          Permission perm = new Permission();
          perm.setTable("Table1");
          perm.setSelect(PermissionLevel.TABLE);
          rm.setPermission(schema.getName(), "ToDelete", perm);

          rm.deleteRole(schema.getName(), "ToDelete");

          assertFalse(
              rm.roleExists(schema.getName(), "ToDelete"), "Role should not exist after delete");

          List<Permission> permissions = rm.getPermissions(schema.getName(), "ToDelete");
          assertEquals(0, permissions.size(), "Permissions should be deleted with role");
        });
  }
}
