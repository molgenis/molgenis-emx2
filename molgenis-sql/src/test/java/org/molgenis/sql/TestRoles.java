package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.Database;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.Table;
import org.molgenis.Row;
import org.molgenis.utils.StopWatch;

import java.sql.SQLException;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.molgenis.Type.STRING;

public class TestRoles {
  private static Database db;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    db = DatabaseFactory.getDatabase("molgenis", "molgenis");
  }

  @Test
  public void testRolePermissions() throws MolgenisException {

    StopWatch.start("start: testRolePermissions()");

    // createColumn some schema to test with
    Schema s = db.createSchema("testRolePermissions");

    // createColumn test users
    db.createUser("user_testRolePermissions_viewer");
    db.createUser("user_testRolePermissions_editor");
    db.createUser("user_testRolePermissions_manager");

    // grant proper roles
    s.grantView("user_testRolePermissions_viewer");
    s.grantEdit("user_testRolePermissions_editor");
    s.grantManage("user_testRolePermissions_manager");

    StopWatch.print("testRolePermissions schema created");

    // test that viewer and editor cannot createColumn, and manager can
    try {
      db.transaction(
          "user_testRolePermissions_viewer",
          db -> {
            db.getSchema("testRolePermissions").createTable("Test");
            fail("role(viewers) should not be able to createColumn tables"); // should not happen
          });
    } catch (Exception e) {
    }

    StopWatch.print("test viewer permission");

    try {
      db.transaction(
          "user_testRolePermissions_editor",
          db -> {
            db.getSchema("testRolePermissions").createTable("Test");
            fail("role(editors) should not be able to createColumn tables"); // should not happen
          });
    } catch (Exception e) {
    }
    StopWatch.print("test editor permission");

    try {
      db.transaction(
          "user_testRolePermissions_manager",
          db -> {
            try {
              db.getSchema("testRolePermissions").createTable("Test");
            } catch (Exception e) {
              e.printStackTrace();
              throw e;
            }
          });
    } catch (Exception e) {
      fail("role(manager) should be able to createColumn tables"); // should not happen
      throw e;
    }
    StopWatch.print("test manager permission -> created a table");

    // test that all can query
    try {
      db.transaction(
          "user_testRolePermissions_viewer",
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
    }
    StopWatch.print("test viewer query");
  }

  @Test
  public void testRole() throws MolgenisException {
    Schema s = db.createSchema("testRole");
    db.createUser("testadmin");
    db.createUser("testuser");
    s.grantAdmin("testadmin");
    s.createTable("Person").addColumn("FirstName", STRING).addColumn("LastName", STRING);

    try {
      db.transaction(
          "MGROLE_TESTROLE_VIEW",
          db -> {
            db.getSchema("testRole").createTable("Test");
          });
      // should throw exception, otherwise fail
      fail();
    } catch (Exception e) {
      // this is expected
    }

    try {
      db.transaction(
          "testadmin",
          db -> {
            db.getSchema("testRole").createTable("Test");
            // this is soo cooool
            db.getSchema("testRole").grantView("testuser");
          });

    } catch (Exception e) {
      // this is NOT expected
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testRls() throws MolgenisException {
    // createColumn schema
    Schema s = db.createSchema("TestRLS");
    // createColumn two users
    db.createUser("testrls1");
    db.createUser("testrls2");
    db.createUser("testrlsnopermission");
    db.createUser("testrls_has_rls_view");
    // grant both admin on TestRLS schema so can add row level security
    s.grantAdmin("testrls1");
    s.grantAdmin("testrls2");
    s.grantView("testrls_has_rls_view"); // can view table but only rows with right RLS

    // let one user createColumn the table
    db.transaction(
        "testrls1",
        db -> {
          db.getSchema("TestRLS").createTable("TestRLS").addColumn("col1", STRING);
        });

    // let the other add RLS
    db.transaction(
        "testrls2",
        db -> {
          db.getSchema("TestRLS").getTable("TestRLS").enableRowLevelSecurity();
        });

    // let the first add a row (checks if admin permissions are setup correctly)
    db.transaction(
        "testrls1",
        db -> {
          db.getSchema("TestRLS")
              .getTable("TestRLS")
              .insert(
                  new Row().setString("col1", "Hello World").setRowEditRole("testrls_has_rls_view"),
                  new Row()
                      .setString("col1", "Hello World2")
                      .setRowEditRole("testrlsnopermission"));
        });

    // let the second admin see it
    db.transaction(
        "testrls2",
        db -> {
          assertEquals(2, db.getSchema("TestRLS").getTable("TestRLS").retrieve().size());
        });

    // have RLS user query and see one row
    db.transaction(
        "testrls_has_rls_view",
        db -> {
          assertEquals(1, db.getSchema("TestRLS").getTable("TestRLS").retrieve().size());
        });
  }
}
