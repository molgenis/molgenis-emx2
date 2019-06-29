package org.molgenis.sql.psql.test;

import org.jooq.Field;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.beans.RowBean;
import org.molgenis.sql.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    db = SqlTestHelper.getEmptyDatabase();
  }

  @Test
  public void testBatch() throws MolgenisException {
    String TEST_BATCH = "TestBatch";
    Table t = db.getSchema().createTable(TEST_BATCH);
    t.addColumn("test", STRING);
    t.addColumn("testint", INT);

    long startTime = System.currentTimeMillis();

    int size = 1000;

    List<Row> rows = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      Row r = new RowBean();
      r.setString("test", "test" + i);
      r.setInt("testint", i);
      rows.add(r);
    }
    long endTime = System.currentTimeMillis();
    System.out.println(
        "Generated " + size + " test record in " + (endTime - startTime) + " milliseconds");

    startTime = System.currentTimeMillis();
    db.insert(TEST_BATCH, rows.subList(0, 100));
    endTime = System.currentTimeMillis();
    System.out.println("First batch insert " + (endTime - startTime) + " milliseconds");

    startTime = System.currentTimeMillis();
    db.insert(TEST_BATCH, rows.subList(100, 200));
    endTime = System.currentTimeMillis();
    System.out.println("Second batch insert " + (endTime - startTime) + " milliseconds");

    startTime = System.currentTimeMillis();
    db.insert(TEST_BATCH, rows.subList(200, 300));
    endTime = System.currentTimeMillis();
    System.out.println("Third batch insert " + (endTime - startTime) + " milliseconds");

    startTime = System.currentTimeMillis();
    for (Row r : rows) {
      r.setString("test", r.getString("test") + "_updated");
    }
    db.update(TEST_BATCH, rows);
    endTime = System.currentTimeMillis();
    System.out.println("Batch update " + (endTime - startTime) + " milliseconds");

    startTime = System.currentTimeMillis();
    for (Row r : db.query("TestBatch").retrieve()) {
      System.out.println(r);
    }
    endTime = System.currentTimeMillis();
    System.out.println("Retrieve " + (endTime - startTime) + " milliseconds");
  }

  @Test
  public void testTypes() throws MolgenisException {

    // generate TypeTest table, with columns for each type
    Table t = db.getSchema().createTable("TypeTest");
    for (Column.Type type : Column.Type.values()) {
      if (REF.equals(type)) {
        Column c =
            t.addRef("Test_" + type.toString().toLowerCase() + "_nillable", t).setNullable(true);
        checkColumnExists(c);
      } else if (MREF.equals(type)) {
        // cannot set nullable
      } else {
        Column c = t.addColumn("Test_" + type.toString().toLowerCase(), type);
        Column c2 =
            t.addColumn("Test_" + type.toString().toLowerCase() + "_nillable", type)
                .setNullable(true);
        checkColumnExists(c);
        checkColumnExists(c2);
      }
    }

    // retrieve this table from metadataa
    String TYPE_TEST = "TypeTest";
    Table t2 = db.getSchema().getTable("TypeTest");
    System.out.println(t2);

    // check nullable ok
    Row row = new RowBean();
    row.setUuid("Test_uuid", java.util.UUID.randomUUID());
    row.setString("Test_string", "test");
    row.setEnum("Test_enum", "test");
    row.setBool("Test_bool", true);
    row.setInt("Test_int", 1);
    row.setDecimal("Test_decimal", 1.1);
    row.setText("Test_text", "testtext");
    row.setDate("Test_date", LocalDate.of(2018, 12, 13));
    row.setDateTime("Test_datetime", OffsetDateTime.of(2018, 12, 13, 12, 40, 0, 0, ZoneOffset.UTC));
    db.insert(TYPE_TEST, row);

    // check not null expects exception
    row = new RowBean();
    row.setUuid("Test_uuid_nillable", java.util.UUID.randomUUID());
    row.setString("Test_string_nillable", "test");
    row.setEnum("Test_enum_nillable", "test");

    row.setBool("Test_bool_nillable", true);
    row.setInt("Test_int_nillable", 1);
    row.setDecimal("Test_decimal_nillable", 1.1);
    row.setText("Test_text_nillable", "testtext");
    row.setDate("Test_date_nillable", LocalDate.of(2018, 12, 13));
    row.setDateTime(
        "Test_datetime_nillable", OffsetDateTime.of(2018, 12, 13, 12, 40, 0, 0, ZoneOffset.UTC));
    try {
      db.insert(TYPE_TEST, row);
      fail(); // should not reach this one
    } catch (MolgenisException e) {
      System.out.println("as expected, caught exceptoin: " + e.getMessage());
    }

    // check queryOld and test getters
    List<Row> result = db.query("TypeTest").retrieve();
    for (Row res : result) {
      System.out.println(res);
      res.setMolgenisid(java.util.UUID.randomUUID());
      assert (res.getDate("Test_date") instanceof LocalDate);
      assert (res.getDateTime("Test_datetime") instanceof OffsetDateTime);
      assert (res.getString("Test_string") instanceof String);
      assert (res.getInt("Test_int") instanceof Integer);
      assert (res.getDecimal("Test_decimal") instanceof Double);
      assert (res.getText("Test_text") instanceof String);
      assert (res.getBool("Test_bool") instanceof Boolean);
      assert (res.getUuid("Test_uuid") instanceof java.util.UUID);

      db.insert(TYPE_TEST, res);
    }

    System.out.println("testing TypeTest queryOld");
    for (Row r : db.query("TypeTest").retrieve()) {
      System.out.println(r);
    }
  }

  @Test
  public void testCreate() throws MolgenisException {

    long startTime = System.currentTimeMillis();

    SqlTestHelper.emptyDatabase();

    // create a fromTable
    String PERSON = "Person";
    Table t = db.getSchema().createTable(PERSON);
    t.addColumn("First Name", STRING);
    t.addRef("Father", t).setNullable(true);
    t.addColumn("Last Name", STRING);
    t.addUnique("First Name", "Last Name");
    long endTime = System.currentTimeMillis();

    System.out.println(
        "Created fromTable: \n" + t.toString() + " in " + (endTime - startTime) + " milliseconds");

    // reinitialise database to see if it can recreate from background
    db = new SqlDatabase(SqlTestHelper.getDataSource());
    assertEquals(1, db.getSchema().getTables().size());

    // insert
    startTime = System.currentTimeMillis();
    Table t2 = db.getSchema().getTable(PERSON);
    List<Row> rows = new ArrayList<>();
    int count = 1000;
    for (int i = 0; i < count; i++) {
      rows.add(new RowBean().setString("Last Name", "Duck" + i).setString("First Name", "Donald"));
    }
    db.insert(PERSON, rows);
    endTime = System.currentTimeMillis();
    long total = (endTime - startTime);
    System.out.println(
        "Insert of "
            + count
            + " records took "
            + total
            + " milliseconds (that is "
            + (1000 * count / total)
            + " rows/sec)");

    // queryOld
    startTime = System.currentTimeMillis();
    Query q = db.query(PERSON);
    for (Row row : q.retrieve()) {
      System.out.println("QueryOld result: " + row);
    }
    endTime = System.currentTimeMillis();
    System.out.println("QueryOld took " + (endTime - startTime) + " milliseconds");
    System.out.println("QueryOld contents " + q);

    // delete
    startTime = System.currentTimeMillis();
    db.delete(PERSON, rows);
    endTime = System.currentTimeMillis();
    total = (endTime - startTime);
    System.out.println(
        "Delete took " + total + " milliseconds (that is " + (1000 * count / total) + " rows/sec)");

    assertEquals(0, db.query("Person").retrieve().size());

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
    db.getSchema().dropTable(t.getName());
    assertEquals(null, db.getSchema().getTable("Person"));
    // make sure nothing was left behind in backend
    db = new SqlDatabase(SqlTestHelper.getDataSource());
    assertEquals(null, db.getSchema().getTable("Person"));
  }

  private void checkColumnExists(Column c) throws MolgenisException {
    List<org.jooq.Table<?>> tables =
        SqlTestHelper.getJooq().meta().getTables(c.getTable().getName());
    if (tables.size() == 0)
      throw new MolgenisException("Table '" + c.getTable().getName() + "' does not exist");
    org.jooq.Table<?> table = tables.get(0);
    Field f = table.field(c.getName());
    if (f == null)
      throw new MolgenisException("Field '" + c.getName() + "." + c.getName() + "' does not exist");
  }
}
