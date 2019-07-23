package org.molgenis.sql.psql.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.beans.RowBean;
import org.molgenis.utils.StopWatch;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.molgenis.sql.SqlRow.MOLGENISID;
import static org.molgenis.Column.Type.*;

public class TestSql {
  private static Database db;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    db = DatabaseFactory.getDatabase();
  }

  @Test
  public void testBatch() throws MolgenisException {

    StopWatch.start("testBatch started");

    Schema s = db.createSchema("testBatch");
    Table test_batch = s.createTable("TestBatch");
    test_batch.addColumn("test", STRING);
    test_batch.addColumn("testint", INT);

    int size = 1000;
    StopWatch.print("Schema created");

    List<Row> rows = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      Row r = new RowBean();
      r.setString("test", "test" + i);
      r.setInt("testint", i);
      rows.add(r);
    }
    StopWatch.print("Generated " + size + " test records", size);

    test_batch.insert(rows.subList(0, 100));

    StopWatch.print("Inserted first batch", 100);

    test_batch.insert(rows.subList(100, 200));

    StopWatch.print("Inserted second batch", 100);

    test_batch.insert(rows.subList(200, 300));

    StopWatch.print("Inserted third batch", 100);

    for (Row r : rows) {
      r.setString("test", r.getString("test") + "_updated");
    }
    test_batch.update(rows);
    StopWatch.print("Batch update ", rows.size());

    StopWatch.print("Retrieved", s.getTable("TestBatch").retrieve().size());
  }

  @Test
  public void testCreate() throws MolgenisException {

    StopWatch.start("");

    Schema s = db.createSchema("testCreate");

    String PERSON = "Person";
    Table t = s.createTable(PERSON);
    t.addColumn("First Name", STRING).setNullable(false); // default nullable=false but for testing
    t.addColumn("Last Name", STRING);
    t.addRef("Father", t.getName()).setNullable(true);
    t.addUnique("First Name", "Last Name");

    // create a fromTable
    // TODO need to optimize the reloading to be more lazy
    for (int i = 0; i < 10; i++) {
      Table t2 = s.createTable(PERSON + i);
      t2.addColumn("First Name", STRING)
          .setNullable(false); // default nullable=false but for testing
      t2.addColumn("Last Name", STRING);
      t2.addRef("Father", t2.getName()).setNullable(true);
      t2.addUnique("First Name", "Last Name");
    }
    StopWatch.print("Created tables");

    // reinitialise database to see if it can recreate from background
    StopWatch.print("reloading database from disk");

    db.clearCache();
    s = db.getSchema("testCreate");
    assertEquals(11, s.getTableNames().size());
    StopWatch.print("reloading complete");

    // insert
    Table t2 = s.getTable(PERSON);
    List<Row> rows = new ArrayList<>();
    int count = 1000;
    for (int i = 0; i < count; i++) {
      rows.add(new RowBean().setString("Last Name", "Duck" + i).setString("First Name", "Donald"));
    }
    System.out.println("Metadata" + t2);
    t2.insert(rows);

    StopWatch.print("insert", count);

    // queryOld
    Query q = s.getTable(PERSON).query();
    StopWatch.print("QueryOld ", q.retrieve().size());

    // delete
    t2.delete(rows);
    StopWatch.print("Delete", count);

    assertEquals(0, s.getTable("Person").retrieve().size());
    assertEquals(2, t.getUniques().size());
    try {
      t.removeUnique(MOLGENISID);
      fail("you shouldn't be allowed to remove primary key unique constraint");
    } catch (Exception e) {
      // good stuff
    }
    t.removeUnique("Last Name", "First Name");
    assertEquals(1, t.getUniques().size());

    assertEquals(4, t.getColumns().size());
    try {
      t.removeColumn(MOLGENISID);
      fail("you shouldn't be allowed to remove primary key column");
    } catch (Exception e) {
      // good stuff
    }
    t.removeColumn("Father");
    assertEquals(3, t.getColumns().size());

    // drop a fromTable
    db.getSchema("testCreate").dropTable(t.getName());
    try {
      db.getSchema("testCreate").getTable(t.getName());
      fail("should have been dropped");
    } catch (Exception e) { // expected
    }

    // make sure nothing was left behind in backend
    db.clearCache();
    try {
      assertEquals(null, db.getSchema("testCreate").getTable(t.getName()));
      fail("should have been dropped");
    } catch (Exception e) { // expected
    }
  }
}
