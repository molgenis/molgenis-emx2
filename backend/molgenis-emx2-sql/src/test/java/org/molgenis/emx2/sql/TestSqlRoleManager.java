package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.MG_ROLE_PREFIX;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.ModifyLevel;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Permission;
import org.molgenis.emx2.RoleInfo;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SelectLevel;

@Tag("rowlevel")
public class TestSqlRoleManager {
  private static Database database;

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @BeforeEach
  public void resetAdmin() {
    database.becomeAdmin();
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

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/DataEditor";
          Boolean hasInsert =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'INSERT')",
                      fullRoleName, "\"" + schema.getName() + "\".\"TestTable\"")
                  .into(Boolean.class);
          assertFalse(hasInsert, "Role should not have INSERT on table");

          Permission perm = new Permission();
          perm.setTable("TestTable");
          perm.setInsert(ModifyLevel.TABLE);
          perm.setUpdate(ModifyLevel.TABLE);
          rm.grant(schema.getName(), "DataEditor", perm);

          hasInsert =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'INSERT')",
                      fullRoleName, "\"" + schema.getName() + "\".\"TestTable\"")
                  .into(Boolean.class);
          assertTrue(hasInsert, "Role should have INSERT on table");

          Permission revokePerm = new Permission();
          revokePerm.setTable("TestTable");
          rm.revoke(schema.getName(), "DataEditor", revokePerm);

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
          perm.setSelect(SelectLevel.ROW);
          rm.grant(schema.getName(), "RowRestricted", perm);

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/RowRestricted";

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
          perm.setSelect(SelectLevel.TABLE);

          assertThrows(
              MolgenisException.class,
              () -> rm.grant(schema.getName(), "TestRole", perm),
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
          perm1.setSelect(SelectLevel.ROW);
          perm1.setInsert(ModifyLevel.ROW);
          rm.grant(schema.getName(), "MultiPermRole", perm1);

          Permission perm2 = new Permission();
          perm2.setTable("Table2");
          perm2.setSelect(SelectLevel.ROW);
          rm.grant(schema.getName(), "MultiPermRole", perm2);

          List<Permission> permissions = rm.getPermissions(schema.getName(), "MultiPermRole");
          assertEquals(2, permissions.size(), "Should have 2 permissions");

          Permission foundPerm1 =
              permissions.stream()
                  .filter(p -> "Table1".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          assertNotNull(foundPerm1, "Should find Table1 permission");
          assertEquals(SelectLevel.ROW, foundPerm1.getSelect());
          assertEquals(ModifyLevel.ROW, foundPerm1.getInsert());
          assertTrue(foundPerm1.hasRowLevelPermissions());

          Permission foundPerm2 =
              permissions.stream()
                  .filter(p -> "Table2".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          assertNotNull(foundPerm2, "Should find Table2 permission");
          assertEquals(SelectLevel.ROW, foundPerm2.getSelect());
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
          perm.setSelect(SelectLevel.TABLE);
          rm.grant(schema.getName(), "InfoRole", perm);

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
          perm.setSelect(SelectLevel.TABLE);
          rm.grant(schema.getName(), "ToDelete", perm);

          rm.deleteRole(schema.getName(), "ToDelete");

          assertFalse(
              rm.roleExists(schema.getName(), "ToDelete"), "Role should not exist after delete");

          List<Permission> permissions = rm.getPermissions(schema.getName(), "ToDelete");
          assertEquals(0, permissions.size(), "Permissions should be deleted with role");
        });
  }

  @Test
  public void testGetRoleInfoRoundTrip() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_roleInfoRT");
          schema.create(table("Patients").add(column("id").setPkey()));
          schema.create(table("Samples").add(column("id").setPkey()));
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "Researcher");
          rm.setDescription(schema.getName(), "Researcher", "Research access");

          Permission p1 = new Permission();
          p1.setTable("Patients");
          p1.setSelect(SelectLevel.ROW);
          p1.setInsert(ModifyLevel.ROW);
          rm.grant(schema.getName(), "Researcher", p1);

          Permission p2 = new Permission();
          p2.setTable("Samples");
          p2.setSelect(SelectLevel.TABLE);
          rm.grant(schema.getName(), "Researcher", p2);

          RoleInfo info = rm.getRoleInfo(schema.getName(), "Researcher");
          assertEquals("Researcher", info.getName());
          assertEquals("Research access", info.getDescription());
          assertFalse(info.isSystem());

          Permission patientPerm =
              info.getPermissions().stream()
                  .filter(p -> "Patients".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          assertNotNull(patientPerm);
          assertEquals(SelectLevel.ROW, patientPerm.getSelect());
          assertEquals(ModifyLevel.ROW, patientPerm.getInsert());

          Permission samplePerm =
              info.getPermissions().stream()
                  .filter(p -> "Samples".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          assertNotNull(samplePerm);
          assertEquals(SelectLevel.TABLE, samplePerm.getSelect());
          assertNull(samplePerm.getInsert());
        });
  }

  @Test
  public void testMergeSemantics() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_merge");
          schema.create(table("Patients").add(column("id").setPkey()));
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "MergeRole");

          Permission first = new Permission();
          first.setTable("Patients");
          first.setSelect(SelectLevel.ROW);
          rm.grant(schema.getName(), "MergeRole", first);

          Permission second = new Permission();
          second.setTable("Patients");
          second.setInsert(ModifyLevel.TABLE);
          rm.grant(schema.getName(), "MergeRole", second);

          List<Permission> permissions = rm.getPermissions(schema.getName(), "MergeRole");
          Permission merged =
              permissions.stream()
                  .filter(p -> "Patients".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          assertNotNull(merged);
          assertEquals(
              SelectLevel.ROW, merged.getSelect(), "Select should be preserved from first grant");
          assertEquals(
              ModifyLevel.TABLE, merged.getInsert(), "Insert should be added from second grant");
        });
  }

  @Test
  public void testGrantPermissionFlag() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_grantFlag");
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "GrantRole");

          Permission perm = new Permission();
          perm.setTable("*");
          perm.setGrant(true);
          rm.grant(schema.getName(), "GrantRole", perm);

          List<Permission> permissions = rm.getPermissions(schema.getName(), "GrantRole");
          Permission wildcardPerm =
              permissions.stream().filter(p -> "*".equals(p.getTable())).findFirst().orElse(null);
          assertNotNull(wildcardPerm, "Should find wildcard permission");
          assertTrue(wildcardPerm.getGrant(), "Grant flag should be true");
        });
  }

  @Test
  public void testCleanupTablePermissions() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_cleanupTable");
          schema.create(table("CleanTable").add(column("id").setPkey()));
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "CleanRole");

          Permission perm = new Permission();
          perm.setTable("CleanTable");
          perm.setSelect(SelectLevel.TABLE);
          rm.grant(schema.getName(), "CleanRole", perm);

          rm.cleanupTablePermissions(schema.getName(), "CleanTable");

          Integer rlsCount =
              jooq(db)
                  .selectCount()
                  .from("\"MOLGENIS\".\"rls_permissions\"")
                  .where(field("table_schema").eq(inline(schema.getName())))
                  .and(field("table_name").eq(inline("CleanTable")))
                  .fetchOne(0, Integer.class);
          assertEquals(0, rlsCount, "rls_permissions rows should be deleted");

          String fullRole = SqlRoleManager.fullRoleName(schema.getName(), "CleanRole");
          Boolean canSelect =
              jooq(db)
                  .select(
                      field(
                          "has_table_privilege({0}, format('%I.%I', {1}, {2}), 'SELECT')",
                          Boolean.class,
                          inline(fullRole),
                          inline(schema.getName()),
                          inline("CleanTable")))
                  .fetchOne(0, Boolean.class);
          assertTrue(canSelect, "PG SELECT grant should still exist after cleanup");
        });
  }

  @Test
  public void testCleanupSchemaPermissions() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_cleanupSchema");
          schema.create(table("Table1").add(column("id").setPkey()));
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "CleanRole2");

          Permission perm = new Permission();
          perm.setTable("Table1");
          perm.setSelect(SelectLevel.ROW);
          rm.grant(schema.getName(), "CleanRole2", perm);

          rm.cleanupSchemaPermissions(schema.getName());

          Integer count =
              jooq(db)
                  .selectCount()
                  .from("\"MOLGENIS\".\"rls_permissions\"")
                  .where(field("table_schema").eq(inline(schema.getName())))
                  .fetchOne(0, Integer.class);
          assertEquals(0, count, "All rls_permissions rows should be deleted");
        });
  }

  @Test
  public void testGrantRevokeSelectCycle() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_selectCycle");
          schema.create(table("TestTable").add(column("id").setPkey()));
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "Reader");

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/Reader";

          Record rlsRecBefore =
              jooq(db)
                  .selectFrom(org.jooq.impl.DSL.table(name("MOLGENIS", "rls_permissions")))
                  .where(field(name("role_name")).eq(fullRoleName))
                  .and(field(name("table_schema")).eq(schema.getName()))
                  .and(field(name("table_name")).eq("TestTable"))
                  .fetchOne();
          assertTrue(rlsRecBefore == null, "No rls_permissions entry should exist before grant");

          Permission perm = new Permission();
          perm.setTable("TestTable");
          perm.setSelect(SelectLevel.TABLE);
          rm.grant(schema.getName(), "Reader", perm);

          String tableFqn = "\"" + schema.getName() + "\".\"TestTable\"";
          Boolean hasSelect =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'SELECT')", fullRoleName, tableFqn)
                  .into(Boolean.class);
          assertTrue(hasSelect, "Role should have SELECT after grant");

          Record rlsRecAfterGrant =
              jooq(db)
                  .selectFrom(org.jooq.impl.DSL.table(name("MOLGENIS", "rls_permissions")))
                  .where(field(name("role_name")).eq(fullRoleName))
                  .and(field(name("table_schema")).eq(schema.getName()))
                  .and(field(name("table_name")).eq("TestTable"))
                  .fetchOne();
          assertNotNull(rlsRecAfterGrant, "rls_permissions entry should exist after grant");
          assertEquals(
              "TABLE",
              rlsRecAfterGrant.get(field(name("select_level")), String.class),
              "select_level should be TABLE");

          Permission revokePerm = new Permission();
          revokePerm.setTable("TestTable");
          revokePerm.setSelect(SelectLevel.TABLE);
          rm.revoke(schema.getName(), "Reader", revokePerm);

          Record rlsRecAfterRevoke =
              jooq(db)
                  .selectFrom(org.jooq.impl.DSL.table(name("MOLGENIS", "rls_permissions")))
                  .where(field(name("role_name")).eq(fullRoleName))
                  .and(field(name("table_schema")).eq(schema.getName()))
                  .and(field(name("table_name")).eq("TestTable"))
                  .fetchOne();
          assertTrue(
              rlsRecAfterRevoke == null
                  || rlsRecAfterRevoke.get(field(name("select_level")), String.class) == null,
              "rls_permissions entry should be removed or select_level cleared after revoke");
        });
  }

  @Test
  public void testGrantRevokeModifyCycle() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_modifyCycle");
          schema.create(table("TestTable").add(column("id").setPkey()));
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "Writer");

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/Writer";
          String tableFqn = "\"" + schema.getName() + "\".\"TestTable\"";

          Permission perm = new Permission();
          perm.setTable("TestTable");
          perm.setInsert(ModifyLevel.TABLE);
          perm.setUpdate(ModifyLevel.TABLE);
          perm.setDelete(ModifyLevel.TABLE);
          rm.grant(schema.getName(), "Writer", perm);

          Boolean hasInsert =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'INSERT')", fullRoleName, tableFqn)
                  .into(Boolean.class);
          Boolean hasUpdate =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'UPDATE')", fullRoleName, tableFqn)
                  .into(Boolean.class);
          Boolean hasDelete =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'DELETE')", fullRoleName, tableFqn)
                  .into(Boolean.class);
          assertTrue(hasInsert, "Role should have INSERT");
          assertTrue(hasUpdate, "Role should have UPDATE");
          assertTrue(hasDelete, "Role should have DELETE");

          Record rlsRec =
              jooq(db)
                  .selectFrom(org.jooq.impl.DSL.table(name("MOLGENIS", "rls_permissions")))
                  .where(field(name("role_name")).eq(fullRoleName))
                  .and(field(name("table_schema")).eq(schema.getName()))
                  .and(field(name("table_name")).eq("TestTable"))
                  .fetchOne();
          assertNotNull(rlsRec, "rls_permissions row should exist");
          Boolean insertRls = rlsRec.get(field(name("insert_rls")), Boolean.class);
          Boolean updateRls = rlsRec.get(field(name("update_rls")), Boolean.class);
          Boolean deleteRls = rlsRec.get(field(name("delete_rls")), Boolean.class);
          assertEquals(Boolean.FALSE, insertRls, "insert_rls should be false for TABLE level");
          assertEquals(Boolean.FALSE, updateRls, "update_rls should be false for TABLE level");
          assertEquals(Boolean.FALSE, deleteRls, "delete_rls should be false for TABLE level");

          Permission revokePerm = new Permission();
          revokePerm.setTable("TestTable");
          revokePerm.setDelete(ModifyLevel.TABLE);
          rm.revoke(schema.getName(), "Writer", revokePerm);

          hasInsert =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'INSERT')", fullRoleName, tableFqn)
                  .into(Boolean.class);
          hasUpdate =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'UPDATE')", fullRoleName, tableFqn)
                  .into(Boolean.class);
          hasDelete =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'DELETE')", fullRoleName, tableFqn)
                  .into(Boolean.class);
          assertTrue(hasInsert, "Role should still have INSERT");
          assertTrue(hasUpdate, "Role should still have UPDATE");
          assertFalse(hasDelete, "Role should not have DELETE after revoke");

          List<Permission> permissionsAfterRevoke = rm.getPermissions(schema.getName(), "Writer");
          Permission foundAfterRevoke =
              permissionsAfterRevoke.stream()
                  .filter(p -> "TestTable".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          if (foundAfterRevoke != null) {
            assertEquals(ModifyLevel.TABLE, foundAfterRevoke.getInsert(), "INSERT should remain");
            assertEquals(ModifyLevel.TABLE, foundAfterRevoke.getUpdate(), "UPDATE should remain");
            assertTrue(foundAfterRevoke.getDelete() == null, "DELETE should be cleared");
          }
        });
  }

  @Test
  public void testRowLevelSelectFiltering() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_rowSelect");
          schema.create(table("Patients").add(column("id").setPkey()).add(column("name")));

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "HospitalA");

          Permission perm = new Permission();
          perm.setTable("Patients");
          perm.setSelect(SelectLevel.ROW);
          rm.grant(schema.getName(), "HospitalA", perm);

          Integer hasMgRoles =
              jooq(db)
                  .selectCount()
                  .from("information_schema.columns")
                  .where(field("table_schema").eq(inline(schema.getName())))
                  .and(field("table_name").eq(inline("Patients")))
                  .and(field("column_name").eq(inline("mg_roles")))
                  .fetchOne(0, Integer.class);
          assertTrue(hasMgRoles != null && hasMgRoles > 0, "mg_roles column should exist");

          Integer hasRlsPolicy =
              jooq(db)
                  .selectCount()
                  .from("pg_policies")
                  .where(field("schemaname").eq(inline(schema.getName())))
                  .and(field("tablename").eq(inline("Patients")))
                  .and(field("policyname").eq(inline("Patients_rls_select")))
                  .fetchOne(0, Integer.class);
          assertTrue(hasRlsPolicy != null && hasRlsPolicy > 0, "RLS policy should exist");

          Record rlsPermission =
              jooq(db)
                  .selectFrom(org.jooq.impl.DSL.table(name("MOLGENIS", "rls_permissions")))
                  .where(
                      field(name("role_name")).eq(MG_ROLE_PREFIX + schema.getName() + "/HospitalA"))
                  .and(field(name("table_schema")).eq(schema.getName()))
                  .and(field(name("table_name")).eq("Patients"))
                  .fetchOne();
          assertNotNull(rlsPermission, "rls_permissions entry should exist");
          assertEquals(
              "ROW",
              rlsPermission.get(field(name("select_level")), String.class),
              "select_level should be ROW");
        });
  }

  @Test
  public void testRowLevelInsertFiltering() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_rowInsert");
          schema.create(table("Orders").add(column("id").setPkey()));

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "ShopA");

          Permission perm = new Permission();
          perm.setTable("Orders");
          perm.setInsert(ModifyLevel.ROW);
          perm.setSelect(SelectLevel.ROW);
          rm.grant(schema.getName(), "ShopA", perm);

          Record rlsPermission =
              jooq(db)
                  .selectFrom(org.jooq.impl.DSL.table(name("MOLGENIS", "rls_permissions")))
                  .where(field(name("role_name")).eq(MG_ROLE_PREFIX + schema.getName() + "/ShopA"))
                  .and(field(name("table_schema")).eq(schema.getName()))
                  .and(field(name("table_name")).eq("Orders"))
                  .fetchOne();
          assertNotNull(rlsPermission, "rls_permissions entry should exist");
          assertEquals(
              "ROW",
              rlsPermission.get(field(name("select_level")), String.class),
              "select_level should be ROW");
          assertEquals(
              Boolean.TRUE,
              rlsPermission.get(field(name("insert_rls")), Boolean.class),
              "insert_rls should be true");

          Integer hasInsertPolicy =
              jooq(db)
                  .selectCount()
                  .from("pg_policies")
                  .where(field("schemaname").eq(inline(schema.getName())))
                  .and(field("tablename").eq(inline("Orders")))
                  .and(field("policyname").eq(inline("Orders_rls_insert")))
                  .fetchOne(0, Integer.class);
          assertTrue(
              hasInsertPolicy != null && hasInsertPolicy > 0, "RLS insert policy should exist");
        });
  }

  @Test
  public void testWildcardGrantExpandsToAllTables() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_wildcard");
          schema.create(table("Table1").add(column("id").setPkey()));
          schema.create(table("Table2").add(column("id").setPkey()));
          schema.create(table("Table3").add(column("id").setPkey()));

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "ReadAll");

          Permission perm = new Permission();
          perm.setTable("*");
          perm.setSelect(SelectLevel.TABLE);
          rm.grant(schema.getName(), "ReadAll", perm);

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/ReadAll";
          String table1Fqn = "\"" + schema.getName() + "\".\"Table1\"";
          String table2Fqn = "\"" + schema.getName() + "\".\"Table2\"";
          String table3Fqn = "\"" + schema.getName() + "\".\"Table3\"";

          Boolean hasSelect1 =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'SELECT')", fullRoleName, table1Fqn)
                  .into(Boolean.class);
          Boolean hasSelect2 =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'SELECT')", fullRoleName, table2Fqn)
                  .into(Boolean.class);
          Boolean hasSelect3 =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'SELECT')", fullRoleName, table3Fqn)
                  .into(Boolean.class);
          assertTrue(hasSelect1, "Role should have SELECT on Table1");
          assertTrue(hasSelect2, "Role should have SELECT on Table2");
          assertTrue(hasSelect3, "Role should have SELECT on Table3");

          Record rlsRec =
              jooq(db)
                  .selectFrom(org.jooq.impl.DSL.table(name("MOLGENIS", "rls_permissions")))
                  .where(field(name("role_name")).eq(fullRoleName))
                  .and(field(name("table_schema")).eq(schema.getName()))
                  .and(field(name("table_name")).eq("*"))
                  .fetchOne();
          assertNotNull(rlsRec, "Wildcard rls_permissions row should exist");
          String selectLevel = rlsRec.get(field(name("select_level")), String.class);
          assertEquals("TABLE", selectLevel, "select_level should be TABLE");

          Permission revokePerm = new Permission();
          revokePerm.setTable("*");
          revokePerm.setSelect(SelectLevel.TABLE);
          rm.revoke(schema.getName(), "ReadAll", revokePerm);

          List<Permission> permissions = rm.getPermissions(schema.getName(), "ReadAll");
          Permission wildcardPerm =
              permissions.stream().filter(p -> "*".equals(p.getTable())).findFirst().orElse(null);
          assertTrue(
              wildcardPerm == null || wildcardPerm.getSelect() == null,
              "Wildcard SELECT permission should be revoked");

          rlsRec =
              jooq(db)
                  .selectFrom(org.jooq.impl.DSL.table(name("MOLGENIS", "rls_permissions")))
                  .where(field(name("role_name")).eq(fullRoleName))
                  .and(field(name("table_schema")).eq(schema.getName()))
                  .and(field(name("table_name")).eq("*"))
                  .fetchOne();
          assertTrue(rlsRec == null, "Wildcard rls_permissions row should be deleted");
        });
  }

  @Test
  public void testPartialRevokePreservesOtherGrants() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_partialRevoke");
          schema.create(table("TestTable").add(column("id").setPkey()));

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "MixedRole");

          Permission perm = new Permission();
          perm.setTable("TestTable");
          perm.setSelect(SelectLevel.TABLE);
          perm.setInsert(ModifyLevel.TABLE);
          perm.setUpdate(ModifyLevel.ROW);
          rm.grant(schema.getName(), "MixedRole", perm);

          List<Permission> permissions = rm.getPermissions(schema.getName(), "MixedRole");
          Permission foundPerm =
              permissions.stream()
                  .filter(p -> "TestTable".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          assertNotNull(foundPerm, "Should find permission");
          assertEquals(SelectLevel.TABLE, foundPerm.getSelect());
          assertEquals(ModifyLevel.TABLE, foundPerm.getInsert());
          assertEquals(ModifyLevel.ROW, foundPerm.getUpdate());

          Permission revokePerm = new Permission();
          revokePerm.setTable("TestTable");
          revokePerm.setInsert(ModifyLevel.TABLE);
          rm.revoke(schema.getName(), "MixedRole", revokePerm);

          permissions = rm.getPermissions(schema.getName(), "MixedRole");
          foundPerm =
              permissions.stream()
                  .filter(p -> "TestTable".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          assertNotNull(foundPerm, "Permission should still exist");
          assertEquals(SelectLevel.TABLE, foundPerm.getSelect(), "SELECT should be preserved");
          assertEquals(ModifyLevel.ROW, foundPerm.getUpdate(), "UPDATE should be preserved");
          assertTrue(foundPerm.getInsert() == null, "INSERT should be revoked");

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/MixedRole";
          Record rlsRec =
              jooq(db)
                  .selectFrom(org.jooq.impl.DSL.table(name("MOLGENIS", "rls_permissions")))
                  .where(field(name("role_name")).eq(fullRoleName))
                  .and(field(name("table_schema")).eq(schema.getName()))
                  .and(field(name("table_name")).eq("TestTable"))
                  .fetchOne();
          assertNotNull(rlsRec, "rls_permissions row should still exist");
          String selectLevel = rlsRec.get(field(name("select_level")), String.class);
          Boolean updateRls = rlsRec.get(field(name("update_rls")), Boolean.class);
          Boolean insertRls = rlsRec.get(field(name("insert_rls")), Boolean.class);
          assertEquals("TABLE", selectLevel, "select_level should be TABLE");
          assertEquals(Boolean.TRUE, updateRls, "update_rls should be true");
          assertTrue(insertRls == null, "insert_rls should be cleared");
        });
  }

  @Test
  public void testRlsPermissionsTableContents() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_rlsTable");
          schema.create(table("TestTable").add(column("id").setPkey()));

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "TestRole");

          Permission perm = new Permission();
          perm.setTable("TestTable");
          perm.setSelect(SelectLevel.ROW);
          perm.setInsert(ModifyLevel.TABLE);
          perm.setGrant(true);
          rm.grant(schema.getName(), "TestRole", perm);

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/TestRole";
          Record rlsRec =
              jooq(db)
                  .selectFrom(org.jooq.impl.DSL.table(name("MOLGENIS", "rls_permissions")))
                  .where(field(name("role_name")).eq(fullRoleName))
                  .and(field(name("table_schema")).eq(schema.getName()))
                  .and(field(name("table_name")).eq("TestTable"))
                  .fetchOne();

          assertNotNull(rlsRec, "rls_permissions row should exist");
          String selectLevel = rlsRec.get(field(name("select_level")), String.class);
          Boolean insertRls = rlsRec.get(field(name("insert_rls")), Boolean.class);
          Boolean updateRls = rlsRec.get(field(name("update_rls")), Boolean.class);
          Boolean deleteRls = rlsRec.get(field(name("delete_rls")), Boolean.class);
          Boolean grantPerm = rlsRec.get(field(name("grant_permission")), Boolean.class);

          assertEquals("ROW", selectLevel, "select_level should be ROW");
          assertEquals(Boolean.FALSE, insertRls, "insert_rls should be false for TABLE level");
          assertTrue(updateRls == null, "update_rls should be NULL");
          assertTrue(deleteRls == null, "delete_rls should be NULL");
          assertEquals(Boolean.TRUE, grantPerm, "grant_permission should be true");
        });
  }

  @Test
  public void testCustomRoleSelectEnforcement() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_selectEnforce");
          schema.create(table("Table1").add(column("id").setPkey()).add(column("value")));

          schema
              .getTable("Table1")
              .insert(new Row().setString("id", "t1_row1").setString("value", "val1"));

          db.addUser("select_user");

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "Reader");

          Permission perm = new Permission();
          perm.setTable("Table1");
          perm.setSelect(SelectLevel.TABLE);
          rm.grant(schema.getName(), "Reader", perm);

          rm.addMember(schema.getName(), "Reader", "select_user");

          db.setActiveUser("select_user");
          assertDoesNotThrow(
              () -> schema.getTable("Table1").retrieveRows(),
              "User with TABLE SELECT permission should be able to read");

          db.becomeAdmin();
        });
  }

  @Test
  public void testCustomRoleInsertEnforcement() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_insertEnforce");
          schema.create(table("Table1").add(column("id").setPkey()));

          db.addUser("insert_user");

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "Writer");

          Permission perm = new Permission();
          perm.setTable("Table1");
          perm.setSelect(SelectLevel.TABLE);
          perm.setInsert(ModifyLevel.TABLE);
          rm.grant(schema.getName(), "Writer", perm);

          rm.addMember(schema.getName(), "Writer", "insert_user");

          db.setActiveUser("insert_user");
          assertDoesNotThrow(
              () -> schema.getTable("Table1").insert(new Row().setString("id", "row1")),
              "User with INSERT permission should be able to insert");

          db.becomeAdmin();
        });
  }

  @Test
  public void testCustomRoleUpdateEnforcement() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_updateEnforce");
          schema.create(table("Table1").add(column("id").setPkey()).add(column("name")));

          schema
              .getTable("Table1")
              .insert(new Row().setString("id", "row1").setString("name", "original"));

          db.addUser("update_user");

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "Updater");

          Permission perm = new Permission();
          perm.setTable("Table1");
          perm.setSelect(SelectLevel.TABLE);
          perm.setUpdate(ModifyLevel.TABLE);
          rm.grant(schema.getName(), "Updater", perm);

          rm.addMember(schema.getName(), "Updater", "update_user");

          db.setActiveUser("update_user");
          assertDoesNotThrow(
              () ->
                  schema
                      .getTable("Table1")
                      .update(new Row().setString("id", "row1").setString("name", "updated")),
              "User with UPDATE permission should be able to update");

          db.becomeAdmin();
        });
  }

  @Test
  public void testCustomRoleDeleteEnforcement() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_deleteEnforce");
          schema.create(table("Table1").add(column("id").setPkey()));

          schema.getTable("Table1").insert(new Row().setString("id", "row1"));

          db.addUser("delete_user");

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "Deleter");

          Permission perm = new Permission();
          perm.setTable("Table1");
          perm.setSelect(SelectLevel.TABLE);
          perm.setDelete(ModifyLevel.TABLE);
          rm.grant(schema.getName(), "Deleter", perm);

          rm.addMember(schema.getName(), "Deleter", "delete_user");

          db.setActiveUser("delete_user");
          assertDoesNotThrow(
              () -> schema.getTable("Table1").delete(new Row().setString("id", "row1")),
              "User with DELETE permission should be able to delete");

          db.becomeAdmin();
        });
  }

  @Test
  public void testCustomRoleMultiplePermissions() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_multiPerm");
          schema.create(table("Table1").add(column("id").setPkey()).add(column("name")));

          schema
              .getTable("Table1")
              .insert(new Row().setString("id", "row1").setString("name", "original"));

          db.addUser("multi_user");

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "MultiRole");

          Permission perm = new Permission();
          perm.setTable("Table1");
          perm.setSelect(SelectLevel.TABLE);
          perm.setInsert(ModifyLevel.TABLE);
          perm.setUpdate(ModifyLevel.TABLE);
          perm.setDelete(ModifyLevel.TABLE);
          rm.grant(schema.getName(), "MultiRole", perm);

          rm.addMember(schema.getName(), "MultiRole", "multi_user");

          db.setActiveUser("multi_user");

          assertDoesNotThrow(() -> schema.getTable("Table1").retrieveRows(), "Should allow SELECT");
          assertDoesNotThrow(
              () -> schema.getTable("Table1").insert(new Row().setString("id", "row2")),
              "Should allow INSERT");
          assertDoesNotThrow(
              () ->
                  schema
                      .getTable("Table1")
                      .update(new Row().setString("id", "row1").setString("name", "updated")),
              "Should allow UPDATE");
          assertDoesNotThrow(
              () -> schema.getTable("Table1").delete(new Row().setString("id", "row2")),
              "Should allow DELETE");

          db.becomeAdmin();
        });
  }

  @Test
  public void testGetMyPermissions() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_myPermissions");
          schema.create(table("Data1").add(column("id").setPkey()));
          schema.create(table("Data2").add(column("id").setPkey()));
          db.addUser("myperms_user");
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "CustomRole");

          Permission perm1 = new Permission();
          perm1.setTable("Data1");
          perm1.setSelect(SelectLevel.ROW);
          perm1.setInsert(ModifyLevel.ROW);
          rm.grant(schema.getName(), "CustomRole", perm1);

          Permission perm2 = new Permission();
          perm2.setTable("Data2");
          perm2.setSelect(SelectLevel.TABLE);
          rm.grant(schema.getName(), "CustomRole", perm2);

          rm.addMember(schema.getName(), "CustomRole", "myperms_user");

          db.setActiveUser("myperms_user");

          List<Permission> myPermissions = rm.getPermissionsForActiveUser(schema.getName());
          assertEquals(2, myPermissions.size(), "Should have 2 permissions");

          Permission foundPerm1 =
              myPermissions.stream()
                  .filter(p -> "Data1".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          assertNotNull(foundPerm1, "Should find Data1 permission");
          assertEquals(SelectLevel.ROW, foundPerm1.getSelect());
          assertEquals(ModifyLevel.ROW, foundPerm1.getInsert());

          Permission foundPerm2 =
              myPermissions.stream()
                  .filter(p -> "Data2".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          assertNotNull(foundPerm2, "Should find Data2 permission");
          assertEquals(SelectLevel.TABLE, foundPerm2.getSelect());
          assertNull(foundPerm2.getInsert());
        });
  }

  @Test
  public void testGetMyPermissionsAsAnonymous() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_myPermsAnon");
          db.clearActiveUser();
          SqlRoleManager rm = roleManager(db);
          List<Permission> myPermissions = rm.getPermissionsForActiveUser(schema.getName());
          assertEquals(0, myPermissions.size(), "Anonymous should have no permissions");
        });
  }

  @Test
  public void testGetMyPermissionsNoRole() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_myPermsNoRole");
          db.addUser("norole_user");
          db.setActiveUser("norole_user");
          SqlRoleManager rm = roleManager(db);
          List<Permission> myPermissions = rm.getPermissionsForActiveUser(schema.getName());
          assertEquals(0, myPermissions.size(), "User with no role should have no permissions");
        });
  }

  @Test
  public void testGetMyPermissionsSystemRoles() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_sysRolePerms");
          schema.create(table("Patients").add(column("id").setPkey()));
          schema.create(table("Samples").add(column("id").setPkey()));
          SqlRoleManager rm = roleManager(db);

          db.addUser("viewer_user");
          schema.addMember("viewer_user", "Viewer");
          db.setActiveUser("viewer_user");

          List<Permission> viewerPerms = rm.getPermissionsForActiveUser(schema.getName());
          assertEquals(2, viewerPerms.size(), "Viewer should see 2 table permissions");
          for (Permission p : viewerPerms) {
            assertEquals(SelectLevel.TABLE, p.getSelect());
            assertNull(p.getInsert(), "Viewer should not have insert");
            assertNull(p.getUpdate(), "Viewer should not have update");
            assertNull(p.getDelete(), "Viewer should not have delete");
          }

          db.becomeAdmin();
          db.addUser("editor_user");
          schema.addMember("editor_user", "Editor");
          db.setActiveUser("editor_user");

          List<Permission> editorPerms = rm.getPermissionsForActiveUser(schema.getName());
          assertEquals(2, editorPerms.size(), "Editor should see 2 table permissions");
          for (Permission p : editorPerms) {
            assertEquals(SelectLevel.TABLE, p.getSelect());
            assertEquals(ModifyLevel.TABLE, p.getInsert());
            assertEquals(ModifyLevel.TABLE, p.getUpdate());
            assertEquals(ModifyLevel.TABLE, p.getDelete());
          }

          db.becomeAdmin();
          db.addUser("count_user");
          schema.addMember("count_user", "Count");
          db.setActiveUser("count_user");

          List<Permission> countPerms = rm.getPermissionsForActiveUser(schema.getName());
          assertEquals(2, countPerms.size(), "Count should see 2 table permissions");
          for (Permission p : countPerms) {
            assertEquals(SelectLevel.COUNT, p.getSelect());
            assertNull(p.getInsert());
          }

          db.becomeAdmin();
        });
  }

  @Test
  public void testDeleteRoleCleansMgRoles() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_delRoleClean");
          schema.create(table("TestTable").add(column("id").setPkey()));

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "TestRole");

          Permission perm = new Permission();
          perm.setTable("TestTable");
          perm.setSelect(SelectLevel.ROW);
          rm.grant(schema.getName(), "TestRole", perm);

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/TestRole";

          schema
              .getTable("TestTable")
              .insert(
                  new Row()
                      .setString("id", "row1")
                      .setStringArray("mg_roles", new String[] {fullRoleName}));
          schema
              .getTable("TestTable")
              .insert(
                  new Row()
                      .setString("id", "row2")
                      .setStringArray("mg_roles", new String[] {fullRoleName, "OtherRole"}));

          Result<?> beforeDelete =
              jooq(db)
                  .select(field(name("id")), field(name("mg_roles")))
                  .from(org.jooq.impl.DSL.table(name(schema.getName(), "TestTable")))
                  .fetch();
          assertEquals(2, beforeDelete.size(), "Should have 2 rows before delete");

          rm.deleteRole(schema.getName(), "TestRole");

          Result<?> afterDelete =
              jooq(db)
                  .select(field(name("id")), field(name("mg_roles")))
                  .from(org.jooq.impl.DSL.table(name(schema.getName(), "TestTable")))
                  .fetch();

          for (org.jooq.Record rec : afterDelete) {
            Object rolesObj = rec.get(field(name("mg_roles")));
            if (rolesObj != null) {
              String[] roles;
              if (rolesObj instanceof String[]) {
                roles = (String[]) rolesObj;
              } else {
                try {
                  roles = (String[]) ((java.sql.Array) rolesObj).getArray();
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              }
              for (String role : roles) {
                assertNotEquals(
                    fullRoleName, role, "mg_roles should not contain deleted role name");
              }
            }
          }
        });
  }

  @Test
  public void testDropTableCleansPermissions() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_dropTblClean");
          schema.create(table("ToDropTable").add(column("id").setPkey()));

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "TableRole");

          Permission perm = new Permission();
          perm.setTable("ToDropTable");
          perm.setSelect(SelectLevel.TABLE);
          rm.grant(schema.getName(), "TableRole", perm);

          Integer countBefore =
              jooq(db)
                  .selectCount()
                  .from("\"MOLGENIS\".\"rls_permissions\"")
                  .where(field("table_schema").eq(inline(schema.getName())))
                  .and(field("table_name").eq(inline("ToDropTable")))
                  .fetchOne(0, Integer.class);
          assertTrue(countBefore != null && countBefore > 0, "Permission should exist before drop");

          schema.dropTable("ToDropTable");

          Integer countAfter =
              jooq(db)
                  .selectCount()
                  .from("\"MOLGENIS\".\"rls_permissions\"")
                  .where(field("table_schema").eq(inline(schema.getName())))
                  .and(field("table_name").eq(inline("ToDropTable")))
                  .fetchOne(0, Integer.class);
          assertEquals(0, countAfter, "Permission should be deleted after table drop");
        });
  }

  @Test
  public void testDropSchemaCleansPermissions() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_dropSchClean");
          schema.create(table("Table1").add(column("id").setPkey()));

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "SchemaRole");

          Permission perm = new Permission();
          perm.setTable("Table1");
          perm.setSelect(SelectLevel.ROW);
          rm.grant(schema.getName(), "SchemaRole", perm);

          Integer countBefore =
              jooq(db)
                  .selectCount()
                  .from("\"MOLGENIS\".\"rls_permissions\"")
                  .where(field("table_schema").eq(inline(schema.getName())))
                  .fetchOne(0, Integer.class);
          assertTrue(
              countBefore != null && countBefore > 0,
              "Permissions should exist before schema drop");

          db.dropSchema("TestRM_dropSchClean");

          Integer countAfter =
              jooq(db)
                  .selectCount()
                  .from("\"MOLGENIS\".\"rls_permissions\"")
                  .where(field("table_schema").eq(inline("TestRM_dropSchClean")))
                  .fetchOne(0, Integer.class);
          assertEquals(0, countAfter, "All permissions should be deleted after schema drop");
        });
  }

  @Test
  public void testAutoPopulateMgRolesOnInsert() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_autoPopulate");
          schema.create(table("Patients").add(column("id").setPkey()).add(column("name")));

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "HospitalA");

          Permission perm = new Permission();
          perm.setTable("Patients");
          perm.setSelect(SelectLevel.ROW);
          perm.setInsert(ModifyLevel.TABLE);
          rm.grant(schema.getName(), "HospitalA", perm);

          db.addUser("auto_user");
          rm.addMember(schema.getName(), "HospitalA", "auto_user");

          db.setActiveUser("auto_user");

          schema
              .getTable("Patients")
              .insert(new Row().setString("id", "patient1").setString("name", "Alice"));

          Result<?> result =
              jooq(db)
                  .select(field(name("id")), field(name("mg_roles")))
                  .from(org.jooq.impl.DSL.table(name(schema.getName(), "Patients")))
                  .where(field(name("id")).eq("patient1"))
                  .fetch();

          assertEquals(1, result.size(), "Should have inserted 1 row");
          org.jooq.Record rec = result.get(0);
          Object rolesObj = rec.get(field(name("mg_roles")));
          assertNotNull(rolesObj, "mg_roles should be auto-populated");

          String[] roles;
          if (rolesObj instanceof String[]) {
            roles = (String[]) rolesObj;
          } else {
            try {
              roles = (String[]) ((java.sql.Array) rolesObj).getArray();
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }

          String expectedRole = MG_ROLE_PREFIX + schema.getName() + "/HospitalA";
          assertEquals(1, roles.length, "Should have 1 role auto-populated");
          assertEquals(
              expectedRole, roles[0], "Auto-populated role should match user's custom role");

          db.setActiveUser("auto_user");
          schema
              .getTable("Patients")
              .insert(
                  new Row()
                      .setString("id", "patient2")
                      .setString("name", "Bob")
                      .setStringArray("mg_roles", new String[] {"ExplicitRole"}));

          db.becomeAdmin();

          Result<?> result2 =
              jooq(db)
                  .select(field(name("id")), field(name("mg_roles")))
                  .from(org.jooq.impl.DSL.table(name(schema.getName(), "Patients")))
                  .where(field(name("id")).eq("patient2"))
                  .fetch();

          org.jooq.Record rec2 = result2.get(0);
          Object rolesObj2 = rec2.get(field(name("mg_roles")));
          String[] roles2;
          if (rolesObj2 instanceof String[]) {
            roles2 = (String[]) rolesObj2;
          } else {
            try {
              roles2 = (String[]) ((java.sql.Array) rolesObj2).getArray();
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }

          assertEquals(1, roles2.length, "Should have 1 explicit role");
          assertEquals("ExplicitRole", roles2[0], "Explicit mg_roles should be preserved");
        });
  }

  @Test
  public void testWildcardGrantsAppliedToNewTable() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_wildcardNew");

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "ReadAllRole");

          Permission wildcardPerm = new Permission();
          wildcardPerm.setTable("*");
          wildcardPerm.setSelect(SelectLevel.TABLE);
          wildcardPerm.setInsert(ModifyLevel.TABLE);
          rm.grant(schema.getName(), "ReadAllRole", wildcardPerm);

          schema.create(table("NewTable").add(column("id").setPkey()));

          Permission newTablePerm = new Permission();
          newTablePerm.setTable("NewTable");
          newTablePerm.setSelect(SelectLevel.ROW);
          rm.grant(schema.getName(), "ReadAllRole", newTablePerm);

          String fullRoleName = MG_ROLE_PREFIX + schema.getName() + "/ReadAllRole";
          String tableFqn = "\"" + schema.getName() + "\".\"NewTable\"";

          Boolean hasSelect =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'SELECT')", fullRoleName, tableFqn)
                  .into(Boolean.class);
          assertTrue(hasSelect, "Role should have SELECT on new table from wildcard grant");

          Boolean hasInsert =
              jooq(db)
                  .fetchOne(
                      "SELECT has_table_privilege({0}, {1}, 'INSERT')", fullRoleName, tableFqn)
                  .into(Boolean.class);
          assertTrue(hasInsert, "Role should have INSERT on new table from wildcard grant");
        });
  }

  @Test
  public void testRlsCacheInvalidationOnPermissionChange() {
    String schemaName = "TestRM_cacheInvalidate";

    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema(schemaName);
          schema.create(table("Patients").add(column("id").setPkey()).add(column("name")));

          SqlRoleManager rm = roleManager(db);
          rm.createRole(schemaName, "HospitalA");
          rm.createRole(schemaName, "HospitalB");

          Permission perm = new Permission();
          perm.setTable("Patients");
          perm.setSelect(SelectLevel.ROW);
          rm.grant(schemaName, "HospitalA", perm);

          db.addUser("cache_user");
          rm.addMember(schemaName, "HospitalA", "cache_user");

          String roleA = MG_ROLE_PREFIX + schemaName + "/HospitalA";
          String roleB = MG_ROLE_PREFIX + schemaName + "/HospitalB";

          schema
              .getTable("Patients")
              .insert(
                  new Row()
                      .setString("id", "p1")
                      .setString("name", "Alice")
                      .setStringArray("mg_roles", new String[] {roleA}),
                  new Row()
                      .setString("id", "p2")
                      .setString("name", "Bob")
                      .setStringArray("mg_roles", new String[] {roleB}),
                  new Row()
                      .setString("id", "p3")
                      .setString("name", "Charlie")
                      .setStringArray("mg_roles", new String[] {roleA}));
        });

    database.tx(
        db -> {
          db.setActiveUser("cache_user");
          List<Row> rowsBefore = db.getSchema(schemaName).getTable("Patients").retrieveRows();
          assertEquals(
              2, rowsBefore.size(), "User with ROW permission should see only HospitalA rows");
          assertTrue(rowsBefore.stream().anyMatch(r -> "p1".equals(r.getString("id"))));
          assertTrue(rowsBefore.stream().anyMatch(r -> "p3".equals(r.getString("id"))));
        });

    database.tx(
        db -> {
          SqlRoleManager rm = roleManager(db);
          Permission upgradePerm = new Permission();
          upgradePerm.setTable("Patients");
          upgradePerm.setSelect(SelectLevel.TABLE);
          rm.grant(schemaName, "HospitalA", upgradePerm);
        });

    database.tx(
        db -> {
          db.setActiveUser("cache_user");
          List<Row> rowsAfter = db.getSchema(schemaName).getTable("Patients").retrieveRows();
          assertEquals(
              3,
              rowsAfter.size(),
              "After upgrading to TABLE, user should see all rows (cache invalidated)");
        });

    database.tx(
        db -> {
          SqlRoleManager rm = roleManager(db);
          Permission downgradePerm = new Permission();
          downgradePerm.setTable("Patients");
          downgradePerm.setSelect(SelectLevel.ROW);
          rm.grant(schemaName, "HospitalA", downgradePerm);
        });

    database.tx(
        db -> {
          db.setActiveUser("cache_user");
          List<Row> rowsDowngraded = db.getSchema(schemaName).getTable("Patients").retrieveRows();
          assertEquals(
              2,
              rowsDowngraded.size(),
              "After downgrading to ROW, user should see filtered rows again");
        });
  }

  @Test
  public void testSystemRolePermissions() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_sysPerms");
          schema.create(table("Table1").add(column("id").setPkey()));
          SqlRoleManager rm = roleManager(db);

          List<Permission> viewerPerms = rm.getPermissions(schema.getName(), "Viewer");
          assertEquals(1, viewerPerms.size(), "Viewer should have 1 wildcard permission");
          assertEquals("*", viewerPerms.get(0).getTable());
          assertEquals(SelectLevel.TABLE, viewerPerms.get(0).getSelect());
          assertNull(viewerPerms.get(0).getInsert());
          assertNull(viewerPerms.get(0).getDelete());

          List<Permission> editorPerms = rm.getPermissions(schema.getName(), "Editor");
          assertEquals(1, editorPerms.size(), "Editor should have 1 wildcard permission");
          assertEquals("*", editorPerms.get(0).getTable());
          assertEquals(SelectLevel.TABLE, editorPerms.get(0).getSelect());
          assertEquals(ModifyLevel.TABLE, editorPerms.get(0).getInsert());
          assertEquals(ModifyLevel.TABLE, editorPerms.get(0).getUpdate());
          assertEquals(ModifyLevel.TABLE, editorPerms.get(0).getDelete());

          List<Permission> managerPerms = rm.getPermissions(schema.getName(), "Manager");
          assertEquals(1, managerPerms.size());
          assertEquals("*", managerPerms.get(0).getTable());
          assertTrue(managerPerms.get(0).getGrant(), "Manager should have grant");

          List<Permission> countPerms = rm.getPermissions(schema.getName(), "Count");
          assertEquals(1, countPerms.size());
          assertEquals(SelectLevel.COUNT, countPerms.get(0).getSelect());

          List<Permission> existsPerms = rm.getPermissions(schema.getName(), "Exists");
          assertEquals(1, existsPerms.size());
          assertEquals(SelectLevel.EXISTS, existsPerms.get(0).getSelect());
        });
  }

  @Test
  public void testSelectLevelPriorityOverPgGrant() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_selectPriority");
          schema.create(table("Data").add(column("id").setPkey()));
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "CountOnly");

          Permission perm = new Permission();
          perm.setTable("Data");
          perm.setSelect(SelectLevel.COUNT);
          rm.grant(schema.getName(), "CountOnly", perm);

          List<Permission> permissions = rm.getPermissions(schema.getName(), "CountOnly");
          Permission dataPerm =
              permissions.stream()
                  .filter(p -> "Data".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          assertNotNull(dataPerm, "Should find Data permission");
          assertEquals(
              SelectLevel.COUNT,
              dataPerm.getSelect(),
              "Select level should be COUNT, not TABLE");
        });
  }

  @Test
  public void testWildcardWithPerTableOverride() {
    database.tx(
        db -> {
          Schema schema = db.dropCreateSchema("TestRM_wildcardOverride");
          schema.create(table("Table1").add(column("id").setPkey()));
          schema.create(table("Table2").add(column("id").setPkey()));
          SqlRoleManager rm = roleManager(db);
          rm.createRole(schema.getName(), "Curator");

          Permission wildcardPerm = new Permission();
          wildcardPerm.setTable("*");
          wildcardPerm.setSelect(SelectLevel.TABLE);
          rm.grant(schema.getName(), "Curator", wildcardPerm);

          Permission table1Perm = new Permission();
          table1Perm.setTable("Table1");
          table1Perm.setSelect(SelectLevel.TABLE);
          table1Perm.setInsert(ModifyLevel.TABLE);
          table1Perm.setUpdate(ModifyLevel.TABLE);
          table1Perm.setDelete(ModifyLevel.TABLE);
          table1Perm.setGrant(true);
          rm.grant(schema.getName(), "Curator", table1Perm);

          List<Permission> permissions = rm.getPermissions(schema.getName(), "Curator");

          Permission wc =
              permissions.stream()
                  .filter(p -> "*".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          assertNotNull(wc, "Should find wildcard permission");
          assertEquals(SelectLevel.TABLE, wc.getSelect());
          assertNull(wc.getInsert());

          Permission t1 =
              permissions.stream()
                  .filter(p -> "Table1".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          assertNotNull(t1, "Should find Table1 override");
          assertEquals(SelectLevel.TABLE, t1.getSelect());
          assertEquals(ModifyLevel.TABLE, t1.getInsert());
          assertEquals(ModifyLevel.TABLE, t1.getDelete());
          assertTrue(t1.getGrant(), "Table1 should have grant");

          Permission t2 =
              permissions.stream()
                  .filter(p -> "Table2".equals(p.getTable()))
                  .findFirst()
                  .orElse(null);
          assertNull(t2, "Table2 should not have explicit permission");
        });
  }
}
