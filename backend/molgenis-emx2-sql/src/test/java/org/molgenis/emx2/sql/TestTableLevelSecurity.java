package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestTableLevelSecurity {

  private static Database database;
  private static final String SCHEMA = "TestTableLevelSecurity";
  private static final String TABLE_A = "TableA";
  private static final String TABLE_B = "TableB";

  private static final String USER_VIEWER = "tls_user_viewer";
  private static final String USER_EDITOR = "tls_user_editor";
  private static final String USER_NO_ACCESS = "tls_user_noaccess";

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
  void cannotGrantToSystemRole() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    TablePermission selectPermission = new TablePermission(TABLE_A).select(true);
    assertThrows(MolgenisException.class, () -> schema.grant("Viewer", selectPermission));
  }

  @Test
  void cannotGrantToNonExistentRole() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    TablePermission selectPermission = new TablePermission(TABLE_A).select(true);
    assertThrows(MolgenisException.class, () -> schema.grant("NonExistentRole", selectPermission));
  }

  @Test
  void cannotGrantToNonExistentTable() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("TempRole");
    TablePermission permission = new TablePermission("NoSuchTable").select(true);
    assertThrows(MolgenisException.class, () -> schema.grant("TempRole", permission));
  }

  @Test
  void getRoleInfoReturnsGrantedPermissions() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("MetaRole");
    schema.grant("MetaRole", new TablePermission(TABLE_A).select(true).update(true));

    Role info = schema.getRoleInfo("MetaRole");
    assertEquals(1, info.permissions().size());

    TablePermission p = info.permissions().getFirst();
    assertEquals(TABLE_A, p.table());
    assertEquals(Boolean.TRUE, p.select());
    assertEquals(Boolean.TRUE, p.update());
    assertNull(p.insert());
    assertNull(p.delete());
  }

  @Test
  void getRoleInfosIncludesCustomRoles() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ListedRole");
    List<Role> all = schema.getRoleInfos();
    assertTrue(all.stream().anyMatch(r -> r.name().equals("ListedRole")));
  }

  @Test
  void userWithViewerRoleCanSelectGrantedTable() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ViewerRole");
    schema.grant("ViewerRole", new TablePermission(TABLE_A).select(true));
    schema.addMember(USER_VIEWER, "ViewerRole");

    database.setActiveUser(USER_VIEWER);

    List<Row> rows = database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows();
    assertEquals(1, rows.size());
  }

  @Test
  void userWithoutGrantCannotSelectTable() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("NoTableRole");
    schema.addMember(USER_NO_ACCESS, "NoTableRole");

    database.setActiveUser(USER_NO_ACCESS);
    assertThrows(
        Exception.class, () -> database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows());
  }

  @Test
  void userCanOnlySeeGrantedTables() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("PartialRole");
    // Grant access to TABLE_A only
    schema.grant("PartialRole", new TablePermission(TABLE_A).select(true));
    schema.addMember(USER_VIEWER, "PartialRole");

    database.setActiveUser(USER_VIEWER);

    // TABLE_A: should succeed
    assertDoesNotThrow(() -> database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows());
    // TABLE_B: should fail – no grant
    assertThrows(
        Exception.class, () -> database.getSchema(SCHEMA).getTable(TABLE_B).retrieveRows());
  }

  @Test
  void userWithInsertPermissionCanInsertRows() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("EditorRole");
    schema.grant(
        "EditorRole",
        new TablePermission(TABLE_A).select(true).insert(true).update(true).delete(true));
    schema.addMember(USER_EDITOR, "EditorRole");

    database.setActiveUser(USER_EDITOR);

    database
        .getSchema(SCHEMA)
        .getTable(TABLE_A)
        .insert(new Row().setString("id", "r_editor").setString("value", "inserted"));

    database.becomeAdmin();
    List<Row> rows = schema.getTable(TABLE_A).retrieveRows();
    assertTrue(rows.stream().anyMatch(r -> "r_editor".equals(r.getString("id"))));

    // Cleanup
    schema.getTable(TABLE_A).delete(new Row().setString("id", "r_editor"));
  }

  @Test
  void userWithoutWritePermissionCannotInsertRows() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ReadOnlyRole");
    // Only SELECT – no insert/update/delete
    schema.grant("ReadOnlyRole", new TablePermission(TABLE_A).select(true));
    schema.addMember(USER_EDITOR, "ReadOnlyRole");

    database.setActiveUser(USER_EDITOR);
    assertThrows(
        Exception.class,
        () ->
            database
                .getSchema(SCHEMA)
                .getTable(TABLE_A)
                .insert(new Row().setString("id", "fail").setString("value", "x")));
  }

  @Test
  void revokeRemovesTableAccess() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("RevokeRole");
    schema.grant("RevokeRole", new TablePermission(TABLE_A).select(true));
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
    // Grant SELECT + INSERT
    schema.grant("PartialRevokeRole", new TablePermission(TABLE_A).select(true).insert(true));
    schema.addMember(USER_EDITOR, "PartialRevokeRole");

    database.setActiveUser(USER_EDITOR);

    database
        .getSchema(SCHEMA)
        .getTable(TABLE_A)
        .insert(new Row().setString("id", "r_partial").setString("value", "v"));

    database.becomeAdmin();
    schema.grant("PartialRevokeRole", new TablePermission(TABLE_A).insert(false));

    database.setActiveUser(USER_EDITOR);

    assertThrows(
        Exception.class,
        () ->
            database
                .getSchema(SCHEMA)
                .getTable(TABLE_A)
                .insert(new Row().setString("id", "r_partial2").setString("value", "v")));

    // Verify SELECT still works
    assertDoesNotThrow(() -> database.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows());

    database.becomeAdmin();
    schema.getTable(TABLE_A).delete(new Row().setString("id", "r_partial"));
    schema.removeMember(USER_EDITOR);
    schema.deleteRole("PartialRevokeRole");
  }

  @Test
  void getPermissionsForActiveUserReturnsCorrectPermissions() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ActiveUserRole");
    schema.grant("ActiveUserRole", new TablePermission(TABLE_B).select(true).delete(true));
    schema.addMember(USER_VIEWER, "ActiveUserRole");

    database.setActiveUser(USER_VIEWER);
    List<TablePermission> perms = database.getSchema(SCHEMA).getPermissionsForActiveUser();
    assertEquals(1, perms.size());
    TablePermission p = perms.getFirst();
    assertEquals(TABLE_B, p.table());
    assertEquals(Boolean.TRUE, p.select());
    assertEquals(Boolean.TRUE, p.delete());
    assertNull(p.insert());
    assertNull(p.update());
  }

  @Test
  void systemRoleReturnsWildcardPermissions() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    Role viewerInfo = schema.getRoleInfo("Viewer");
    assertTrue(viewerInfo.isSystemRole());
    assertEquals(1, viewerInfo.permissions().size());
    TablePermission p = viewerInfo.permissions().getFirst();
    assertEquals("*", p.table());
    assertEquals(Boolean.TRUE, p.select());
  }

  @Test
  void grantIsLostAfterTableDropAndRecreate() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("LifecycleRole");
    schema.grant("LifecycleRole", new TablePermission(TABLE_A).select(true));
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

  @Test
  void multipleGrantsOnSameTableAreMergedForActiveUser() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("MergeGrantRole");
    schema.grant("MergeGrantRole", new TablePermission(TABLE_A).select(true));
    schema.grant("MergeGrantRole", new TablePermission(TABLE_A).insert(true));
    schema.addMember(USER_EDITOR, "MergeGrantRole");

    database.setActiveUser(USER_EDITOR);
    List<TablePermission> perms = database.getSchema(SCHEMA).getPermissionsForActiveUser();
    TablePermission merged =
        perms.stream()
            .filter(p -> TABLE_A.equals(p.table()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No permission entry for " + TABLE_A));
    assertEquals(Boolean.TRUE, merged.select());
    assertEquals(Boolean.TRUE, merged.insert());
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
}
