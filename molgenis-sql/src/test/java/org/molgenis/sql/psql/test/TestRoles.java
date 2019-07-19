package org.molgenis.sql.psql.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.Database;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.beans.RowBean;

import java.sql.SQLException;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.molgenis.Column.Type.STRING;

public class TestRoles {
  private static Database db;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    db = SqlTestHelper.getEmptyDatabase();
  }

  @Test
  public void testRolePermissions() throws MolgenisException {
    Schema s = db.createSchema("TestRolePermissions");

    // create test users
    db.createUser("testRolePermissions_viewer");
    db.createUser("testRolePermissions_editor");
    db.createUser("testRolePermissions_manager");

    // grant proper roles
    s.grantView("testRolePermissions_viewer");
    s.grantEdit("testRolePermissions_editor");
    s.grantManage("testRolePermissions_manager");

    // test that viewer and editor cannot create, and manager can
    try {
      db.transaction(
          "testRolePermissions_viewer",
          db -> {
            db.getSchema("TestRolePermissions").createTable("Test");
            fail("role(viewers) should not be able to create tables"); // should not happen
          });
    } catch (Exception e) {
    }
    try {
      db.transaction(
          "testRolePermissions_editor",
          db -> {
            db.getSchema("TestRolePermissions").createTable("Test");
            fail("role(editors) should not be able to create tables"); // should not happen
          });
    } catch (Exception e) {
    }
    try {
      db.transaction(
          "testRolePermissions_manager",
          db -> {
            try {
              db.getSchema("TestRolePermissions").createTable("Test");
            } catch (Exception e) {
              e.printStackTrace();
              throw e;
            }
          });
    } catch (Exception e) {
      fail("role(manager) should be able to create tables"); // should not happen
      throw e;
    }
    // test that all can query
    try {
      db.transaction(
          "testRolePermissions_viewer",
          db -> {
            db.getSchema("TestRolePermissions").getTable("Test").retrieve();
          });
    } catch (Exception e) {
      e.printStackTrace();
      fail("role(viewers) should  be able to query "); // should not happen
    }
  }

  @Test
  public void testRole() throws MolgenisException {
    db.createUser("testadmin");
    db.createUser("testuser");
    Schema s = db.createSchema("testRole");
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
    // create schema
    Schema s = db.createSchema("TestRLS");
    // create two users
    db.createUser("testrls1");
    db.createUser("testrls2");
    db.createUser("testrlsnopermission");
    db.createUser("testrls_has_rls_view");
    // grant both admin on TestRLS schema so can add row level security
    s.grantAdmin("testrls1");
    s.grantAdmin("testrls2");
    s.grantView("testrls_has_rls_view"); // can view table but only rows with right RLS

    // let one user create the table
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
                  new RowBean()
                      .setString("col1", "Hello World")
                      .setRowEditRole("testrls_has_rls_view"),
                  new RowBean()
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
