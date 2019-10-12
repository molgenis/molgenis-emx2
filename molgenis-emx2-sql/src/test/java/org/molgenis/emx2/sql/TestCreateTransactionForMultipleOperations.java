package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.utils.MolgenisException;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.ColumnType.STRING;

public class TestCreateTransactionForMultipleOperations {
  private static Database db;

  @BeforeClass
  public static void setUp() , SQLException {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testCommit()  {

    db.transaction(
        db -> {
          Schema schema = db.createSchema("testCommit");
          Table testTable = schema.createTableIfNotExists("testCommit");
          testTable.getMetadata().addColumn("ColA", STRING).addUnique("ColA");
          testTable.insert(new Row().setString("ColA", "test"));
          testTable.insert(new Row().setString("ColA", "DependencyOrderOutsideTransactionFails"));
        });
    db.clearCache();
    assertEquals(2, db.getSchema("testCommit").getTable("testCommit").retrieve().size());
  }

  @Test(expected = MolgenisException.class)
  public void testRollBack()  {
    db.transaction(
        db -> {
          Schema schema = db.createSchema("testRollBack");
          Table testTable = schema.createTableIfNotExists("testRollBack");
          testTable.getMetadata().addColumn("ColA", STRING).addUnique("ColA");

          Row r = new Row().setString("ColA", "test");
          testTable.insert(r);
          testTable.insert(r);
        });
  }
}
