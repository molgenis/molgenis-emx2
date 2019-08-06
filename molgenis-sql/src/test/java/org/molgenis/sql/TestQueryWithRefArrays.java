package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.Row;
import org.molgenis.utils.StopWatch;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.molgenis.Type.INT;
import static org.molgenis.Type.STRING;

public class TestQueryWithRefArrays {
  static Database db;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");

    // createColumn a schema to test with
    Schema schema = db.createSchema("TestQueryWithRefArray");

    // createColumn some tables with contents
    String PERSON = "Person";
    Table personTable = schema.createTableIfNotExists(PERSON);
    personTable.addColumn("First Name", STRING);
    personTable.addRef("Father", PERSON).nullable(true);
    personTable.addColumn("Last Name", STRING);
    personTable.addUnique("First Name", "Last Name");

    org.molgenis.Row father =
        new Row().setString("First Name", "Donald").setString("Last Name", "Duck");
    org.molgenis.Row child =
        new Row()
            .setString("First Name", "Kwik")
            .setString("Last Name", "Duck")
            .setRef("Father", father);

    personTable.insert(father);
    personTable.insert(child);
  }

  @Test
  public void test1() throws MolgenisException {

    StopWatch.start("test1");

    Schema schema = db.getSchema("TestQueryWithRefArray");

    StopWatch.print("got schema");

    Query query1 =
        schema
            .query("Person")
            .select("First Name")
            .select("Last Name")
            .expand("Father")
            .include("First Name")
            .include("Last Name")
            .where("Last Name")
            .eq("Duck")
            .and("Father", "Last Name")
            .eq("Duck");

    StopWatch.print("created query");

    List<org.molgenis.Row> rows = query1.retrieve();
    for (org.molgenis.Row r : rows) {
      System.out.println(r);
    }

    StopWatch.print("query complete");

    query1 =
        schema
            .query("Person")
            .select("First Name")
            .select("Last Name")
            .expand("Father")
            .include("Last Name")
            .include("First Name")
            .where("Last Name")
            .eq("Duck")
            .and("Father", "Last Name")
            .eq("Duck");

    rows = query1.retrieve();
    assertEquals(1, rows.size());

    StopWatch.print("second time");
  }

  @Test
  public void test2() throws MolgenisException {
    Schema schema = db.getSchema("TestQueryWithRefArray");

    StopWatch.start("DependencyOrderOutsideTransactionFails");

    String PART = "Part";
    Table partTable = schema.createTableIfNotExists(PART);
    partTable.addColumn("name", STRING);
    partTable.addColumn("weight", INT);
    partTable.addUnique("name");

    org.molgenis.Row part1 = new Row().setString("name", "forms").setInt("weight", 100);
    org.molgenis.Row part2 = new Row().setString("name", "login").setInt("weight", 50);
    partTable.insert(part1);
    partTable.insert(part2);

    String COMPONENT = "Component";
    Table componentTable = schema.createTableIfNotExists(COMPONENT);
    componentTable.addColumn("name", STRING);
    componentTable.addUnique("name");
    componentTable.addRefArray("parts", "Part", "name");

    org.molgenis.Row component1 =
        new Row().setString("name", "explorer").setRefArray("parts", "forms", "login");
    org.molgenis.Row component2 =
        new Row().setString("name", "navigator").setRefArray("parts", "login");
    componentTable.insert(component1);
    componentTable.insert(component2);

    String PRODUCT = "Product";
    Table productTable = schema.createTableIfNotExists(PRODUCT);
    productTable.addColumn("name", STRING);
    productTable.addUnique("name");
    productTable.addRefArray("components", "Component", "name");

    org.molgenis.Row product1 =
        new Row().setString("name", "molgenis").setRefArray("components", "explorer", "navigator");

    productTable.insert(product1);

    StopWatch.print("tables created");

    // now getQuery to show product.name and parts.name linked by path Assembly.product,part

    // needed:
    // join+columns paths, potentially multiple paths. We only support outer join over relationships
    // if names are not unique, require explicit select naming
    // complex nested where clauses
    // sortby clauses
    // later: group by.

    //        QueryOldImpl q1 = new PsqlQueryBack(database);
    // database        q1.select("Product").columns("name").as("productName");
    //        q1.mref("ProductComponent").columns("name").as("componentName");
    //        q1.mref("ComponentPart").columns("name").as("partName");
    //        //q1.where("productName").eq("molgenis");
    //
    //        System.out.println(q1);

    Query q = schema.query("Product");
    q.select("name")
        .expand("components")
        .include("name")
        .expand("components", "parts")
        .include("name");
    //            .where("components", "parts", "weight")
    //            .eq(50)
    //            .and("name")
    //            .eq("explorer", "navigator");

    List<org.molgenis.Row> rows = q.retrieve();
    assertEquals(3, rows.size());
    for (org.molgenis.Row r : rows) {
      System.out.println(r);
    }

    StopWatch.print("query completed");

    // restart database and see if it is still there

    db.clearCache();
    schema = db.getSchema("TestQueryWithRefArray");

    StopWatch.print("cleared cache");

    Query q2 = schema.query("Product");
    q2.select("name")
        .expand("components")
        .include("name")
        .expand("components", "parts")
        .include("name");
    // q.where("components", "parts", "weight").eq(50).and("name").eq("explorer", "navigator");

    StopWatch.print("created query (needed to get metadata from disk)");

    List<org.molgenis.Row> rows2 = q2.retrieve();
    assertEquals(3, rows2.size());
    for (org.molgenis.Row r : rows2) {
      System.out.println(r);
    }

    StopWatch.print("queried again, cached so for free");

    //      try {
    //          database.query("pietje");
    //          fail("exception handling from(pietje) failed");
    //      } catch (Exception e) {
    //          System.out.println("Succesfully caught exception: " + e);
    //      }

    //      try {
    //          database.query("Product").as("p").join("Comp", "p", "components");
    //          fail("should fail because faulty table");
    //      } catch (Exception e) {
    //          System.out.println("Succesfully caught exception: " + e);
    //      }
    //
    //      try {
    //          database.query("Product").as("p").join("Component", "p2", "components");
    //          fail("should fail because faulty toTabel");
    //      } catch (Exception e) {
    //          System.out.println("Succesfully caught exception: " + e);
    //      }
    //
    //      try {
    //          database.queryOld("Product").as("p").join("Component", "p2", "components");
    //          fail("should fail because faulty on although it is an mref");
    //      } catch (Exception e) {
    //          System.out.println("Succesfully caught exception: " + e);
    //      }
    //
    //      try {
    //          database.queryOld("Product").as("p").join("Component", "p", "comps");
    //          fail("should fail because faulty on");
    //      } catch (Exception e) {
    //          System.out.println("Succesfully caught exception: " + e);
    //      }
    //
    //      try {
    //          database.queryOld("Product").as("p").select("wrongname").as("productName");
    //          fail("should fail because faulty 'select'");
    //      } catch (Exception e) {
    //          System.out.println("Succesfully caught exception: " + e);
    //      }
  }
}
