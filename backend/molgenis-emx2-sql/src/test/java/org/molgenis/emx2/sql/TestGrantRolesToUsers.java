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

  public static final Username VIEWER = new Username("user_testRolePermissions_viewer");
  public static final Username EDITOR = new Username("user_testRolePermissions_editor");
  public static final Username MANAGER = new Username("user_testRolePermissions_manager");

  private static Database database;

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  @Tag("windowsFail")
  public void testGrantRevokeMembership() {

    Schema schema = database.dropCreateSchema("testGrantRevokeMembership");
    List<String> first =
        Arrays.asList(
            "Member",
            "Exists",
            "Range",
            "Aggregator",
            "Count",
            "Viewer",
            "Editor",
            "Manager",
            "Owner");
    List<String> second = schema.getRoles();
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
    database.becomeAdmin(); // admin
    Schema schema = database.dropCreateSchema("testRolePermissions");

    // test that admin has all roles
    List<String> roles = schema.getInheritedRolesForActiveUser();
    assertTrue(roles.contains(Privileges.VIEWER.toString()));
    assertTrue(roles.contains(Privileges.EDITOR.toString()));
    assertTrue(roles.contains(Privileges.MANAGER.toString()));
    assertTrue(roles.contains(OWNER.toString()));

    // createColumn test users
    database.addUser(VIEWER);
    database.addUser(EDITOR);
    database.addUser(MANAGER);

    // grant proper roles
    schema.addMember(VIEWER.getValue(), Privileges.VIEWER.toString());
    schema.addMember(EDITOR.getValue(), Privileges.EDITOR.toString());
    schema.addMember(MANAGER.getValue(), Privileges.MANAGER.toString());

    // test that manager also had editor and viewer roles
    roles = schema.getInheritedRolesForUser(MANAGER.getValue());
    assertTrue(roles.contains(Privileges.VIEWER.toString()));
    assertTrue(roles.contains(Privileges.EDITOR.toString()));
    assertTrue(roles.contains(Privileges.MANAGER.toString()));

    // test that editor also had editor and viewer roles
    roles = schema.getInheritedRolesForUser(EDITOR.getValue());
    assertTrue(roles.contains(Privileges.VIEWER.toString()));
    assertTrue(roles.contains(Privileges.EDITOR.toString()));
    assertFalse(roles.contains(Privileges.MANAGER.toString()));

    // test that editor also had editor and viewer roles
    roles = schema.getInheritedRolesForUser(VIEWER.getValue());
    assertTrue(roles.contains(Privileges.VIEWER.toString()));
    assertFalse(roles.contains(Privileges.EDITOR.toString()));
    assertFalse(roles.contains(Privileges.MANAGER.toString()));

    StopWatch.print("testRolePermissions schema created");

    // test that viewer and editor cannot createColumn, and manager can
    try {
      database.setActiveUser(VIEWER);
      database.tx(
          db -> {
            db.getSchema("testRolePermissions").create(table("Test"));
            fail("role(viewers) should not be able to createColumn tables");
            // should not happen
          });
      database.becomeAdmin();
    } catch (Exception e) {
    }

    StopWatch.print("test editor permission");

    try {
      database.setActiveUser(EDITOR);
      database.tx(
          db -> {
            db.getSchema("testRolePermissions").create(table("Test"));
            fail("role(editors) should not be able to createColumn tables");
            // should not happen
          });
      database.becomeAdmin();
    } catch (Exception e) {
    }
    StopWatch.print("test editor permission success");

    try {
      database.setActiveUser(MANAGER);
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
      database.becomeAdmin();
    } catch (Exception e) {
      fail("role(manager) should be able to createColumn tables"); // should not happen
      throw e;
    }
    StopWatch.print("test manager permission -> created a table, success");

    // test that all can query
    try {
      database.setActiveUser(VIEWER);
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
      database.becomeAdmin();
    }
    StopWatch.print("test viewer query, success");

    // test that owner manager can assign roles and normal users cant
    try {
      database.setActiveUser(VIEWER);
      database.tx(
          db -> {
            StopWatch.print("settings permissions Table");
            db.getSchema("testRolePermissions").addMember("fail", Privileges.VIEWER.toString());
          });
      fail("role(viewers) should not be able to add members");
    } catch (Exception e) {
      // correct
    } finally {
      database.becomeAdmin();
    }

    try {
      Username successUsername = new Username("user_testRolePermissions_success");
      database.addUser(successUsername); // manager cannot create users, needs elevated
      // privilegs
      database.setActiveUser(MANAGER);
      database.tx(
          db -> {
            StopWatch.print("settings permissions Table");
            db.getSchema("testRolePermissions")
                .addMember(successUsername.getValue(), Privileges.VIEWER.toString());
            db.becomeAdmin();
            db.removeUser(successUsername);
          });
    } catch (Exception e) {
      e.printStackTrace();
      fail("roles(manager) should be able to assign roles, found exception " + e);
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  @Tag("windowsFail")
  public void testRole() {
    try {
      Schema schema = database.dropCreateSchema("testRole");
      Username testadmin = new Username("testadmin");
      Username testuser = new Username("testuser");

      database.addUser(testadmin);
      database.addUser(testuser);

      // should not be able to see as user, until permission (later)
      database.setActiveUser(testuser);
      assertNull(schema.getRoleForActiveUser()); // should have no role in this schema
      assertFalse(database.getSchemaNames().contains("testRole"));
      assertNull(database.getSchema("testRole"));

      database.becomeAdmin();

      schema.addMember(testadmin.getValue(), OWNER.toString());
      assertEquals(OWNER.toString(), schema.getRoleForUser(testadmin.getValue()));

      assertTrue(schema.getInheritedRolesForUser(testadmin.getValue()).contains(OWNER.toString()));
      assertEquals(9, schema.getInheritedRolesForUser(testadmin.getValue()).size());

      database.setActiveUser(testadmin);
      assertEquals(OWNER.toString(), schema.getRoleForActiveUser());
      database.becomeAdmin();

      schema.create(
          table("Person")
              .add(column("id").setPkey())
              .add(column("FirstName"))
              .add(column("LastName")));

      try {
        database.setActiveUser(testuser);
        database.tx(
            db -> {
              db.getSchema("testRole").create(table("Test"));
            });
        // should throw exception, otherwise fail
        fail();
      } catch (MolgenisException e) {
        System.out.println("erorred correclty:\n" + e);
      }
      database.becomeAdmin();

      try {
        database.setActiveUser(testadmin);
        database.tx(
            db -> {
              db.getSchema("testRole").create(table("Test"));
              // this is soo cooool
              db.getSchema("testRole").addMember(testuser.getValue(), Privileges.VIEWER.toString());
            });

      } catch (Exception e) {
        // this is NOT expected
        e.printStackTrace();
        fail();
      }
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  public void testCaseSensitiveSchemaNames() {
    // following bug #405
    Schema s1 = database.dropCreateSchema("testCaseSensitiveSchemaNames");

    // add member to schema
    final Username USER = new Username("testCaseSensitiveSchemaNamesMANAGER");
    s1.addMember(USER.getValue(), Privileges.MANAGER.toString());
    assertEquals(1, s1.getMembers().size());

    // create another schema with case insensitive equal name
    Schema s2 = database.dropCreateSchema("testCaseSensitiveSchemaNAMES");
    assertEquals(0, s2.getMembers().size());

    // proof that USER can only add tables in one schema/drop in one schema
    database.setActiveUser(USER);
    s1.create(table("ATable", column("id").setPkey()));

    // proof that user can NOT add in other schema
    try {
      s2.create(table("ATable", column("id").setPkey()));
      fail("user should not be able to add tables in schema it has no permissions on");
    } catch (Exception e) {
      // correct
    }

    // proof user can drop
    database.becomeAdmin(); // reset to admin
    database.dropSchema(s1.getName()); // clean up
    database.dropSchema(s2.getName()); // clean up
    database.removeUser(USER);
  }
}
