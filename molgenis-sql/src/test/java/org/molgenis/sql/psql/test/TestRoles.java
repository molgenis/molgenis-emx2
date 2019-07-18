package org.molgenis.sql.psql.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.Database;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;

import java.sql.SQLException;

import static junit.framework.TestCase.fail;
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
          db -> {
            db.getSchema("testRole").createTable("Test");
          },
          "TESTROLE_VIEW");
      // should throw exception, otherwise fail
      fail();
    } catch (Exception e) {
      // this is expected
    }

    try {
      db.transaction(
          db -> {
            db.getSchema("testRole").createTable("Test");
            // this is soo cooool
            db.getSchema("testRole").grantView("testuser");
          },
          "testadmin");

    } catch (Exception e) {
      // this is NOT expected
      e.printStackTrace();
      fail();
    }
  }
}
