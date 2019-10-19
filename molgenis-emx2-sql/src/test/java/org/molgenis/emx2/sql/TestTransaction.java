package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.utils.MolgenisException;

import java.sql.SQLException;

import static org.junit.Assert.assertNull;

public class TestTransaction {
  private static Database db;

  @BeforeClass
  public static void setUp() throws SQLException {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testTransaction() {
    Schema s = db.createSchema("testTransaction");

    // as long as from same db instance you can use resources in multiple Tx
    try {
      db.transaction(
          db -> {
            s.createTableIfNotExists("a");
            s.createTableIfNotExists("b");
            throw new RuntimeException("transaction stopped to check if it rolled back");
          });
    } catch (Exception e) {
      System.out.println(e);
      assertNull(s.getTable("a"));
    }
  }
}
