package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestTableLevelSecurity {

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

  @AfterAll
  static void tearDown() {
    database.becomeAdmin();
    for (String user : List.of(USER_VIEWER, USER_EDITOR, USER_NO_ACCESS)) {
      if (database.hasUser(user)) database.removeUser(user);
    }
    database.dropSchema(SCHEMA);
  }

  // --- Role lifecycle ---

  @Test
  void createAndDeleteRole() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);

    schema.createRole("ReaderA", "Can only read TableA");

    Role info = schema.getRoleInfo("ReaderA");
    assertEquals("ReaderA", info.name());
    assertEquals("Can only read TableA", info.description());
    assertFalse(info.isSystemRole());

    schema.deleteRole("ReaderA");
    // After deletion the role must no longer appear in the role list
    assertFalse(schema.getRoleInfos().stream().anyMatch(r -> r.name().equals("ReaderA")));
  }

  @Test
  void cannotCreateSystemRole() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    assertThrows(MolgenisException.class, () -> schema.createRole("Viewer", null));
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
    // SqlRoleManager rejects grants to system roles directly
    assertThrows(
        MolgenisException.class,
        () ->
            (SqlDatabase)
                database
                    .getRoleManager()
                    .grant(
                        SCHEMA,
                        "Viewer",
                        new TablePermission(TABLE_A, Privileges.VIEWER, null, null, null)));
  }

  @Test
  void cannotGrantToNonExistentRole() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    assertThrows(
        MolgenisException.class,
        () ->
            schema.grant(
                "NonExistentRole",
                new TablePermission(TABLE_A, Privileges.VIEWER, null, null, null)));
  }

  @Test
  void cannotGrantToNonExistentTable() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("TempRole", null);
    try {
      assertThrows(
          MolgenisException.class,
          () ->
              schema.grant(
                  "TempRole",
                  new TablePermission("NoSuchTable", Privileges.VIEWER, null, null, null)));
    } finally {
      schema.deleteRole("TempRole");
    }
  }

  // --- Permission metadata ---

  @Test
  void getRoleInfoReturnsGrantedPermissions() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("MetaRole", "test");
    try {
      schema.grant("MetaRole", new TablePermission(TABLE_A, Privileges.VIEWER, null, true, null));

      Role info = schema.getRoleInfo("MetaRole");
      assertEquals(1, info.permissions().size());

      TablePermission p = info.permissions().get(0);
      assertEquals(TABLE_A, p.table());
      assertEquals(Privileges.VIEWER, p.select());
      assertEquals(Boolean.TRUE, p.update());
      assertNull(p.insert());
      assertNull(p.delete());
    } finally {
      schema.deleteRole("MetaRole");
    }
  }

  @Test
  void getRoleInfosIncludesCustomRoles() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ListedRole", null);
    try {
      List<Role> all = schema.getRoleInfos();
      assertTrue(all.stream().anyMatch(r -> r.name().equals("ListedRole")));
    } finally {
      schema.deleteRole("ListedRole");
    }
  }

  // --- Access control enforcement ---

  @Test
  void userWithViewerRoleCanSelectGrantedTable() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ViewerRole", null);
    schema.grant("ViewerRole", new TablePermission(TABLE_A, Privileges.VIEWER, null, null, null));
    schema.addMember(USER_VIEWER, "ViewerRole");

    try {
      database.setActiveUser(USER_VIEWER);
      database.tx(
          db -> {
            List<Row> rows = db.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows();
            assertEquals(1, rows.size());
          });
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_VIEWER);
      schema.deleteRole("ViewerRole");
    }
  }

  @Test
  void userWithoutGrantCannotSelectTable() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("NoTableRole", null);
    schema.addMember(USER_NO_ACCESS, "NoTableRole");

    try {
      database.setActiveUser(USER_NO_ACCESS);
      database.tx(
          db -> {
            assertThrows(
                Exception.class, () -> db.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows());
          });
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_NO_ACCESS);
      schema.deleteRole("NoTableRole");
    }
  }

  @Test
  void userCanOnlySeeGrantedTables() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("PartialRole", null);
    // Grant access to TABLE_A only
    schema.grant("PartialRole", new TablePermission(TABLE_A, Privileges.VIEWER, null, null, null));
    schema.addMember(USER_VIEWER, "PartialRole");

    try {
      database.setActiveUser(USER_VIEWER);
      database.tx(
          db -> {
            // TABLE_A: should succeed
            assertDoesNotThrow(() -> db.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows());
            // TABLE_B: should fail – no grant
            assertThrows(
                Exception.class, () -> db.getSchema(SCHEMA).getTable(TABLE_B).retrieveRows());
          });
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_VIEWER);
      schema.deleteRole("PartialRole");
    }
  }

  @Test
  void userWithInsertPermissionCanInsertRows() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("EditorRole", null);
    schema.grant("EditorRole", new TablePermission(TABLE_A, Privileges.VIEWER, true, true, true));
    schema.addMember(USER_EDITOR, "EditorRole");

    try {
      database.setActiveUser(USER_EDITOR);
      database.tx(
          db -> {
            db.getSchema(SCHEMA)
                .getTable(TABLE_A)
                .insert(new Row().setString("id", "r_editor").setString("value", "inserted"));
          });

      database.becomeAdmin();
      List<Row> rows = schema.getTable(TABLE_A).retrieveRows();
      assertTrue(rows.stream().anyMatch(r -> "r_editor".equals(r.getString("id"))));

      // Cleanup
      schema.getTable(TABLE_A).delete(new Row().setString("id", "r_editor"));
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_EDITOR);
      schema.deleteRole("EditorRole");
    }
  }

  @Test
  void userWithoutWritePermissionCannotInsertRows() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ReadOnlyRole", null);
    // Only SELECT – no insert/update/delete
    schema.grant("ReadOnlyRole", new TablePermission(TABLE_A, Privileges.VIEWER, null, null, null));
    schema.addMember(USER_EDITOR, "ReadOnlyRole");

    try {
      database.setActiveUser(USER_EDITOR);
      database.tx(
          db -> {
            assertThrows(
                Exception.class,
                () ->
                    db.getSchema(SCHEMA)
                        .getTable(TABLE_A)
                        .insert(new Row().setString("id", "fail").setString("value", "x")));
          });
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_EDITOR);
      schema.deleteRole("ReadOnlyRole");
    }
  }

  // --- Revoke ---

  @Test
  void revokeRemovesTableAccess() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("RevokeRole", null);
    schema.grant("RevokeRole", new TablePermission(TABLE_A, Privileges.VIEWER, null, null, null));
    schema.addMember(USER_VIEWER, "RevokeRole");

    // Verify access exists
    database.setActiveUser(USER_VIEWER);
    database.tx(
        db -> assertDoesNotThrow(() -> db.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows()));

    // Revoke
    database.becomeAdmin();
    schema.revoke("RevokeRole", TABLE_A);

    // Verify access is gone
    database.setActiveUser(USER_VIEWER);
    database.tx(
        db ->
            assertThrows(
                Exception.class, () -> db.getSchema(SCHEMA).getTable(TABLE_A).retrieveRows()));

    database.becomeAdmin();
    schema.removeMember(USER_VIEWER);
    schema.deleteRole("RevokeRole");
  }

  // --- getPermissionsForActiveUser ---

  @Test
  void getPermissionsForActiveUserReturnsCorrectPermissions() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("ActiveUserRole", null);
    schema.grant(
        "ActiveUserRole", new TablePermission(TABLE_B, Privileges.VIEWER, null, null, true));
    schema.addMember(USER_VIEWER, "ActiveUserRole");

    try {
      database.setActiveUser(USER_VIEWER);
      database.tx(
          db -> {
            List<TablePermission> perms = db.getSchema(SCHEMA).getPermissionsForActiveUser();
            assertEquals(1, perms.size());
            TablePermission p = perms.getFirst();
            assertEquals(TABLE_B, p.table());
            assertEquals(Privileges.VIEWER, p.select());
            assertEquals(Boolean.TRUE, p.delete());
            assertNull(p.insert());
            assertNull(p.update());
          });
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_VIEWER);
      schema.deleteRole("ActiveUserRole");
    }
  }

  // --- System role permissions ---

  @Test
  void systemRoleReturnsWildcardPermissions() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    Role viewerInfo = schema.getRoleInfo("Viewer");
    assertTrue(viewerInfo.system());
    assertEquals(1, viewerInfo.permissions().size());
    TablePermission p = viewerInfo.permissions().get(0);
    assertEquals("*", p.table());
    assertEquals(Privileges.VIEWER, p.select());
  }

  // --- Require manager to manage roles ---

  @Test
  void nonManagerCannotCreateRole() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.addMember(USER_NO_ACCESS, Privileges.VIEWER.toString());

    try {
      database.setActiveUser(USER_NO_ACCESS);
      assertThrows(MolgenisException.class, () -> schema.createRole("Forbidden", null));
    } finally {
      database.becomeAdmin();
      schema.removeMember(USER_NO_ACCESS);
    }
  }
}
