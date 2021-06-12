package org.molgenis.emx2.sql;

import static org.junit.Assert.assertNull;

import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;

public class TestTransaction {
  private static Database db;

  @BeforeClass
  public static void setUp() throws SQLException {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testTransaction() {
    Schema s = db.dropCreateSchema("testTransaction");

    // as long as from same db instance you can use resources in multiple Tx
    try {
      db.tx(
          db -> {
            s.create(TableMetadata.table("a"));
            s.create(TableMetadata.table("b"));
            throw new RuntimeException("transaction stopped to check if it rolled back");
          });
    } catch (Exception e) {
      System.out.println(e);
      assertNull(s.getTable("a"));
    }
  }
}
