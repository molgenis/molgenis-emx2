package org.molgenis.emx2.sql; // package org.molgenis.sql;
//
// import org.junit.BeforeClass;
// import org.junit.Test;
// import org.molgenis.*;
// import Row;
// import StopWatch;
//
// import java.jooq.SQLException;
// import java.util.List;
//
// import static org.junit.Assert.assertEquals;
// import static Column.Type.INT;
// import static Column.Type.STRING;
// import static Row.MOLGENISID;
//
// public class TestQuery {
//  static Database database;
//
//  @BeforeClass
//  public static void setUp() throws MolgenisException, SQLException {
//    database = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
//
//    // createColumn a schema to test with
//    Schema s = database.createSchema("TestQuery");
//
//    // createColumn some tables with contents
//    String PERSON = "Person";
//    Table person = s.createTableIfNotExists(PERSON);
//    person.addColumn("First Name", STRING);
//    person.addRef("Father", PERSON).setNullable(true);
//    person.addColumn("Last Name", STRING);
//    person.addUnique("First Name", "Last Name");
//
//    Row father =
//        new Row().setString("First Name", "Donald").setString("Last Name", "Duck");
//    Row child =
//        new Row()
//            .setString("First Name", "Kwik")
//            .setString("Last Name", "Duck")
//            .setRef("Father", father);
//
//    person.insert(father);
//    person.insert(child);
//  }
//
//  @Test
//  public void DependencyOrderNotNeededInTransaction() throws MolgenisException {
//
//    StopWatch.start("DependencyOrderNotNeededInTransaction");
//
//    Schema s = database.getSchema("TestQuery");
//
//    StopWatch.print("got schema");
//
//    Query q = s.getTableMetadata("Person").query();
//    q.select("First Name")
//        .select("Last Name")
//        .expand("Father")
//        .include("First Name")
//        .include("Last Name");
//    q.where("Last Name").eq("Duck").and("Father", "Last Name").eq("Duck");
//
//    StopWatch.print("created query");
//
//    List<Row> rows = q.retrieve();
//    for (Row r : rows) {
//      System.out.println(r);
//    }
//
//    StopWatch.print("query complete");
//
//    q = s.getTableMetadata("Person").query();
//    q.select("First Name")
//        .select("Last Name")
//        .expand("Father")
//        .include("Last Name")
//        .include("First Name");
//    q.where("Last Name").eq("Duck").and("Father", "Last Name").eq("Duck");
//
//    rows = q.retrieve();
//
//    StopWatch.print("second time");
//  }
//
//  @Test
//  public void DependencyOrderOutsideTransactionFails() throws MolgenisException {
//    Schema s = database.getSchema("TestQuery");
//
//    StopWatch.start("DependencyOrderOutsideTransactionFails");
//
//    String PART = "Part";
//    Table part = s.createTableIfNotExists(PART);
//    part.addColumn("name", STRING);
//    part.addColumn("weight", INT);
//    part.addUnique("name");
//
//    Row part1 = new Row().setString("name", "forms").setInt("weight", 100);
//    Row part2 = new Row().setString("name", "login").setInt("weight", 50);
//    part.insert(part1);
//    part.insert(part2);
//
//    String COMPONENT = "Component";
//    Table component = s.createTableIfNotExists(COMPONENT);
//    component.addColumn("name", STRING);
//    component.addUnique("name");
//    component.addMref("parts", PART, MOLGENISID, "components", "ComponentPart");
//
//    Row component1 =
//        new Row().setString("name", "explorer").setMref("parts", part1, part2);
//    Row component2 = new Row().setString("name", "navigator").setMref("parts",
// part2);
//    component.insert(component1);
//    component.insert(component2);
//
//    String PRODUCT = "Product";
//    Table product = s.createTableIfNotExists(PRODUCT);
//    product.addColumn("name", STRING);
//    product.addUnique("name");
//    product.addMref("components", COMPONENT, MOLGENISID, "ProductComponent", "products");
//
//    Row product1 =
//        new Row().setString("name", "molgenis").setMref("components", component1, component2);
//
//    product.insert(product1);
//
//    StopWatch.print("tables created");
//
//    // now getQuery to show product.name and parts.name linked by path
// Assembly.product,part
//
//    // needed:
//    // join+columns paths, potentially multiple paths. We only support outer join over
// relationships
//    // if names are not unique, require explicit select naming
//    // complex nested where clauses
//    // sortby clauses
//    // later: group by.
//
//    //        QueryOldImpl q1 = new PsqlQueryBack(database);
//    // database        q1.select("Product").columns("name").as("productName");
//    //        q1.mref("ProductComponent").columns("name").as("componentName");
//    //        q1.mref("ComponentPart").columns("name").as("partName");
//    //        //q1.where("productName").eq("molgenis");
//    //
//    //        System.out.println(q1);
//
//    Query q = s.getTableMetadata("Product").query();
//    q.select("name")
//        .expand("components")
//        .include("name")
//        .expand("components", "parts")
//        .include("name");
//    // q.where("components", "parts", "weight").eq(50).and("name").eq("explorer", "navigator");
//
//    List<Row> rows = q.retrieve();
//    assertEquals(rows.size(), 3);
//    for (Row r : rows) {
//      System.out.println(r);
//    }
//
//    StopWatch.print("query completed");
//
//    // restart database and see if it is still there
//    database.clearCache();
//    s = database.getSchema("TestQuery");
//
//    StopWatch.print("cleared cache");
//
//    Query q2 = s.getTableMetadata("Product").query();
//    q2.select("name")
//        .expand("components")
//        .include("name")
//        .expand("components", "parts")
//        .include("name");
//    // q.where("components", "parts", "weight").eq(50).and("name").eq("explorer", "navigator");
//
//    StopWatch.print("created query (needed to get metadata from disk)");
//
//    List<Row> rows2 = q2.retrieve();
//    assertEquals(rows2.size(), 3);
//    for (Row r : rows2) {
//      System.out.println(r);
//    }
//
//    StopWatch.print("queried again, cached so for free");
//
//    //      try {
//    //          database.query("pietje");
//    //          fail("exception handling from(pietje) failed");
//    //      } catch (Exception e) {
//    //          System.out.println("Succesfully caught exception: " + e);
//    //      }
//
//    //      try {
//    //          database.query("Product").as("p").join("Comp", "p", "components");
//    //          fail("should fail because faulty table");
//    //      } catch (Exception e) {
//    //          System.out.println("Succesfully caught exception: " + e);
//    //      }
//    //
//    //      try {
//    //          database.query("Product").as("p").join("Component", "p2", "components");
//    //          fail("should fail because faulty toTabel");
//    //      } catch (Exception e) {
//    //          System.out.println("Succesfully caught exception: " + e);
//    //      }
//    //
//    //      try {
//    //          database.queryOld("Product").as("p").join("Component", "p2", "components");
//    //          fail("should fail because faulty on although it is an mref");
//    //      } catch (Exception e) {
//    //          System.out.println("Succesfully caught exception: " + e);
//    //      }
//    //
//    //      try {
//    //          database.queryOld("Product").as("p").join("Component", "p", "comps");
//    //          fail("should fail because faulty on");
//    //      } catch (Exception e) {
//    //          System.out.println("Succesfully caught exception: " + e);
//    //      }
//    //
//    //      try {
//    //          database.queryOld("Product").as("p").select("wrongname").as("productName");
//    //          fail("should fail because faulty 'select'");
//    //      } catch (Exception e) {
//    //          System.out.println("Succesfully caught exception: " + e);
//    //      }
//  }
// }
