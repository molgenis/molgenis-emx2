package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.Database;
import org.molgenis.Row;
import org.molgenis.Table;
import org.molgenis.Schema;
import org.molgenis.utils.MolgenisException;
import org.molgenis.utils.StopWatch;

import java.sql.SQLException;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.molgenis.Permission.*;
import static org.molgenis.Type.STRING;
import static org.molgenis.sql.SqlTable.MG_ROLE_PREFIX;

public class TestRoles {
  private static Database database;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    database = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testRolePermissions() throws MolgenisException {

    StopWatch.start("start: testRolePermissions()");

    // createColumn some schema to test with
    Schema schema = database.createSchema("testRolePermissions");

    // createColumn test users
    database.addUser("user_testRolePermissions_viewer");
    database.addUser("user_testRolePermissions_editor");
    database.addUser("user_testRolePermissions_manager");

    // grant proper roles
    schema.grant(VIEW, "user_testRolePermissions_viewer");
    schema.grant(EDIT, "user_testRolePermissions_editor");
    schema.grant(MANAGE, "user_testRolePermissions_manager");

    StopWatch.print("testRolePermissions schema created");

    // test that viewer and editor cannot createColumn, and manager can
    try {
      database.transaction(
          "user_testRolePermissions_viewer",
          db -> {
            db.getSchema("testRolePermissions").createTableIfNotExists("Test");
            fail("role(viewers) should not be able to createColumn tables"); // should not
            // happen
          });
    } catch (Exception e) {
    }

    StopWatch.print("test viewer permission");

    try {
      database.transaction(
          "user_testRolePermissions_editor",
          db -> {
            db.getSchema("testRolePermissions").createTableIfNotExists("Test");
            fail("role(editors) should not be able to createColumn tables"); // should not
            // happen
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
    database.addUser("testadmin");
    database.addUser("testuser");
    schema.grant(ADMIN, "testadmin");
    schema
        .createTableIfNotExists("Person")
        .getMetadata()
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
            db.getSchema("testRole").grant(VIEW, "testuser");
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
    database.addUser("testrls1");
    database.addUser("testrls2");
    database.addUser("testrlsnopermission");
    database.addUser("testrls_has_rls_view");
    // grant both admin on TestRLS schema so can add row level security
    s.grant(ADMIN, "testrls1");
    s.grant(ADMIN, "testrls2");
    s.grant(VIEW, "testrls_has_rls_view"); // can view table but only rows with right RLS

    // let one user createColumn the table
    database.transaction(
        "testrls1",
        db -> {
          db.getSchema("TestRLS")
              .createTableIfNotExists("TestRLS")
              .getMetadata()
              .addColumn("col1", STRING);
        });

    // let the other add RLS
    database.transaction(
        "testrls2",
        db -> {
          db.getSchema("TestRLS").getTable("TestRLS").getMetadata().enableRowLevelSecurity();
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
