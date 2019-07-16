package org.molgenis.sql.psql.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.Database;
import org.molgenis.MolgenisException;
import org.molgenis.Row;
import org.molgenis.Table;
import org.molgenis.beans.RowBean;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.molgenis.Column.Type.STRING;

public class TestTransaction {
  private static Database db;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    db = SqlTestHelper.getEmptyDatabase();
  }

  @Test
  public void testCommit() throws MolgenisException {

    db.transaction(
        db -> {
          Table t = db.getSchema().createTable("TxTestCommit");
          t.addColumn("ColA", STRING);
          t.addUnique("ColA");

          Row r = new RowBean().setString("ColA", "test");
          Row r2 = new RowBean().setString("ColA", "test2");

          db.insert("TxTestCommit", r);
          db.insert("TxTestCommit", r2);
          assertEquals(2, db.query("TxTestCommit").retrieve().size());
        });
  }

  @Test(expected = MolgenisException.class)
  public void testRollBack() throws MolgenisException {
    db.transaction(
        db -> {
          Table t = db.getSchema().createTable("TxTestRollback");
          t.addColumn("ColA", STRING);
          t.addUnique("ColA");

          Row r = new RowBean().setString("ColA", "test");
          db.insert("TxTest", r);
          db.insert("TxTest", r);
        });
  }
}
