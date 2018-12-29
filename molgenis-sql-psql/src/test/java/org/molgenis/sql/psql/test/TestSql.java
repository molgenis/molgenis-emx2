package org.molgenis.sql.psql.test;

import org.jooq.DSLContext;
import org.jooq.ForeignKey;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.sql.*;
import org.molgenis.sql.psql.PsqlDatabase;
import org.molgenis.sql.psql.PsqlRow;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.molgenis.sql.SqlType.*;
import static org.molgenis.sql.psql.PsqlDatabase.MOLGENISID;

public class TestSql {

  private static SqlDatabase db = null;

  @BeforeClass
  public static void setUp() {
    String userName = "molgenis";
    String password = "molgenis";
    String url = "jdbc:postgresql:molgenis";

    try {
      Connection conn = DriverManager.getConnection(url, userName, password);
      DSLContext context = DSL.using(conn, SQLDialect.POSTGRES_10);

      // delete all foreign key constaints
      for (Table t : context.meta().getTables()) {
        for (ForeignKey k : (List<ForeignKey>) t.getReferences()) {
          context.alterTable(t).dropConstraint(k.getName()).execute();
        }
      }
      // delete all tables
      for (Table t : context.meta().getTables()) {
        context.dropTable(t).execute();
      }
      db = new PsqlDatabase(context);
    }

    // For the sake of this test, let's keep exception handling simple
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testBatch() throws SqlDatabaseException, SqlQueryException {
    SqlTable t = db.createTable("TestBatch");
    t.addColumn("test", STRING);
    t.addColumn("testint", INT);

    List<SqlRow> rows = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      SqlRow r = new PsqlRow();
      r.setString("test", "test" + i);
      r.setInt("testint", i);
      rows.add(r);
    }
    t.insert(rows);
    for (SqlRow r : rows) {
      r.setString("test", r.getString("test") + "_updated)");
    }
    t.update(rows);

    System.out.println("and retrieving");
    for (SqlRow r : db.getQuery().from("TestBatch").retrieve()) {
      System.out.println(r);
    }
  }

  @Test
  public void testTypes() throws SqlDatabaseException, SqlQueryException {

    // generate TypeTest table, with columns for each type
    SqlTable t = db.createTable("TypeTest");
    for (SqlType type : SqlType.values()) {
      if (REF.equals(type)) {
        t.addColumn("Test_" + type.toString().toLowerCase() + "_nillable", t).setNullable(true);

      } else {
        t.addColumn("Test_" + type.toString().toLowerCase(), type);
        t.addColumn("Test_" + type.toString().toLowerCase() + "_nillable", type).setNullable(true);
      }
    }

    // retrieve this table from metadataa
    SqlTable t2 = db.getTable("TypeTest");
    System.out.println(t2);

    // check nullable ok
    SqlRow row = new PsqlRow();
    row.setUuid("Test_uuid", java.util.UUID.randomUUID());
    row.setString("Test_string", "test");
    row.setBool("Test_bool", true);
    row.setInt("Test_int", 1);
    row.setDecimal("Test_decimal", 1.1);
    row.setText("Test_text", "testtext");
    row.setDate("Test_date", LocalDate.of(2018, 12, 13));
    row.setDateTime("Test_datetime", OffsetDateTime.of(2018, 12, 13, 12, 40, 0, 0, ZoneOffset.UTC));
    t2.insert(row);

    // check not null expects exception
    row = new PsqlRow();
    row.setUuid("Test_uuid_nillable", java.util.UUID.randomUUID());
    row.setString("Test_string_nillable", "test");
    row.setBool("Test_bool_nillable", true);
    row.setInt("Test_int_nillable", 1);
    row.setDecimal("Test_decimal_nillable", 1.1);
    row.setText("Test_text_nillable", "testtext");
    row.setDate("Test_date_nillable", LocalDate.of(2018, 12, 13));
    row.setDateTime(
        "Test_datetime_nillable", OffsetDateTime.of(2018, 12, 13, 12, 40, 0, 0, ZoneOffset.UTC));
    try {
      t2.insert(row);
      fail(); // should not reach this one
    } catch (SqlDatabaseException e) {
      System.out.println("as expected, caught exceptoin: " + e.getMessage());
    }

    // check query and test getters
    List<SqlRow> result = db.getQuery().from("TypeTest").retrieve();
    for (SqlRow res : result) {
      System.out.println(res);
      res.setRowID(java.util.UUID.randomUUID());
      assert (res.getDate("Test_date") instanceof LocalDate);
      assert (res.getDateTime("Test_datetime") instanceof OffsetDateTime);
      assert (res.getString("Test_string") instanceof String);
      assert (res.getInt("Test_int") instanceof Integer);
      assert (res.getDecimal("Test_decimal") instanceof Double);
      assert (res.getText("Test_text") instanceof String);
      assert (res.getBool("Test_bool") instanceof Boolean);
      assert (res.getUuid("Test_uuid") instanceof java.util.UUID);

      t2.insert(res);
    }
  }

  @Test
  public void testCreate() throws SqlDatabaseException {

    // create a fromTable
    SqlTable t = db.createTable("Person");
    t.addColumn("First Name", STRING);
    t.addColumn("Father", t).setNullable(true);
    t.addColumn("Last Name", STRING);
    t.addUnique("First Name", "Last Name");

    System.out.println("Created fromTable: \n" + t.toString());

    // inspect a table (reloads metadata on every call, is that what we want?)
    SqlTable t2 = db.getTable("Person");

    List<SqlRow> rows = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      rows.add(new PsqlRow().setString("Last Name", "Duck" + i).setString("First Name", "Donald"));
    }
    t.insert(rows);
    t.delete(rows);

    System.out.println("Refreshed fromTable: \n" + t2.toString());

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
    // db.dropTable(t.getName());
  }

  @Test
  public void testQuery() throws SqlDatabaseException, SqlQueryException {

    SqlTable product = db.createTable("Product");
    product.addColumn("name", STRING);
    product.addUnique("name");

    SqlRow product1 = new PsqlRow().setString("name", "molgenis");
    product.insert(product1);

    SqlTable component = db.createTable("Component");
    component.addColumn("name", STRING);
    component.addUnique("name");

    SqlRow component1 = new PsqlRow().setString("name", "explorer");
    SqlRow component2 = new PsqlRow().setString("name", "navigator");

    component.insert(component1);
    component.insert(component2);

    SqlTable productComponent = db.createTable("ProductComponent");
    productComponent.addColumn("product", product);
    productComponent.addColumn("component", component);
    productComponent.addUnique("product", "component");

    SqlRow productComponent1 =
        new PsqlRow().setRef("component", component1).setRef("product", product1);
    SqlRow productComponent2 =
        new PsqlRow().setRef("component", component2).setRef("product", product1);
    productComponent.insert(productComponent1);
    productComponent.insert(productComponent2);

    SqlTable part = db.createTable("Part");
    part.addColumn("name", STRING);
    part.addColumn("weight", INT);
    part.addUnique("name");

    SqlRow part1 = new PsqlRow().setString("name", "forms").setInt("weight", 100);
    SqlRow part2 = new PsqlRow().setString("name", "login").setInt("weight", 50);
    part.insert(part1);
    part.insert(part2);

    SqlTable componentPart = db.createTable("ComponentPart");
    componentPart.addColumn("component", component);
    componentPart.addColumn("part", part);
    componentPart.addUnique("component", "part");

    SqlRow componentPart1 = new PsqlRow().setRef("component", component1).setRef("part", part1);
    SqlRow componentPart2 = new PsqlRow().setRef("component", component1).setRef("part", part2);
    componentPart.insert(componentPart1);
    componentPart.insert(componentPart2);

    // now getQuery to show product.name and parts.name linked by path Assembly.product,part

    // needed:
    // join+columns paths, potentially multiple paths. We only support outer join over relationships
    // if names are not unique, require explicit select naming
    // complex nested where clauses
    // sortby clauses
    // later: group by.

    //        PsqlQuery q1 = new PsqlQueryBack(db);
    // db        q1.select("Product").columns("name").as("productName");
    //        q1.mref("ProductComponent").columns("name").as("componentName");
    //        q1.mref("ComponentPart").columns("name").as("partName");
    //        //q1.where("productName").eq("molgenis");
    //
    //        System.out.println(q1);

    try {
      db.getQuery().from("pietje");
      fail("exception handling from(pietje) failed");
    } catch (Exception e) {
      // good stuff
    }

    try {
      db.getQuery().from("Product").from("Product");
      fail("shouldn't be able to say 'from' 2x");
    } catch (Exception e) {
      // good stuff
    }

    try {
      db.getQuery().from("Product").as("p").join("ProductComponent", "p2", "product");
      fail("should fail because faulty toTabel");
    } catch (Exception e) {
      // good stuff
    }

    try {
      db.getQuery().join("Component", "pc", "component").from("Product");
      fail("shouldn't be able to join before from");

    } catch (Exception e) {
      // good stuff
    }

    try {
      db.getQuery().from("Product").as("p").join("ProductComponent", "p", "component");
      fail("should fail because faulty 'on'");
    } catch (Exception e) {
      // good stuff
    }
    try {
      db.getQuery().from("Product").as("p").select("wrongname").as("productName");
      fail("should fail because faulty 'select'");
    } catch (Exception e) {
      // good stuff
    }

    SqlQuery q2 = db.getQuery();
    q2.from("Product").as("p").select("name").as("productName");
    q2.join("ProductComponent", "p", "product").as("pc");
    q2.join("Component", "pc", "component").as("c").select("name").as("componentName");
    q2.join("ComponentPart", "c", "component").as("cp");
    q2.join("Part", "cp", "part").as("p2").select("name").as("partName").select("weight");
    q2.eq("p2", "weight", 50).eq("c", "name", "explorer", "navigator");

    for (SqlRow row : q2.retrieve()) {
      System.out.println(row);
    }

    System.out.println(q2);
  }

  @AfterClass
  public static void close() {
    db.close();
  }
}
