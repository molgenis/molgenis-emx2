package org.molgenis.sql.psql.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.beans.RowBean;
import org.molgenis.sql.StopWatch;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.molgenis.Column.Type.INT;
import static org.molgenis.Column.Type.STRING;

public class TestQuery {
  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    Database db = DatabaseFactory.getDatabase();

    // create a schema to test with
    Schema s = db.createSchema("TestQuery");

    // create some tables with contents
    String PERSON = "Person";
    Table person = s.createTable(PERSON);
    person.addColumn("First Name", STRING);
    person.addRef("Father", PERSON).setNullable(true);
    person.addColumn("Last Name", STRING);
    person.addUnique("First Name", "Last Name");

    Row father = new RowBean().setString("First Name", "Donald").setString("Last Name", "Duck");
    Row child =
        new RowBean()
            .setString("First Name", "Kwik")
            .setString("Last Name", "Duck")
            .setRef("Father", father);

    person.insert(father);
    person.insert(child);
  }

  @Test
  public void test1() throws MolgenisException {

    StopWatch.start("test1");

    Database db = DatabaseFactory.getDatabase();
    Schema s = db.getSchema("TestQuery");

    StopWatch.print("got schema");

    Query q = s.getTable("Person").query();
    q.select("First Name")
        .select("Last Name")
        .expand("Father")
        .include("First Name")
        .include("Last Name");
    q.where("Last Name").eq("Duck").and("Father", "Last Name").eq("Duck");

    StopWatch.print("created query");

    List<Row> rows = q.retrieve();
    for (Row r : rows) {
      System.out.println(r);
    }

    StopWatch.print("query complete");

    q = s.getTable("Person").query();
    q.select("First Name")
        .select("Last Name")
        .expand("Father")
        .include("Last Name")
        .include("First Name");
    q.where("Last Name").eq("Duck").and("Father", "Last Name").eq("Duck");

    rows = q.retrieve();

    StopWatch.print("second time");
  }

  @Test
  public void test2() throws MolgenisException {
    Database db = DatabaseFactory.getDatabase();
    Schema s = db.getSchema("TestQuery");

    StopWatch.start("test2");

    String PART = "Part";
    Table part = s.createTable(PART);
    part.addColumn("name", STRING);
    part.addColumn("weight", INT);
    part.addUnique("name");

    Row part1 = new RowBean().setString("name", "forms").setInt("weight", 100);
    Row part2 = new RowBean().setString("name", "login").setInt("weight", 50);
    part.insert(part1);
    part.insert(part2);

    String COMPONENT = "Component";
    Table component = s.createTable(COMPONENT);
    component.addColumn("name", STRING);
    component.addUnique("name");
    component.addMref("parts", PART, "ComponentPart", "components");

    Row component1 = new RowBean().setString("name", "explorer").setMref("parts", part1, part2);
    Row component2 = new RowBean().setString("name", "navigator").setMref("parts", part2);
    component.insert(component1);
    component.insert(component2);

    String PRODUCT = "Product";
    Table product = s.createTable(PRODUCT);
    product.addColumn("name", STRING);
    product.addUnique("name");
    product.addMref("components", COMPONENT, "ProductComponent", "products");

    Row product1 =
        new RowBean().setString("name", "molgenis").setMref("components", component1, component2);

    product.insert(product1);

    StopWatch.print("tables created");

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

    Query q = s.getTable("Product").query();
    q.select("name")
        .expand("components")
        .include("name")
        .expand("components", "parts")
        .include("name");
    // q.where("components", "parts", "weight").eq(50).and("name").eq("explorer", "navigator");

    List<Row> rows = q.retrieve();
    assertEquals(rows.size(), 3);
    for (Row r : rows) {
      System.out.println(r);
    }

    StopWatch.print("query completed");

    // restart database and see if it is still there
    db.clearCache();
    s = db.getSchema("TestQuery");

    StopWatch.print("cleared cache");

    Query q2 = s.getTable("Product").query();
    q2.select("name")
        .expand("components")
        .include("name")
        .expand("components", "parts")
        .include("name");
    // q.where("components", "parts", "weight").eq(50).and("name").eq("explorer", "navigator");

    StopWatch.print("created query (needed to get metadata from disk)");

    List<Row> rows2 = q2.retrieve();
    assertEquals(rows2.size(), 3);
    for (Row r : rows2) {
      System.out.println(r);
    }

    StopWatch.print("queried again, cached so for free");

    //      try {
    //          db.query("pietje");
    //          fail("exception handling from(pietje) failed");
    //      } catch (Exception e) {
    //          System.out.println("Succesfully caught exception: " + e);
    //      }

    //      try {
    //          db.query("Product").as("p").join("Comp", "p", "components");
    //          fail("should fail because faulty table");
    //      } catch (Exception e) {
    //          System.out.println("Succesfully caught exception: " + e);
    //      }
    //
    //      try {
    //          db.query("Product").as("p").join("Component", "p2", "components");
    //          fail("should fail because faulty toTabel");
    //      } catch (Exception e) {
    //          System.out.println("Succesfully caught exception: " + e);
    //      }
    //
    //      try {
    //          db.queryOld("Product").as("p").join("Component", "p2", "components");
    //          fail("should fail because faulty on although it is an mref");
    //      } catch (Exception e) {
    //          System.out.println("Succesfully caught exception: " + e);
    //      }
    //
    //      try {
    //          db.queryOld("Product").as("p").join("Component", "p", "comps");
    //          fail("should fail because faulty on");
    //      } catch (Exception e) {
    //          System.out.println("Succesfully caught exception: " + e);
    //      }
    //
    //      try {
    //          db.queryOld("Product").as("p").select("wrongname").as("productName");
    //          fail("should fail because faulty 'select'");
    //      } catch (Exception e) {
    //          System.out.println("Succesfully caught exception: " + e);
    //      }
  }
}
