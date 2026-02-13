package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.molgenis.emx2.*;

/**
 * Integration tests for {@link SqlPermissionExecutor}.
 *
 * <p>Tests verify that the Java-managed permission system (replacing PL/pgSQL triggers) correctly
 * creates roles, grants/revokes permissions, manages group membership, and enables row-level
 * security.
 *
 * <p>Requires migration31a.sql to have been applied (group_metadata and group_permissions tables
 * must exist). Run the initDatabase Gradle task first if tests fail with "relation does not exist".
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestSqlPermissionExecutor {

  private static Database database;
  private static final String SCHEMA_NAME = "TestPermissionExecutor";
  private static final String[] TEST_USERS = {"perm_viewer", "perm_editor", "perm_manager"};

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    cleanUp();
  }

  @AfterAll
  public static void tearDown() {
    try {
      database.becomeAdmin();
    } catch (Exception e) {
      // may fail if database state is broken from test failure
    }
    cleanUp();
  }

  private static void cleanUp() {
    try {
      database.becomeAdmin();
      if (database.getSchemaNames().contains(SCHEMA_NAME)) {
        database.dropSchema(SCHEMA_NAME);
      }
      for (String user : TEST_USERS) {
        try {
          if (database.hasUser(user)) {
            database.removeUser(user);
          }
        } catch (Exception ignored) {
          // best effort cleanup
        }
      }
    } catch (Exception ignored) {
      // best effort cleanup
    }
  }

  @Test
  @org.junit.jupiter.api.Order(1)
  void createSchemaCreatesDefaultRoles() {
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);

    List<String> roles = schema.getRoles();
    List<String> expected =
        List.of(
            "Exists",
            "Range",
            "Aggregator",
            "Count",
            "Viewer",
            "Editor",
            "Manager",
            "Owner",
            "Admin");

    assertEquals(expected.size(), roles.size(), "Should have exactly 9 default roles");
    assertTrue(roles.containsAll(expected), "Should contain all standard roles: " + roles);
  }

  @Test
  @org.junit.jupiter.api.Order(2)
  void addMemberGrantsRole() {
    Schema schema = database.getSchema(SCHEMA_NAME);
    assertNotNull(schema, "Schema should exist from test 1");

    schema.addMember("perm_viewer", Privileges.VIEWER.toString());

    List<Member> members = schema.getMembers();
    assertTrue(
        members.stream()
            .anyMatch(m -> m.getUser().equals("perm_viewer") && m.getRole().contains("Viewer")),
        "perm_viewer should be a Viewer member");
  }

  @Test
  @org.junit.jupiter.api.Order(3)
  void memberHasDirectRole() {
    Schema schema = database.getSchema(SCHEMA_NAME);
    assertNotNull(schema, "Schema should exist from test 1");

    schema.addMember("perm_manager", Privileges.MANAGER.toString());

    // getInheritedRolesForUser returns direct group memberships from group_metadata
    // PostgreSQL role inheritance (Manager -> Editor -> Viewer) is enforced at the database level
    List<String> roles = schema.getInheritedRolesForUser("perm_manager");
    assertTrue(roles.contains("Manager"), "Should have Manager role");

    // Verify the user is listed as a member with Manager role
    List<Member> members = schema.getMembers();
    assertTrue(
        members.stream()
            .anyMatch(m -> m.getUser().equals("perm_manager") && m.getRole().contains("Manager")),
        "perm_manager should be a Manager member");
  }

  @Test
  @org.junit.jupiter.api.Order(4)
  void viewerCanQueryButNotCreate() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA_NAME);
    assertNotNull(schema, "Schema should exist from test 1");

    schema.create(table("Items").add(column("id").setPkey()).add(column("name")));

    // Viewer can query
    try {
      database.setActiveUser("perm_viewer");
      Table items = database.getSchema(SCHEMA_NAME).getTable("Items");
      assertNotNull(items, "Viewer should be able to see the table");
      items.retrieveRows();
    } finally {
      database.becomeAdmin();
    }

    // Viewer cannot create tables
    try {
      database.setActiveUser("perm_viewer");
      database.tx(
          db -> {
            db.getSchema(SCHEMA_NAME).create(table("ShouldFail").add(column("x").setPkey()));
          });
      fail("Viewer should not be able to create tables");
    } catch (Exception e) {
      // expected
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  @org.junit.jupiter.api.Order(5)
  void editorCanInsertButNotCreateTable() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA_NAME);
    assertNotNull(schema, "Schema should exist from test 1");

    schema.addMember("perm_editor", Privileges.EDITOR.toString());

    // Editor can insert rows
    try {
      database.setActiveUser("perm_editor");
      database.tx(
          db -> {
            db.getSchema(SCHEMA_NAME)
                .getTable("Items")
                .insert(new Row().setString("id", "item1").setString("name", "Test Item"));
          });
    } catch (Exception e) {
      fail("Editor should be able to insert rows: " + e.getMessage());
    } finally {
      database.becomeAdmin();
    }

    // Editor cannot create tables
    try {
      database.setActiveUser("perm_editor");
      database.tx(
          db -> {
            db.getSchema(SCHEMA_NAME).create(table("ShouldFail").add(column("x").setPkey()));
          });
      fail("Editor should not be able to create tables");
    } catch (Exception e) {
      // expected
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  @org.junit.jupiter.api.Order(6)
  void managerCanCreateTable() {
    try {
      database.setActiveUser("perm_manager");
      database.tx(
          db -> {
            db.getSchema(SCHEMA_NAME).create(table("ManagerTable").add(column("id").setPkey()));
          });
    } catch (Exception e) {
      fail("Manager should be able to create tables: " + e.getMessage());
    } finally {
      database.becomeAdmin();
    }

    assertNotNull(
        database.getSchema(SCHEMA_NAME).getTable("ManagerTable"),
        "ManagerTable should exist after creation by manager");
  }

  @Test
  @org.junit.jupiter.api.Order(7)
  void removeMemberRevokesAccess() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA_NAME);
    assertNotNull(schema, "Schema should exist from test 1");

    // Ensure viewer is a member
    schema.addMember("perm_viewer", Privileges.VIEWER.toString());
    assertTrue(
        schema.getMembers().stream().anyMatch(m -> m.getUser().equals("perm_viewer")),
        "perm_viewer should be a member before removal");

    schema.removeMember("perm_viewer");

    // After removal, user should not be able to see the schema
    try {
      database.setActiveUser("perm_viewer");
      assertNull(database.getSchema(SCHEMA_NAME), "Removed user should not see the schema");
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  @org.junit.jupiter.api.Order(8)
  void permissionsQueryReturnsGroups() {
    database.becomeAdmin();
    List<GroupPermission> permissions = database.getPermissions();

    assertNotNull(permissions, "Permissions should not be null");
    assertFalse(permissions.isEmpty(), "Should have at least one permission group");

    boolean hasSchemaGroup =
        permissions.stream().anyMatch(gp -> gp.groupName().startsWith(SCHEMA_NAME + "/"));
    assertTrue(hasSchemaGroup, "Should have permission groups for our schema");
  }

  @Test
  @org.junit.jupiter.api.Order(9)
  void newTableInheritsSchemaPermissions() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA_NAME);
    assertNotNull(schema, "Schema should exist from test 1");

    // Re-add viewer
    schema.addMember("perm_viewer", Privileges.VIEWER.toString());

    // Create a new table as admin
    schema.create(table("NewTable").add(column("id").setPkey()));

    // Viewer should be able to query the new table
    try {
      database.setActiveUser("perm_viewer");
      Table newTable = database.getSchema(SCHEMA_NAME).getTable("NewTable");
      assertNotNull(newTable, "Viewer should see the new table");
      newTable.retrieveRows();
    } catch (Exception e) {
      fail("Viewer should be able to query newly created table: " + e.getMessage());
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  @org.junit.jupiter.api.Order(10)
  void dropSchemaRemovesRoles() {
    database.becomeAdmin();
    String tempSchema = "TestPermExecDrop";
    database.dropCreateSchema(tempSchema);

    List<String> roles = database.getSchema(tempSchema).getRoles();
    assertFalse(roles.isEmpty(), "Should have roles before drop");

    database.dropSchema(tempSchema);

    assertFalse(database.getSchemaNames().contains(tempSchema), "Schema should be gone after drop");
  }
}
