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
import static org.molgenis.sql.SqlTable.MG_ROLE_PREFIX;

public class TestRoles {
  private static Database database;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    database = DatabaseFactory.getDatabase("molgenis", "molgenis");
  }

  @Test
  public void testRolePermissions() throws MolgenisException {

    StopWatch.start("start: testRolePermissions()");

    // createColumn some schema to test with
    Schema schema = database.createSchema("testRolePermissions");

    // createColumn test users
    database.createUser("user_testRolePermissions_viewer");
    database.createUser("user_testRolePermissions_editor");
    database.createUser("user_testRolePermissions_manager");

    // grant proper roles
    schema.grantView("user_testRolePermissions_viewer");
    schema.grantEdit("user_testRolePermissions_editor");
    schema.grantManage("user_testRolePermissions_manager");

    StopWatch.print("testRolePermissions schema created");

    // test that viewer and editor cannot createColumn, and manager can
    try {
      database.transaction(
          "user_testRolePermissions_viewer",
          db -> {
            db.getSchema("testRolePermissions").createTableIfNotExists("Test");
            fail("role(viewers) should not be able to createColumn tables"); // should not happen
          });
    } catch (Exception e) {
    }

    StopWatch.print("test viewer permission");

    try {
      database.transaction(
          "user_testRolePermissions_editor",
          db -> {
            db.getSchema("testRolePermissions").createTableIfNotExists("Test");
            fail("role(editors) should not be able to createColumn tables"); // should not happen
          });
    } catch (Exception e) {
    }
    StopWatch.print("test editor permission");

    try {
      database.transaction(
          "user_testRolePermissions_manager",
          db -> {
            try {
              db.getSchema("testRolePermissions").createTableIfNotExists("Test");
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
      database.transaction(
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
    Schema schema = database.createSchema("testRole");
    database.createUser("testadmin");
    database.createUser("testuser");
    schema.grantAdmin("testadmin");
    schema
        .createTableIfNotExists("Person")
        .addColumn("FirstName", STRING)
        .addColumn("LastName", STRING);

    try {
      database.transaction(
          MG_ROLE_PREFIX + "TESTROLE_VIEW",
          db -> {
            db.getSchema("testRole").createTableIfNotExists("Test");
          });
      // should throw exception, otherwise fail
      fail();
    } catch (Exception e) {
      // this is expected
    }

    try {
      database.transaction(
          "testadmin",
          db -> {
            db.getSchema("testRole").createTableIfNotExists("Test");
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
    Schema s = database.createSchema("TestRLS");
    // createColumn two users
    database.createUser("testrls1");
    database.createUser("testrls2");
    database.createUser("testrlsnopermission");
    database.createUser("testrls_has_rls_view");
    // grant both admin on TestRLS schema so can add row level security
    s.grantAdmin("testrls1");
    s.grantAdmin("testrls2");
    s.grantView("testrls_has_rls_view"); // can view table but only rows with right RLS

    // let one user createColumn the table
    database.transaction(
        "testrls1",
        db -> {
          db.getSchema("TestRLS").createTableIfNotExists("TestRLS").addColumn("col1", STRING);
        });

    // let the other add RLS
    database.transaction(
        "testrls2",
        db -> {
          db.getSchema("TestRLS").getTable("TestRLS").enableRowLevelSecurity();
        });

    // let the first add a row (checks if admin permissions are setup correctly)
    database.transaction(
        "testrls1",
        db -> {
          db.getSchema("TestRLS")
              .getTable("TestRLS")
              .insert(
                  new Row()
                      .setString("col1", "Hello World")
                      .set(SqlTable.MG_EDIT_ROLE, "testrls_has_rls_view"),
                  new Row()
                      .setString("col1", "Hello World2")
                      .set(SqlTable.MG_EDIT_ROLE, "testrlsnopermission"));
        });

    // let the second admin see it
    database.transaction(
        "testrls2",
        db -> {
          assertEquals(2, db.getSchema("TestRLS").getTable("TestRLS").retrieve().size());
        });

    // have RLS user query and see one row
    database.transaction(
        "testrls_has_rls_view",
        db -> {
          assertEquals(1, db.getSchema("TestRLS").getTable("TestRLS").retrieve().size());
        });
  }
}
