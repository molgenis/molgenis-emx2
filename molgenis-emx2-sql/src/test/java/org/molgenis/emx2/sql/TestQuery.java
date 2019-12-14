package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;

import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.StopWatch;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Operator.EQUALS;

public class TestQuery {
  static Database database;

  @BeforeClass
  public static void setUp() {
    database = DatabaseFactory.getTestDatabase();

    // createColumn a schema to test with
    Schema s = database.createSchema("TestQuery");

    // createColumn some tables with contents
    String PERSON = "Person";
    Table person = s.createTableIfNotExists(PERSON);
    person.getMetadata().addColumn("ID", INT).primaryKey();
    person.getMetadata().addColumn("First Name", STRING);
    person.getMetadata().addColumn("Last Name", STRING);
    person.getMetadata().addRef("Father", PERSON).setNullable(true);
    person.getMetadata().addRef("Mother", PERSON).setNullable(true);
    person.getMetadata().addUnique("First Name", "Last Name");

    Row donald =
        new Row().setInt("ID", 1).setString("First Name", "Donald").setString("Last Name", "Duck");
    Row katrien =
        new Row().setInt("ID", 2).setString("First Name", "Katrien").setString("Last Name", "Duck");

    Row kwik =
        new Row()
            .setInt("ID", 3)
            .setString("First Name", "Kwik")
            .setString("Last Name", "Duck")
            .setInt("Father", 1)
            .setInt("Mother", 2);
    Row kwek =
        new Row()
            .setInt("ID", 4)
            .setString("First Name", "Kwek")
            .setString("Last Name", "Duck")
            .setInt("Father", 1)
            .setInt("Mother", 2);
    Row kwak =
        new Row()
            .setInt("ID", 5)
            .setString("First Name", "Kwak")
            .setString("Last Name", "Duck")
            .setInt("Father", 1)
            .setInt("Mother", 2);

    Row mickey =
        new Row().setInt("ID", 6).setString("First Name", "Mickey").setString("Last Name", "Mouse");
    Row minie =
        new Row().setInt("ID", 7).setString("First Name", "Minie").setString("Last Name", "Mouse");

    person.insert(donald, katrien, kwik, kwek, kwak);
  }

  @Test
  public void testQuery1() {

    StopWatch.start("testQuery1");

    Schema s = database.getSchema("TestQuery");

    StopWatch.print("got schema");

    Query q = s.getTable("Person").query();
    q.select("First Name", "Last Name", "Father/First Name", "Father/Last Name");
    q.where("Last Name", EQUALS, "Duck").and("Father/First Name", EQUALS, "Donald");

    StopWatch.print("created query");

    List<Row> rows = q.retrieve();
    for (Row r : rows) {
      System.out.println(r);
    }

    // count kwik, kwek and kwak
    assertEquals(3, rows.size());

    StopWatch.print("query complete");

    q = s.getTable("Person").query();
    q.select("First Name")
        .select("Last Name")
        .expand("Father")
        .select("Last Name")
        .select("First Name");
    q.where("Last Name", EQUALS, "Duck").and("Father/Last Name", EQUALS, "Donald");

    rows = q.retrieve();
    // TODO this should succeed    assertEquals(3, rows.size());

    StopWatch.print("created query second time, to check caching effects");
  }

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
