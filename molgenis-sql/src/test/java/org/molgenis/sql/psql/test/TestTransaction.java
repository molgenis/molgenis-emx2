package org.molgenis.sql.psql.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.beans.RowBean;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.molgenis.Column.Type.STRING;

public class TestTransaction {
  private static Database db;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    db = DatabaseFactory.getDatabase();
  }

  @Test
  public void testCommit() throws MolgenisException {

    Schema schema = db.createSchema("testCommit"); // not transactional in jooq :-(

    db.transaction(
        db -> {
          Schema s = db.getSchema("testCommit");
          Table t = s.createTable("testCommit");
          t.addColumn("ColA", STRING);
          t.addUnique("ColA");
          t.insert(new RowBean().setString("ColA", "test"));
          t.insert(new RowBean().setString("ColA", "test2"));
        });
    assertEquals(2, schema.getTable("testCommit").retrieve().size());
  }

  @Test(expected = MolgenisException.class)
  public void testRollBack() throws MolgenisException {
    db.transaction(
        db -> {
          Schema s = db.createSchema("testRollBack");
          Table t = s.createTable("testRollBack");
          t.addColumn("ColA", STRING);
          t.addUnique("ColA");

          Row r = new RowBean().setString("ColA", "test");
          t.insert(r);
          t.insert(r);
        });
  }
}
