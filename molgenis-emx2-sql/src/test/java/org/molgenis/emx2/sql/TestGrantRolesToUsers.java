package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.utils.StopWatch;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

public class TestGrantRolesToUsers {
  private static Database database;

  @BeforeClass
  public static void setUp() throws SQLException {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testGrantRevokeMembership() {

    Schema schema = database.createSchema("testGrantRevokeMembership");
    List first = Arrays.asList("Viewer", "Editor", "Manager", "Owner");
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
      database.tx(
          db -> {
            db.getSchema("testRolePermissions").create(table("Test"));
            fail("role(viewers) should not be able to createColumn tables"); // should not
            // happen
          });
      database.clearActiveUser();
    } catch (Exception e) {
    }

    StopWatch.print("test editor permission");

    try {
      database.setActiveUser("user_testRolePermissions_editor");
      database.tx(
          db -> {
            db.getSchema("testRolePermissions").create(table("Test"));
            fail("role(editors) should not be able to createColumn tables"); // should not
            // happen
          });
      database.clearActiveUser();
    } catch (Exception e) {
    }
    StopWatch.print("test editor permission success");

    try {
      database.setActiveUser("user_testRolePermissions_manager");
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
      database.clearActiveUser();
    } catch (Exception e) {
      fail("role(manager) should be able to createColumn tables"); // should not happen
      throw e;
    }
    StopWatch.print("test manager permission -> created a table, success");

    // test that all can query
    try {
      database.setActiveUser("user_testRolePermissions_viewer");
      database.tx(
          db -> {
            StopWatch.print("getting Table");
            Table t = db.getSchema("testRolePermissions").getTable("Test");
            StopWatch.print("got table");
            t.getRows();
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

      schema.create(table("Person").addColumn(column("FirstName")).addColumn(column("LastName")));

      try {
        database.setActiveUser(Constants.MG_ROLE_PREFIX + "TESTROLE_VIEW");
        database.tx(
            db -> {
              db.getSchema("testRole").create(table("Test"));
            });
        // should throw exception, otherwise fail
        fail();
      } catch (MolgenisException e) {
        System.out.println("erorred correclty:\n" + e);
      }
      database.clearActiveUser();

      try {
        database.setActiveUser("testadmin");
        database.tx(
            db -> {
              db.getSchema("testRole").create(table("Test"));
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
