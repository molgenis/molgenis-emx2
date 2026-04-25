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
import org.molgenis.emx2.TablePermission.SelectScope;
import org.molgenis.emx2.TablePermission.UpdateScope;

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

  // ── Grant / revoke ─────────────────────────────────────────────────────────

  @Test
  void cannotGrantToSystemRole() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    TablePermission selectPermission =
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.NONE,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false);
    assertThrows(MolgenisException.class, () -> schema.grant("Viewer", selectPermission));
  }

  @Test
  void cannotGrantToNonExistentRole() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    TablePermission selectPermission =
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.NONE,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false);
    assertThrows(MolgenisException.class, () -> schema.grant("NonExistentRole", selectPermission));
  }

  @Test
  void cannotGrantToNonExistentTable() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("TempRole");
    TablePermission permission =
        new TablePermission(
            null,
            "NoSuchTable",
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.NONE,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false);
    assertThrows(MolgenisException.class, () -> schema.grant("TempRole", permission));
  }

  @Test
  void revokeRemovesTableAccess() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("RevokeRole");
    schema.grant(
        "RevokeRole",
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.NONE,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false));
    schema.addMember(USER_VIEWER, "RevokeRole");

    // Verify access exists
    database.setActiveUser(USER_VIEWER);
    assertDoesNotThrow(() -> database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows());

    // Revoke
    database.becomeAdmin();
    schema.revoke("RevokeRole", TABLE_A);

    // Verify access is gone
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
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.ALL,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false));
    schema.addMember(USER_EDITOR, "PartialRevokeRole");

    database.setActiveUser(USER_EDITOR);

    database
        .getSchema(SCHEMA)
        .getTable(TABLE_A)
        .insert(new Row().setString("id", "r_partial").setString("value", "v"));

    database.becomeAdmin();
    schema.grant(
        "PartialRevokeRole",
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.NONE,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false));

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
  void revokeInsertLeavesSelectIntact() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("FalseNullRole");
    schema.grant(
        "FalseNullRole",
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.ALL,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false));
    schema.grant(
        "FalseNullRole",
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.NONE,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false));

    Role info = schema.getRoleInfo("FalseNullRole");
    TablePermission p = info.permissions().getFirst();
    assertEquals(TABLE_A, p.table());
    assertTrue(p.select().contains(SelectScope.ALL), "select should still be ALL");
    assertEquals(UpdateScope.NONE, p.insert(), "insert should be NONE after revoke");
    assertEquals(UpdateScope.NONE, p.update());
    assertEquals(UpdateScope.NONE, p.delete());
  }

  @Test
  void grantIsLostAfterTableDropAndRecreate() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("LifecycleRole");
    schema.grant(
        "LifecycleRole",
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.NONE,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false));
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
        "MetaRole",
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.NONE,
            UpdateScope.ALL,
            UpdateScope.NONE,
            false,
            false));

    Role info = schema.getRoleInfo("MetaRole");
    assertEquals(1, info.permissions().size());

    TablePermission p = info.permissions().getFirst();
    assertEquals(TABLE_A, p.table());
    assertTrue(p.select().contains(SelectScope.ALL));
    assertEquals(UpdateScope.ALL, p.update());
    assertEquals(UpdateScope.NONE, p.insert());
    assertEquals(UpdateScope.NONE, p.delete());
  }

  @Test
  void getRoleInfosIncludesCustomRoles() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ListedRole");
    List<Role> all = schema.getRoleInfos();
    assertTrue(all.stream().anyMatch(r -> r.name().equals("ListedRole")));
  }

  static Stream<Arguments> systemRolePermissions() {
    return Stream.of(
        Arguments.of(
            "Viewer", SelectScope.ALL, UpdateScope.NONE, UpdateScope.NONE, UpdateScope.NONE),
        Arguments.of("Editor", SelectScope.ALL, UpdateScope.ALL, UpdateScope.ALL, UpdateScope.ALL),
        Arguments.of("Manager", SelectScope.ALL, UpdateScope.ALL, UpdateScope.ALL, UpdateScope.ALL),
        Arguments.of("Owner", SelectScope.ALL, UpdateScope.ALL, UpdateScope.ALL, UpdateScope.ALL),
        Arguments.of(
            "Exists", SelectScope.EXISTS, UpdateScope.NONE, UpdateScope.NONE, UpdateScope.NONE),
        Arguments.of(
            "Range", SelectScope.RANGE, UpdateScope.NONE, UpdateScope.NONE, UpdateScope.NONE),
        Arguments.of(
            "Aggregator",
            SelectScope.AGGREGATE,
            UpdateScope.NONE,
            UpdateScope.NONE,
            UpdateScope.NONE),
        Arguments.of(
            "Count", SelectScope.COUNT, UpdateScope.NONE, UpdateScope.NONE, UpdateScope.NONE));
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
    assertTrue(p.select().contains(select), roleName + " select must contain " + select);
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
        new TablePermission(
            null,
            TABLE_B,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.NONE,
            UpdateScope.NONE,
            UpdateScope.ALL,
            false,
            false));
    schema.addMember(USER_VIEWER, "ActiveUserRole");

    database.setActiveUser(USER_VIEWER);
    List<TablePermission> perms = database.getSchema(SCHEMA).getPermissionsForActiveUser();
    TablePermission p =
        perms.stream()
            .filter(perm -> TABLE_B.equals(perm.table()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No permission entry for " + TABLE_B));
    assertTrue(p.select().contains(SelectScope.ALL));
    assertEquals(UpdateScope.ALL, p.delete());
    assertEquals(UpdateScope.NONE, p.insert());
    assertEquals(UpdateScope.NONE, p.update());
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
        perms.stream()
            .noneMatch(
                p ->
                    p.insert() != UpdateScope.NONE
                        || p.update() != UpdateScope.NONE
                        || p.delete() != UpdateScope.NONE),
        "User with empty role should have no write permissions");
    assertTrue(
        perms.stream()
            .allMatch(
                p ->
                    p.select().isEmpty()
                        || p.select().contains(SelectScope.EXISTS)
                        || p.select().contains(SelectScope.ALL)),
        "User with empty role should have at most EXISTS-level access");
  }

  @Test
  void multipleGrantsOnSameTableAreMergedForActiveUser() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("MergeGrantRole");
    schema.grant(
        "MergeGrantRole",
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.ALL,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false));
    schema.addMember(USER_EDITOR, "MergeGrantRole");

    database.setActiveUser(USER_EDITOR);
    List<TablePermission> perms = database.getSchema(SCHEMA).getPermissionsForActiveUser();
    TablePermission merged =
        perms.stream()
            .filter(p -> TABLE_A.equals(p.table()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No permission entry for " + TABLE_A));
    assertTrue(merged.select().contains(SelectScope.ALL));
    assertEquals(UpdateScope.ALL, merged.insert());
    assertEquals(UpdateScope.NONE, merged.update());
    assertEquals(UpdateScope.NONE, merged.delete());

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

    // anonymous gets Viewer, so select on all tables
    schema.addMember("anonymous", "Viewer");

    // custom role grants select+insert on TABLE_A
    schema.createRole("InsertOnly");
    schema.grant(
        "InsertOnly",
        new TablePermission(
            null,
            TABLE_A,
            TablePermission.singletonSelect(SelectScope.ALL),
            UpdateScope.ALL,
            UpdateScope.NONE,
            UpdateScope.NONE,
            false,
            false));
    schema.addMember(USER_VIEWER, "InsertOnly");

    // every user inherits anonymous privileges via PostgreSQL role inheritance,
    // so the user should see merged permissions: select+insert from InsertOnly role
    database.setActiveUser(USER_VIEWER);
    List<TablePermission> perms = database.getSchema(SCHEMA).getPermissionsForActiveUser();
    TablePermission merged =
        perms.stream()
            .filter(p -> TABLE_A.equals(p.table()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No permission entry for " + TABLE_A));
    assertTrue(
        merged.select().contains(SelectScope.ALL), "select should be present from InsertOnly role");
    assertEquals(UpdateScope.ALL, merged.insert(), "insert should be present from InsertOnly role");
    assertEquals(UpdateScope.NONE, merged.update());
    assertEquals(UpdateScope.NONE, merged.delete());

    // clean up to avoid leaking anonymous Viewer into other tests
    database.becomeAdmin();
    schema.removeMember("anonymous");
    schema.removeMember(USER_VIEWER);
    schema.deleteRole("InsertOnly");
  }
}
