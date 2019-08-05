package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.Row;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.molgenis.Type.STRING;

public class TestTransaction {
  private static Database db;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    db = DatabaseFactory.getDatabase("molgenis", "molgenis");
  }

  @Test
  public void testCommit() throws MolgenisException {

    db.transaction(
        db -> {
          Schema schema = db.createSchema("testCommit");
          Table testTable = schema.createTableIfNotExists("testCommit");
          testTable.addColumn("ColA", STRING);
          testTable.addUnique("ColA");
          testTable.insert(new Row().setString("ColA", "test"));
          testTable.insert(new Row().setString("ColA", "DependencyOrderOutsideTransactionFails"));
        });
    db.clearCache();
    assertEquals(2, db.getSchema("testCommit").getTable("testCommit").retrieve().size());
  }

  @Test(expected = MolgenisException.class)
  public void testRollBack() throws MolgenisException {
    db.transaction(
        db -> {
          Schema schema = db.createSchema("testRollBack");
          Table testTable = schema.createTableIfNotExists("testRollBack");
          testTable.addColumn("ColA", STRING);
          testTable.addUnique("ColA");

          org.molgenis.Row r = new Row().setString("ColA", "test");
          testTable.insert(r);
          testTable.insert(r);
        });
  }
}
