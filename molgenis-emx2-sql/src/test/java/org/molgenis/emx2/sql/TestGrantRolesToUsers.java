package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.StopWatch;
import org.molgenis.emx2.utils.MolgenisException;

import java.sql.SQLException;
import java.util.Arrays;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.ColumnType.STRING;

public class TestGrantRolesToUsers {
  private static Database database;

  @BeforeClass
  public static void setUp() throws SQLException {
    database = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testGrantRevokeMembership() {

    Schema schema = database.createSchema("testGrantRevokeMembership");
    assertEquals(Arrays.asList("Viewer", "Editor", "Manager", "Owner"), schema.getRoles());

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
    Schema schema = database.createSchema("testRolePermissions");

    // createColumn test users
    database.addUser("user_testRolePermissions_viewer");
    database.addUser("user_testRolePermissions_editor");
    database.addUser("user_testRolePermissions_manager");

    // grant proper roles
    schema.addMember("user_testRolePermissions_viewer", DefaultRoles.VIEWER.toString());
    schema.addMember("user_testRolePermissions_editor", DefaultRoles.EDITOR.toString());
    schema.addMember("user_testRolePermissions_manager", DefaultRoles.MANAGER.toString());

    StopWatch.print("testRolePermissions schema created");

    // test that viewer and editor cannot createColumn, and manager can
    try {
      database.setActiveUser("user_testRolePermissions_viewer");
      database.transaction(
          db -> {
            db.getSchema("testRolePermissions").createTableIfNotExists("Test");
            fail("role(viewers) should not be able to createColumn tables"); // should not
            // happen
          });
      database.clearActiveUser();
    } catch (Exception e) {
    }

    StopWatch.print("test editor permission");

    try {
      database.setActiveUser("user_testRolePermissions_editor");
      database.transaction(
          db -> {
            db.getSchema("testRolePermissions").createTableIfNotExists("Test");
            fail("role(editors) should not be able to createColumn tables"); // should not
            // happen
          });
      database.clearActiveUser();
    } catch (Exception e) {
    }
    StopWatch.print("test editor permission success");

    try {
      database.setActiveUser("user_testRolePermissions_manager");
      database.transaction(
          db -> {
            try {
              db.getSchema("testRolePermissions").createTableIfNotExists("Test");
              //                  .getMetadata()
              //                  .addColumn("ID", ColumnType.INT);
            } catch (Exception e) {
              e.printStackTrace();
              throw e;
            }
          });
      database.clearActiveUser();
    } catch (Exception e) {
      fail("role(manager) should be able to createColumn tables"); // should not happen
      throw e;
    }
    StopWatch.print("test manager permission -> created a table, success");

    // test that all can query
    try {
      database.setActiveUser("user_testRolePermissions_viewer");
      database.transaction(
          db -> {
            StopWatch.print("getting Table");
            Table t = db.getSchema("testRolePermissions").getTable("Test");
            StopWatch.print("got table");
            t.retrieve();
            StopWatch.print("completed query");
          });
    } catch (Exception e) {
      e.printStackTrace();
      fail("role(viewers) should  be able to query "); // should not happen
    } finally {
      database.clearActiveUser();
    }
    StopWatch.print("test viewer query, success");
  }

  @Test
  public void testRole() {
    try {
      Schema schema = database.createSchema("testRole");
      database.addUser("testadmin");
      database.addUser("testuser");
      schema.addMember("testadmin", DefaultRoles.OWNER.toString());
      assertEquals(DefaultRoles.OWNER.toString(), schema.getRoleForUser("testadmin"));

      schema
          .createTableIfNotExists("Person")
          .getMetadata()
          .addColumn("FirstName", STRING)
          .addColumn("LastName", STRING);

      try {
        database.setActiveUser(Constants.MG_ROLE_PREFIX + "TESTROLE_VIEW");
        database.transaction(
            db -> {
              db.getSchema("testRole").createTableIfNotExists("Test");
            });
        // should throw exception, otherwise fail
        fail();
      } catch (Exception e) {
        // this is expected
      }
      database.clearActiveUser();

      try {
        database.setActiveUser("testadmin");
        database.transaction(
            db -> {
              db.getSchema("testRole").createTableIfNotExists("Test");
              // this is soo cooool
              db.getSchema("testRole").addMember("testuser", DefaultRoles.VIEWER.toString());
            });

      } catch (Exception e) {
        // this is NOT expected
        e.printStackTrace();
        fail();
      }
    } finally {
      database.clearActiveUser();
    }
  }
}
