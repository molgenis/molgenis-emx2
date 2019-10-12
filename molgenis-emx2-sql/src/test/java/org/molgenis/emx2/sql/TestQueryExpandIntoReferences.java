package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.examples.ProductComponentPartsExample;
import org.molgenis.emx2.utils.StopWatch;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Operator.EQUALS;

public class TestQueryExpandIntoReferences {
  static Database db;

  @BeforeClass
  public static void setUp() throws SQLException {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");

    // createColumn a schema to test with
    Schema schema = db.createSchema("TestQueryWithRefArray");

    // createColumn some tables with contents
    String PERSON = "Person";
    Table personTable = schema.createTableIfNotExists(PERSON);
    personTable
        .getMetadata()
        .addColumn("ID", ColumnType.INT)
        .primaryKey()
        .addColumn("First Name", STRING)
        .addRef("Father", PERSON)
        .setNullable(true)
        .addColumn("Last Name", STRING)
        .addUnique("First Name", "Last Name");

    Row father =
        new Row().setInt("ID", 1).setString("First Name", "Donald").setString("Last Name", "Duck");
    Row child =
        new Row()
            .setInt("ID", 2)
            .setString("First Name", "Kwik")
            .setString("Last Name", "Duck")
            .setInt("Father", father.getInteger("ID"));

    personTable.insert(father);
    personTable.insert(child);
  }

  @Test
  public void canExpandQueryInReferences() {

    StopWatch.start("canExpandQueryInReferences");

    Schema schema = db.getSchema("TestQueryWithRefArray");

    StopWatch.print("got schema");

    Query query1 =
        schema
            .query("Person")
            .select("First Name")
            .select("Last Name")
            .expand("Father")
            .select("First Name")
            .select("Last Name")
            .where("Last Name", EQUALS, "Duck")
            .and("Father/Last Name", EQUALS, "Duck");

    StopWatch.print("created query");

    List<Row> rows = query1.retrieve();
    for (Row r : rows) {
      System.out.println(r);
    }

    StopWatch.print("query complete");

    query1 =
        schema
            .query("Person")
            .select("First Name")
            .select("Last Name")
            .expand("Father")
            .select("Last Name")
            .select("First Name")
            .where("Last Name", EQUALS, "Duck")
            .and("Father/Last Name", EQUALS, "Duck");

    rows = query1.retrieve();
    for (Row r : rows) System.out.println(r);
    assertEquals(1, rows.size());

    StopWatch.print("second time");
  }

  @Test
  public void CanQueryExpandIntoArrayForeignKeys() {
    Schema schema = db.getSchema("TestQueryWithRefArray");

    StopWatch.start("CanQueryExpandIntoArrayForeignKeys");

    ProductComponentPartsExample.create(schema.getMetadata());
    ProductComponentPartsExample.populate(schema);

    StopWatch.print("tables created");

    Query q = schema.query("Product");
    q.select("name").expand("components").select("name").expand("parts").select("name");

    List<Row> rows = q.retrieve();
    assertEquals(3, rows.size());
    for (Row r : rows) {
      System.out.println(r);
    }

    StopWatch.print("query completed");

    // restart database and see if it is still there

    db.clearCache();
    schema = db.getSchema("TestQueryWithRefArray");

    StopWatch.print("cleared cache");

    Query q2 = schema.query("Product");
    q2.select("name").expand("components").select("name").expand("parts").select("name");

    // todo query expansion! q2.where("components", "parts",
    // "weight").eq(50).and("name").eq("explorer", "navigator");

    StopWatch.print("created query (needed to get metadata from disk)");

    List<Row> rows2 = q2.retrieve();
    assertEquals(3, rows2.size());
    for (Row r : rows2) {
      System.out.println(r);
    }

    StopWatch.print("queried again, cached so for free");
  }
}
