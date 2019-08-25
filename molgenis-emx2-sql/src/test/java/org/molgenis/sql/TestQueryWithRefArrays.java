package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.data.Database;
import org.molgenis.data.Row;
import org.molgenis.data.Table;
import org.molgenis.emx2.examples.ProductComponentPartsExample;
import org.molgenis.query.Query;
import org.molgenis.data.Schema;
import org.molgenis.utils.StopWatch;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.molgenis.metadata.Type.STRING;

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
    personTable
        .getMetadata()
        .addColumn("First Name", STRING)
        .addRef("Father", PERSON)
        .setNullable(true)
        .addColumn("Last Name", STRING)
        .addUnique("First Name", "Last Name");

    Row father = new Row().setString("First Name", "Donald").setString("Last Name", "Duck");
    Row child =
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

    ProductComponentPartsExample.create(schema.getMetadata());
    ProductComponentPartsExample.populate(schema);

    StopWatch.print("tables created");

    Query q = schema.query("Product");
    q.select("name")
        .expand("components")
        .include("name")
        .expand("components", "parts")
        .include("name");

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
    q2.select("name")
        .expand("components")
        .include("name")
        .expand("components", "parts")
        .include("name");

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
