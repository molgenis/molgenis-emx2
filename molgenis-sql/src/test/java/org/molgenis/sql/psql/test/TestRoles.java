package org.molgenis.sql.psql.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.Database;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.Table;
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
    // grant both admin on TestRLS
    s.grantAdmin("testrls1");
    s.grantAdmin("testrls2");

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
              .insert(new RowBean().setString("col1", "Hello World"));
        });

    // let the second see it
    db.transaction(
        "testrls2",
        db -> {
          assertEquals(1, db.getSchema("TestRLS").getTable("TestRLS").retrieve().size());
        });
  }
}
