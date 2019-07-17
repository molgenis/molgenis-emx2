package org.molgenis.sql.psql.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.beans.QueryBean;
import org.molgenis.beans.RowBean;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.molgenis.Column.Type.INT;
import static org.molgenis.Column.Type.STRING;

public class TestQuery {

  public static Database db = null;
  public static Schema s = null;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    db = SqlTestHelper.getEmptyDatabase();

    // create a table to test with
    s = db.createSchema("TestQuery");
    String PERSON = "Person";
    Table person = s.createTable(PERSON);
    person.addColumn("First Name", STRING);
    person.addRef("Father", person).setNullable(true);
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
    Query q = s.query("Person");
    q.select("First Name")
        .select("Last Name")
        .expand("Father")
        .include("First Name")
        .include("Last Name");
    q.where("Last Name").eq("Duck").and("Father", "Last Name").eq("Duck");

    List<Row> rows = q.retrieve();
    for (Row r : rows) {
      System.out.println(r);
    }
  }

  @Test
  public void test2() throws MolgenisException {

    long startTime = System.currentTimeMillis();

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
    component.addMref("parts", part, "ComponentPart", "components");

    Row component1 = new RowBean().setString("name", "explorer").setMref("parts", part1, part2);
    Row component2 = new RowBean().setString("name", "navigator").setMref("parts", part2);
    component.insert(component1);
    component.insert(component2);

    String PRODUCT = "Product";
    Table product = s.createTable(PRODUCT);
    product.addColumn("name", STRING);
    product.addUnique("name");
    product.addMref("components", component, "ProductComponent", "products");

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

    startTime = System.currentTimeMillis();

    Query q = s.query("Product");
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

    System.out.println("Query took " + (endTime - startTime) + " milliseconds");

    // restart database and see if it is still there
    db = SqlTestHelper.reload();

    System.out.println(db.getSchema("TestQuery"));

    startTime = System.currentTimeMillis();

    Query q2 = s.query("Product");
    q2.select("name")
        .expand("components")
        .include("name")
        .expand("components", "parts")
        .include("name");
    // q.where("components", "parts", "weight").eq(50).and("name").eq("explorer", "navigator");

    List<Row> rows2 = q2.retrieve();
    assertEquals(rows2.size(), 3);
    for (Row r : rows2) {
      System.out.println(r);
    }

    System.out.println("Query took " + (endTime - startTime) + " milliseconds");

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
