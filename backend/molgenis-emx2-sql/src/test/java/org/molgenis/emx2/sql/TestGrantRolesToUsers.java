package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Privileges.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.StopWatch;

public class TestGrantRolesToUsers {
  public static final String TEST_ROLE = "testRole";
  private static Database database;

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  @Tag("windowsFail")
  public void testGrantRevokeMembership() {

    Schema schema = database.dropCreateSchema("testGrantRevokeMembership");
    List first = Arrays.asList("Aggregator", "Viewer", "Editor", "Manager", "Owner");
    List second = schema.getRoles();
    assertTrue(
        first.size() == second.size() && first.containsAll(second) && second.containsAll(first));

    schema.addMember("user1", "Viewer");
    assertEquals(1, schema.getMembers().size());

    schema.addMember("user1", "Editor"); // should override previous
    assertEquals(1, schema.getMembers().size());
    assertEquals("Editor", schema.getRoleForUser("user1"));

    schema.removeMember("user1");

    assertEquals(0, schema.getMembers().size());
  }

  @Test
  public void testRolePermissions() {

    StopWatch.start("start: testRolePermissions()");

    // createColumn some schema to test with
    Schema schema = database.dropCreateSchema("testRolePermissions");

    // test that admin has all roles
    List<String> roles = schema.getInheritedRolesForActiveUser();
    assertTrue(roles.contains(VIEWER.toString()));
    assertTrue(roles.contains(EDITOR.toString()));
    assertTrue(roles.contains(MANAGER.toString()));
    assertTrue(roles.contains(OWNER.toString()));

    // createColumn test users
    database.addUser("user_testRolePermissions_viewer");
    database.addUser("user_testRolePermissions_editor");
    database.addUser("user_testRolePermissions_manager");

    // grant proper roles
    schema.addMember("user_testRolePermissions_viewer", VIEWER.toString());
    schema.addMember("user_testRolePermissions_editor", EDITOR.toString());
    schema.addMember("user_testRolePermissions_manager", MANAGER.toString());

    // test that manager also had editor and viewer roles
    roles = schema.getInheritedRolesForUser("user_testRolePermissions_manager");
    assertTrue(roles.contains(VIEWER.toString()));
    assertTrue(roles.contains(EDITOR.toString()));
    assertTrue(roles.contains(MANAGER.toString()));

    // test that editor also had editor and viewer roles
    roles = schema.getInheritedRolesForUser("user_testRolePermissions_editor");
    assertTrue(roles.contains(VIEWER.toString()));
    assertTrue(roles.contains(EDITOR.toString()));
    assertFalse(roles.contains(MANAGER.toString()));

    // test that editor also had editor and viewer roles
    roles = schema.getInheritedRolesForUser("user_testRolePermissions_viewer");
    assertTrue(roles.contains(VIEWER.toString()));
    assertFalse(roles.contains(EDITOR.toString()));
    assertFalse(roles.contains(MANAGER.toString()));

    StopWatch.print("testRolePermissions schema created");

    // test that viewer and editor cannot createColumn, and manager can
    try {
      database = new SqlDatabase("user_testRolePermissions_viewer");
      database.tx(
          db -> {
            db.getSchema("testRolePermissions").create(table("Test"));
            fail("role(viewers) should not be able to createColumn tables");
            // should not happen
          });
      database = new SqlDatabase(SqlDatabase.ADMIN_USER);
    } catch (Exception e) {
    }

    StopWatch.print("test editor permission");

    try {
      database = new SqlDatabase("user_testRolePermissions_editor");
      database.tx(
          db -> {
            db.getSchema("testRolePermissions").create(table("Test"));
            fail("role(editors) should not be able to createColumn tables");
            // should not happen
          });
      database = new SqlDatabase(SqlDatabase.ADMIN_USER);
    } catch (Exception e) {
    }
    StopWatch.print("test editor permission success");

    try {
      database = new SqlDatabase("user_testRolePermissions_manager");
      database.tx(
          db -> {
            try {
              db.getSchema("testRolePermissions").create(table("Test"));
              //                  .getMetadata()
              //                  .addColumn("ID", ColumnType.INT);
            } catch (Exception e) {
              e.printStackTrace();
              throw e;
            }
          });
      database = new SqlDatabase(SqlDatabase.ADMIN_USER);
    } catch (Exception e) {
      fail("role(manager) should be able to createColumn tables"); // should not happen
      throw e;
    }
    StopWatch.print("test manager permission -> created a table, success");

    // test that all can query
    try {
      database = new SqlDatabase("user_testRolePermissions_viewer");
      database.tx(
          db -> {
            StopWatch.print("getting Table");
            Table t = db.getSchema("testRolePermissions").getTable("Test");
            StopWatch.print("got table");
            t.retrieveRows();
            StopWatch.print("completed query");
          });
    } catch (Exception e) {
      e.printStackTrace();
      fail("role(viewers) should  be able to query "); // should not happen
    } finally {
      database = new SqlDatabase(SqlDatabase.ADMIN_USER);
    }
    StopWatch.print("test viewer query, success");

    // test that owner manager can assign roles and normal users cant
    try {
      database = new SqlDatabase("user_testRolePermissions_viewer");
      database.tx(
          db -> {
            StopWatch.print("settings permissions Table");
            db.getSchema("testRolePermissions").addMember("fail", VIEWER.toString());
          });
      fail("role(viewers) should not be able to add members");
    } catch (Exception e) {
      // correct
    } finally {
      database = new SqlDatabase(SqlDatabase.ADMIN_USER);
    }

    try {
      database.addUser(
          "user_testRolePermissions_success"); // manager cannot create users, needs elevated
      // privilegs
      database = new SqlDatabase("user_testRolePermissions_manager");
      database.tx(
          db -> {
            StopWatch.print("settings permissions Table");
            db.getSchema("testRolePermissions")
                .addMember("user_testRolePermissions_success", VIEWER.toString());
            db = new SqlDatabase(SqlDatabase.ADMIN_USER);
            db.removeUser("user_testRolePermissions_success");
          });
    } catch (Exception e) {
      e.printStackTrace();
      fail("roles(manager) should be able to assign roles, found exception " + e);
    } finally {
      database = new SqlDatabase(SqlDatabase.ADMIN_USER);
    }
  }

  @Test
  @Tag("windowsFail")
  public void testRole() {
    try {
      Schema schema = database.dropCreateSchema(TEST_ROLE);

      database.addUser("testadmin");
      database.addUser("testuser");

      // should not be able to see as user, until permission (later)
      database = new SqlDatabase("testuser");
      assertNull(schema.getRoleForActiveUser()); // should have no role in this schema
      assertFalse(database.getSchemaNames().contains(TEST_ROLE));
      assertNull(database.getSchema(TEST_ROLE));

      database = new SqlDatabase(SqlDatabase.ADMIN_USER);

      schema.addMember("testadmin", OWNER.toString());
      assertEquals(OWNER.toString(), schema.getRoleForUser("testadmin"));

      assertTrue(schema.getInheritedRolesForUser("testadmin").contains(OWNER.toString()));
      assertEquals(5, schema.getInheritedRolesForUser("testadmin").size());

      database = new SqlDatabase("testadmin");
      schema = database.getSchema(TEST_ROLE);
      assertEquals(OWNER.toString(), schema.getRoleForActiveUser());
      database = new SqlDatabase(SqlDatabase.ADMIN_USER);

      schema.create(
          table("Person")
              .add(column("id").setPkey())
              .add(column("FirstName"))
              .add(column("LastName")));

      try {
        database = new SqlDatabase(Constants.MG_ROLE_PREFIX + "TESTROLE_VIEW");
        database.tx(
            db -> {
              db.getSchema(TEST_ROLE).create(table("Test"));
            });
        // should throw exception, otherwise fail
        fail();
      } catch (MolgenisException e) {
        System.out.println("erorred correclty:\n" + e);
      }
      database = new SqlDatabase(SqlDatabase.ADMIN_USER);

      try {
        database = new SqlDatabase("testadmin");
        database.tx(
            db -> {
              db.getSchema(TEST_ROLE).create(table("Test"));
              // this is soo cooool
              db.getSchema(TEST_ROLE).addMember("testuser", VIEWER.toString());
            });

      } catch (Exception e) {
        // this is NOT expected
        e.printStackTrace();
        fail();
      }
    } finally {
      database = new SqlDatabase(SqlDatabase.ADMIN_USER);
    }
  }

  @Test
  public void testCaseSensitiveSchemaNames() {
    // following bug #405
    Schema s1 = database.dropCreateSchema("testCaseSensitiveSchemaNames");

    // add member to schema
    final String USER = "testCaseSensitiveSchemaNamesMANAGER";
    s1.addMember(USER, MANAGER.toString());
    assertEquals(1, s1.getMembers().size());

    // create another schema with case insensitive equal name
    Schema s2 = database.dropCreateSchema("testCaseSensitiveSchemaNAMES");
    assertEquals(0, s2.getMembers().size());

    // proof that USER can only add tables in one schema/drop in one schema
    database = new SqlDatabase(USER);
    s1 = database.getSchema("testCaseSensitiveSchemaNames");
    s1.create(table("ATable", column("id").setPkey()));

    // proof that user can NOT add in other schema
    try {
      database
          .getSchema("testCaseSensitiveSchemaNAMES")
          .create(table("ATable", column("id").setPkey()));
      fail("user should not be able to add tables in schema it has no permissions on");
    } catch (Exception e) {
      // correct
    }

    // proof user can drop
    database = new SqlDatabase(SqlDatabase.ADMIN_USER); // reset to admin
    database.dropSchema(s1.getName()); // clean up
    database.dropSchema(s2.getName()); // clean up
    database.removeUser(USER);
  }
}
