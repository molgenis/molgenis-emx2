package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.molgenis.emx2.TableMetadata.table;

import java.sql.SQLException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

public class TestTransaction {
  private static Database db;

  @BeforeAll
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
            Schema s2 = db.getSchema("testTransaction");
            s2.create(table("a"));
            s2.create(table("b"));
            throw new RuntimeException("transaction stopped to check if it rolled back");
          });
    } catch (Exception e) {
      System.out.println(e);
      assertNull(s.getTable("a"));
    }
  }
}
