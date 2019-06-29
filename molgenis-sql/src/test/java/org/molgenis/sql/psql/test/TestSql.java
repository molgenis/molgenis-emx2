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

  @Test
  public void testQuery() throws MolgenisException {

    long startTime = System.currentTimeMillis();

    String PART = "Part";
    Table part = db.getSchema().createTable(PART);
    part.addColumn("name", STRING);
    part.addColumn("weight", INT);
    part.addUnique("name");

    Row part1 = new RowBean().setString("name", "forms").setInt("weight", 100);
    Row part2 = new RowBean().setString("name", "login").setInt("weight", 50);
    db.insert(PART, part1);
    db.insert(PART, part2);

    /*
    Table component = db.createTable("Component");
    component.addColumn("name", STRING);
    component.addUnique("name");
    component.addMref("parts", part, "partOfComponent");

    Row component1 = new RowBean().setString("name", "explorer").setMref("parts", part1, part2);
    Row component2 = new RowBean().setString("name", "navigator").setMref("parts", part2);
    component.insert(component1);
    component.insert(component2);

    Table product = db.createTable("Product");
    product.addColumn("name", STRING);
    product.addUnique("name");
    product.addMref("components", component, "partOfProduct");

    Row product1 =
        new RowBean().setString("name", "molgenis").setMref("components", component1, component2);
    product.insert(product1);


    long endTime = System.currentTimeMillis();
    System.out.println("Creation took " + (endTime - startTime) + " milliseconds");

    // now getQuery to show product.name and parts.name linked by path Assembly.product,part

    // needed:
    // join+columns paths, potentially multiple paths. We only support outer join over relationships
    // if names are not unique, require explicit select naming
    // complex nested where clauses
    // sortby clauses
    // later: group by.

    //        QueryOldImpl q1 = new PsqlQueryBack(db);
    // db        q1.select("Product").columns("name").as("productName");
    //        q1.mref("ProductComponent").columns("name").as("componentName");
    //        q1.mref("ComponentPart").columns("name").as("partName");
    //        //q1.where("productName").eq("molgenis");
    //
    //        System.out.println(q1);

    try {
      db.queryOld("pietje");
      fail("exception handling from(pietje) failed");
    } catch (Exception e) {
      System.out.println("Succesfully caught exception: " + e);
    }

    try {
      db.queryOld("Product").as("p").join("Comp", "p", "components");
      fail("should fail because faulty table");
    } catch (Exception e) {
      System.out.println("Succesfully caught exception: " + e);
    }

    try {
      db.queryOld("Product").as("p").join("Component", "p2", "components");
      fail("should fail because faulty toTabel");
    } catch (Exception e) {
      System.out.println("Succesfully caught exception: " + e);
    }

    try {
      db.queryOld("Product").as("p").join("Component", "p2", "components");
      fail("should fail because faulty on although it is an mref");
    } catch (Exception e) {
      System.out.println("Succesfully caught exception: " + e);
    }

    try {
      db.queryOld("Product").as("p").join("Component", "p", "comps");
      fail("should fail because faulty on");
    } catch (Exception e) {
      System.out.println("Succesfully caught exception: " + e);
    }

    try {
      db.queryOld("Product").as("p").select("wrongname").as("productName");
      fail("should fail because faulty 'select'");
    } catch (Exception e) {
      System.out.println("Succesfully caught exception: " + e);
    }

    startTime = System.currentTimeMillis();
    QueryOldImpl q2 = db.queryOld("Product").as("p").select("name").as("productName");
    q2.join("Component", "p", "components").as("c").select("name").as("componentName");
    q2.join("Part", "c", "parts").as("pt").select("name").as("partName").select("weight");
    q2.eq("pt", "weight", 50).eq("c", "name", "explorer", "navigator");
    for (Row row : q2.retrieve()) {
      System.out.println(row);
    }
    endTime = System.currentTimeMillis();
    System.out.println("QueryOldImpl took " + (endTime - startTime) + " milliseconds");
    System.out.println("QueryOldImpl contents: " + q2);

    // again reversing the 'on' columns
    QueryOldImpl q3 = db.queryOld("Product").as("p").select("name").as("productName");
    q3.join("Component", "p", "partOfProduct").as("c").select("name").as("componentName");
    q3.join("Part", "c", "partOfComponent").as("pt").select("name").as("partName").select("weight");
    q3.eq("pt", "weight", 50).eq("c", "name", "explorer", "navigator");
    for (Row row : q3.retrieve()) {
      System.out.println(row);
    }

    // test delete
    product.delete(product1);
    assertEquals(0, db.queryOld("Product").retrieve().size());
    */
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
