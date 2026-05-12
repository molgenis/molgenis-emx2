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
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;

/** Tests for role CRUD, grant/revoke, role info queries, and permission merging. */
class TestTableRoleManagement {

  private static Database database;
  private static final String SCHEMA = "TestTableRoleManagement";
  private static final String TABLE_A = "TableA";
  private static final String TABLE_B = "TableB";

  private static final String USER_VIEWER = "trm_user_viewer";
  private static final String USER_EDITOR = "trm_user_editor";
  private static final String USER_NO_ACCESS = "trm_user_noaccess";

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();

    for (String user : List.of(USER_VIEWER, USER_EDITOR, USER_NO_ACCESS)) {
      if (!database.hasUser(user)) database.addUser(user);
    }

    Schema schema = database.dropCreateSchema(SCHEMA);
    schema.create(
        table(TABLE_A).add(column("id").setPkey()).add(column("value")),
        table(TABLE_B).add(column("id").setPkey()).add(column("value")));

    schema.getTable(TABLE_A).insert(new Row().setString("id", "r1").setString("value", "hello"));
    schema.getTable(TABLE_B).insert(new Row().setString("id", "r1").setString("value", "world"));
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
    assertFalse(schema.getRoles().stream().anyMatch(r -> r.name().equals("ReaderA")));
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

  // ── Grant / revoke ─────────────────────────────────────────────────────────

  @Test
  void cannotGrantToSystemRole() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    TablePermission selectPermission = new TablePermission(TABLE_A).select(SelectScope.ALL);
    assertThrows(MolgenisException.class, () -> schema.grant("Viewer", selectPermission));
  }

  @Test
  void cannotGrantToNonExistentRole() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    TablePermission selectPermission = new TablePermission(TABLE_A).select(SelectScope.ALL);
    assertThrows(MolgenisException.class, () -> schema.grant("NonExistentRole", selectPermission));
  }

  @Test
  void cannotGrantToNonExistentTable() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("TempRole");
    TablePermission permission = new TablePermission("NoSuchTable").select(SelectScope.ALL);
    assertThrows(MolgenisException.class, () -> schema.grant("TempRole", permission));
  }

  @Test
  void revokeRemovesTableAccess() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("RevokeRole");
    schema.grant("RevokeRole", new TablePermission(TABLE_A).select(SelectScope.ALL));
    schema.addMember(USER_VIEWER, "RevokeRole");

    database.setActiveUser(USER_VIEWER);
    assertDoesNotThrow(() -> database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows());

    database.becomeAdmin();
    schema.revoke("RevokeRole", TABLE_A);

    database.setActiveUser(USER_VIEWER);

    assertThrows(
        Exception.class, () -> database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows());
  }

  @Test
  void grantWithFalseRevokesIndividualPrivilege() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("PartialRevokeRole");
    schema.grant(
        "PartialRevokeRole",
        new TablePermission(TABLE_A).select(SelectScope.ALL).insert(UpdateScope.ALL));
    schema.addMember(USER_EDITOR, "PartialRevokeRole");

    database.setActiveUser(USER_EDITOR);

    database
        .getSchema(SCHEMA)
        .getTable(TABLE_A)
        .insert(new Row().setString("id", "r_partial").setString("value", "v"));

    database.becomeAdmin();
    schema.grant("PartialRevokeRole", new TablePermission(TABLE_A).insert(UpdateScope.NONE));

    database.setActiveUser(USER_EDITOR);

    assertThrows(
        Exception.class,
        () ->
            database
                .getSchema(SCHEMA)
                .getTable(TABLE_A)
                .insert(new Row().setString("id", "r_partial2").setString("value", "v")));

    assertDoesNotThrow(() -> database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows());
  }

  @Test
  void grantWithFalseIsDistinguishableFromNull() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("FalseNullRole");
    schema.grant(
        "FalseNullRole",
        new TablePermission(TABLE_A).select(SelectScope.ALL).insert(UpdateScope.ALL));
    schema.grant("FalseNullRole", new TablePermission(TABLE_A).insert(UpdateScope.NONE));

    Role info = schema.getRoleInfo("FalseNullRole");
    TablePermission p = info.permissions().getFirst();
    assertEquals(TABLE_A, p.table());
    assertEquals(SelectScope.ALL, p.select(), "select should still be ALL");
    assertNull(p.insert(), "insert should be null after revoke (not granted)");
    assertNull(p.update());
    assertNull(p.delete());
  }

  @Test
  void grantIsLostAfterTableDropAndRecreate() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("LifecycleRole");
    schema.grant("LifecycleRole", new TablePermission(TABLE_A).select(SelectScope.ALL));
    schema.addMember(USER_VIEWER, "LifecycleRole");

    database.setActiveUser(USER_VIEWER);
    database.tx(
        db -> assertDoesNotThrow(() -> db.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows()));

    database.becomeAdmin();
    schema.dropTable(TABLE_A);
    schema.create(table(TABLE_A).add(column("id").setPkey()).add(column("value")));

    database.setActiveUser(USER_VIEWER);
    assertThrows(
        Exception.class, () -> database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows());

    database.becomeAdmin();
    schema.getTable(TABLE_A).insert(new Row().setString("id", "r1").setString("value", "hello"));
    schema.removeMember(USER_VIEWER);
    schema.deleteRole("LifecycleRole");
  }

  // ── Role info & system role permissions ────────────────────────────────────

  @Test
  void getRoleInfoReturnsGrantedPermissions() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("MetaRole");
    schema.grant(
        "MetaRole", new TablePermission(TABLE_A).select(SelectScope.ALL).update(UpdateScope.ALL));

    Role info = schema.getRoleInfo("MetaRole");
    assertEquals(1, info.permissions().size());

    TablePermission p = info.permissions().getFirst();
    assertEquals(TABLE_A, p.table());
    assertEquals(SelectScope.ALL, p.select());
    assertEquals(UpdateScope.ALL, p.update());
    assertNull(p.insert());
    assertNull(p.delete());
  }

  @Test
  void getRolesIncludesCustomRoles() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ListedRole");
    List<Role> all = schema.getRoles();
    assertTrue(all.stream().anyMatch(r -> r.name().equals("ListedRole")));
  }

  static Stream<Arguments> systemRolePermissions() {
    return Stream.of(
        Arguments.of("Viewer", SelectScope.ALL, null, null, null),
        Arguments.of("Editor", SelectScope.ALL, UpdateScope.ALL, UpdateScope.ALL, UpdateScope.ALL),
        Arguments.of("Manager", SelectScope.ALL, UpdateScope.ALL, UpdateScope.ALL, UpdateScope.ALL),
        Arguments.of("Owner", SelectScope.ALL, UpdateScope.ALL, UpdateScope.ALL, UpdateScope.ALL),
        Arguments.of("Exists", null, null, null, null),
        Arguments.of("Range", null, null, null, null),
        Arguments.of("Aggregator", null, null, null, null),
        Arguments.of("Count", null, null, null, null));
  }

  @ParameterizedTest
  @MethodSource("systemRolePermissions")
  void systemRoleReturnsExpectedPermissions(
      String roleName,
      SelectScope select,
      UpdateScope insert,
      UpdateScope update,
      UpdateScope delete) {
    database.becomeAdmin();
    Role role = database.getSchema(SCHEMA).getRoleInfo(roleName);
    assertTrue(role.isSystemRole());
    assertEquals(1, role.permissions().size());
    TablePermission p = role.permissions().getFirst();
    assertEquals("*", p.table());
    assertEquals(select, p.select(), roleName + " select");
    assertEquals(insert, p.insert(), roleName + " insert");
    assertEquals(update, p.update(), roleName + " update");
    assertEquals(delete, p.delete(), roleName + " delete");
  }

  // ── Permission merging ─────────────────────────────────────────────────────

  @Test
  void getPermissionsForActiveUserReturnsCorrectPermissions() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ActiveUserRole");
    schema.grant(
        "ActiveUserRole",
        new TablePermission(TABLE_B).select(SelectScope.ALL).delete(UpdateScope.ALL));
    schema.addMember(USER_VIEWER, "ActiveUserRole");

    database.setActiveUser(USER_VIEWER);
    List<TablePermission> perms = database.getSchema(SCHEMA).getPermissionsForActiveUser();
    assertEquals(1, perms.size());
    TablePermission p = perms.getFirst();
    assertEquals(TABLE_B, p.table());
    assertEquals(SelectScope.ALL, p.select());
    assertEquals(UpdateScope.ALL, p.delete());
    assertNull(p.insert());
    assertNull(p.update());
  }

  @Test
  void userWithEmptyRoleHasNoPermissions() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("NoGrantRole");
    schema.addMember(USER_NO_ACCESS, "NoGrantRole");

    database.setActiveUser(USER_NO_ACCESS);
    List<TablePermission> perms = database.getSchema(SCHEMA).getPermissionsForActiveUser();
    assertTrue(
        perms.isEmpty() || perms.stream().noneMatch(p -> p.select() == SelectScope.ALL),
        "User with no grants should have no select permissions");
  }

  @Test
  void multipleGrantsOnSameTableAreMergedForActiveUser() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("MergeGrantRole");
    schema.grant("MergeGrantRole", new TablePermission(TABLE_A).select(SelectScope.ALL));
    schema.grant("MergeGrantRole", new TablePermission(TABLE_A).insert(UpdateScope.ALL));
    schema.addMember(USER_EDITOR, "MergeGrantRole");

    database.setActiveUser(USER_EDITOR);
    List<TablePermission> perms = database.getSchema(SCHEMA).getPermissionsForActiveUser();
    TablePermission merged =
        perms.stream()
            .filter(p -> TABLE_A.equals(p.table()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No permission entry for " + TABLE_A));
    assertEquals(SelectScope.ALL, merged.select());
    assertEquals(UpdateScope.ALL, merged.insert());
    assertNull(merged.update());
    assertNull(merged.delete());

    assertDoesNotThrow(() -> database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows());
    assertDoesNotThrow(
        () ->
            database
                .getSchema(SCHEMA)
                .getTable(TABLE_A)
                .insert(new Row().setString("id", "r_merge").setString("value", "v")));

    database.becomeAdmin();
    schema.getTable(TABLE_A).delete(new Row().setString("id", "r_merge"));
  }

  @Test
  void anonymousViewerAndCustomRolePermissionsAreMerged() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);

    schema.addMember("anonymous", "Viewer");

    schema.createRole("InsertOnly");
    schema.grant("InsertOnly", new TablePermission(TABLE_A).insert(UpdateScope.ALL));
    schema.addMember(USER_VIEWER, "InsertOnly");

    database.setActiveUser(USER_VIEWER);
    List<TablePermission> perms = database.getSchema(SCHEMA).getPermissionsForActiveUser();
    TablePermission merged =
        perms.stream()
            .filter(p -> TABLE_A.equals(p.table()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No permission entry for " + TABLE_A));
    assertEquals(SelectScope.ALL, merged.select(), "select should be merged from anonymous Viewer");
    assertEquals(UpdateScope.ALL, merged.insert(), "insert should be merged from InsertOnly role");
    assertNull(merged.update());
    assertNull(merged.delete());

    database.becomeAdmin();
    schema.removeMember("anonymous");
    schema.removeMember(USER_VIEWER);
    schema.deleteRole("InsertOnly");
  }
}
