package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.molgenis.emx2.*;

/** Tests for role CRUD, grant/revoke, role info queries, and permission merging. */
class TestTableRoleManagement {

  private static Database database;
  private static SqlRoleManager roleManager;
  private static final String SCHEMA = "TestTableRoleManagement";
  private static final String TABLE_A = "TableA";
  private static final String TABLE_B = "TableB";
  private static final String GROUP_X = "groupX";

  private static final String USER_VIEWER = "trm_user_viewer";
  private static final String USER_EDITOR = "trm_user_editor";
  private static final String USER_NO_ACCESS = "trm_user_noaccess";

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
    roleManager = new SqlRoleManager((SqlDatabase) database);

    for (String user : List.of(USER_VIEWER, USER_EDITOR, USER_NO_ACCESS)) {
      if (!database.hasUser(user)) database.addUser(user);
    }

    Schema schema = database.dropCreateSchema(SCHEMA);
    schema.create(
        table(TABLE_A).add(column("id").setPkey()).add(column("value")),
        table(TABLE_B).add(column("id").setPkey()).add(column("value")));

    schema.getTable(TABLE_A).insert(new Row().setString("id", "r1").setString("value", "hello"));
    schema.getTable(TABLE_B).insert(new Row().setString("id", "r1").setString("value", "world"));

    roleManager.createGroup(schema, GROUP_X);
  }

  // ── Role CRUD ──────────────────────────────────────────────────────────────

  @Test
  void createAndDeleteRole() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);

    schema.createRole("ReaderA");

    Role info = schema.getRoleInfo("ReaderA");
    assertEquals("ReaderA", info.name());
    assertFalse(info.isSystemRole());

    schema.deleteRole("ReaderA");
    assertFalse(schema.getRoleInfos().stream().anyMatch(r -> r.name().equals("ReaderA")));
  }

  @Test
  void cannotCreateSystemRole() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    assertThrows(MolgenisException.class, () -> schema.createRole("Viewer"));
  }

  @Test
  void cannotDeleteSystemRole() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    assertThrows(MolgenisException.class, () -> schema.deleteRole("Editor"));
  }

  @Test
  void roleNameTooLongIsRejected() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    String tooLong = "A".repeat(33);
    assertThrows(MolgenisException.class, () -> schema.createRole(tooLong));
  }

  @Test
  void nonManagerCannotCreateRole() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.addMember(USER_NO_ACCESS, Privileges.VIEWER.toString());

    database.setActiveUser(USER_NO_ACCESS);
    assertThrows(MolgenisException.class, () -> schema.createRole("Forbidden"));
  }

  // ── Grant / revoke validation ───────────────────────────────────────────────

  @Test
  void cannotGrantToSystemRole() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    TablePermission selectPermission = new TablePermission(TABLE_A).select(true);
    assertThrows(MolgenisException.class, () -> schema.grant("Viewer", selectPermission));
  }

  @Test
  void cannotGrantToNonExistentTable() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("TempRole");
    TablePermission permission = new TablePermission("NoSuchTable").select(true);
    assertThrows(MolgenisException.class, () -> schema.grant("TempRole", permission));
    schema.deleteRole("TempRole");
  }

  @Test
  void grantWithFalseIsDistinguishableFromNull() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("FalseNullRole");
    schema.grant("FalseNullRole", new TablePermission(TABLE_A).select(true).insert(true));
    schema.grant("FalseNullRole", new TablePermission(TABLE_A).insert(false));

    Role info = schema.getRoleInfo("FalseNullRole");
    assertEquals(1, info.permissions().size());

    TablePermission perm = info.permissions().getFirst();
    assertEquals(TABLE_A, perm.table());
    assertEquals(Boolean.TRUE, perm.select(), "select should still be true");
    assertNull(perm.insert(), "insert should be null after revoke (not true)");
    assertNull(perm.update());
    assertNull(perm.delete());

    schema.deleteRole("FalseNullRole");
  }

  @Test
  void grantIsLostAfterTableDropPermissionsCleared() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.create(table("DropMe").add(column("id").setPkey()));
    schema.createRole("LifecycleRole");
    schema.grant("LifecycleRole", new TablePermission("DropMe").select(true));

    assertFalse(
        schema.getRoleInfo("LifecycleRole").permissions().isEmpty(),
        "permission must exist before drop");

    schema.dropTable("DropMe");

    assertTrue(
        schema.getRoleInfo("LifecycleRole").permissions().isEmpty(),
        "permission must be cleared after table drop");

    schema.deleteRole("LifecycleRole");
  }

  // ── Role info & system role permissions ────────────────────────────────────

  @Test
  void getRoleInfoReturnsGrantedPermissions() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("MetaRole");
    schema.grant("MetaRole", new TablePermission(TABLE_A).select(true).update(true));

    Role info = schema.getRoleInfo("MetaRole");
    assertEquals(1, info.permissions().size());

    TablePermission perm = info.permissions().getFirst();
    assertEquals(TABLE_A, perm.table());
    assertEquals(Boolean.TRUE, perm.select());
    assertEquals(Boolean.TRUE, perm.update());
    assertNull(perm.insert());
    assertNull(perm.delete());

    schema.deleteRole("MetaRole");
  }

  @Test
  void getRoleInfosIncludesCustomRoles() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ListedRole");
    schema.grant("ListedRole", new TablePermission(TABLE_A).select(true));
    List<Role> all = schema.getRoleInfos();
    assertTrue(all.stream().anyMatch(r -> r.name().equals("ListedRole")));
    schema.deleteRole("ListedRole");
  }

  static Stream<Arguments> systemRolePermissions() {
    return Stream.of(
        Arguments.of("Viewer", true, null, null, null),
        Arguments.of("Editor", true, true, true, true),
        Arguments.of("Manager", true, true, true, true),
        Arguments.of("Owner", true, true, true, true),
        Arguments.of("Exists", null, null, null, null),
        Arguments.of("Range", null, null, null, null),
        Arguments.of("Aggregator", null, null, null, null),
        Arguments.of("Count", null, null, null, null));
  }

  @ParameterizedTest
  @MethodSource("systemRolePermissions")
  void systemRoleReturnsExpectedPermissions(
      String roleName, Boolean select, Boolean insert, Boolean update, Boolean delete) {
    database.becomeAdmin();
    Role role = database.getSchema(SCHEMA).getRoleInfo(roleName);
    assertTrue(role.isSystemRole());
    assertEquals(1, role.permissions().size());
    TablePermission perm = role.permissions().getFirst();
    assertEquals("*", perm.table());
    assertEquals(select, perm.select(), roleName + " select");
    assertEquals(insert, perm.insert(), roleName + " insert");
    assertEquals(update, perm.update(), roleName + " update");
    assertEquals(delete, perm.delete(), roleName + " delete");
  }

  // ── Permission merging for active user ────────────────────────────────────

  @Test
  void getPermissionsForActiveUserReturnsCorrectPermissions() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ActiveUserRole");
    schema.grant("ActiveUserRole", new TablePermission(TABLE_B).select(true).delete(true));
    roleManager.addGroupMembership(SCHEMA, GROUP_X, USER_VIEWER, "ActiveUserRole");

    database.setActiveUser(USER_VIEWER);
    List<TablePermission> perms = schema.getPermissionsForActiveUser();
    assertEquals(1, perms.size());
    TablePermission perm = perms.getFirst();
    assertEquals(TABLE_B, perm.table());
    assertEquals(Boolean.TRUE, perm.select());
    assertEquals(Boolean.TRUE, perm.delete());
    assertNull(perm.insert());
    assertNull(perm.update());

    database.becomeAdmin();
    roleManager.removeGroupMembership(SCHEMA, GROUP_X, USER_VIEWER, "ActiveUserRole");
    schema.deleteRole("ActiveUserRole");
  }

  @Test
  void userWithEmptyRoleHasNoPermissions() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("NoGrantRole");
    roleManager.addGroupMembership(SCHEMA, GROUP_X, USER_NO_ACCESS, "NoGrantRole");

    database.setActiveUser(USER_NO_ACCESS);
    List<TablePermission> perms = schema.getPermissionsForActiveUser();
    assertTrue(
        perms.isEmpty() || perms.stream().noneMatch(p -> Boolean.TRUE.equals(p.select())),
        "User with no grants should have no select permissions");

    database.becomeAdmin();
    roleManager.removeGroupMembership(SCHEMA, GROUP_X, USER_NO_ACCESS, "NoGrantRole");
    schema.deleteRole("NoGrantRole");
  }

  @Test
  void multipleGrantsOnSameTableAreMergedForActiveUser() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("MergeGrantRole");
    schema.grant("MergeGrantRole", new TablePermission(TABLE_A).select(true));
    schema.grant("MergeGrantRole", new TablePermission(TABLE_A).insert(true));
    roleManager.addGroupMembership(SCHEMA, GROUP_X, USER_EDITOR, "MergeGrantRole");

    database.setActiveUser(USER_EDITOR);
    List<TablePermission> perms = schema.getPermissionsForActiveUser();
    TablePermission merged =
        perms.stream()
            .filter(p -> TABLE_A.equals(p.table()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No permission entry for " + TABLE_A));
    assertEquals(Boolean.TRUE, merged.select());
    assertEquals(Boolean.TRUE, merged.insert());
    assertNull(merged.update());
    assertNull(merged.delete());

    database.becomeAdmin();
    roleManager.removeGroupMembership(SCHEMA, GROUP_X, USER_EDITOR, "MergeGrantRole");
    schema.deleteRole("MergeGrantRole");
  }
}
