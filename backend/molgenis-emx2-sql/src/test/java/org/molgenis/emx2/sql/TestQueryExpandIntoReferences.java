package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.examples.ProductComponentPartsExample;
import org.molgenis.emx2.utils.StopWatch;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

public class TestQueryExpandIntoReferences {
  static Database db;

  @BeforeClass
  public static void setUp() throws SQLException {
    db = TestDatabaseFactory.getTestDatabase();

    // createColumn a schema to test with
    Schema schema = db.createSchema("TestQueryWithRefArray");

    // createColumn some tables with contents
    String PERSON = "Person";
    schema
        .getMetadata()
        .create(
            table(PERSON)
                .add(column("ID").type(INT))
                .add(column("First Name"))
                .add(column("Father").type(REF).refTable(PERSON).nullable(true))
                .add(column("Last Name"))
                .addUnique("First Name", "Last Name")
                .pkey("ID"));

    Row father =
        new Row().setInt("ID", 1).setString("First Name", "Donald").setString("Last Name", "Duck");
    Row child =
        new Row()
            .setInt("ID", 2)
            .setString("First Name", "Kwik")
            .setString("Last Name", "Duck")
            .setInt("Father", father.getInteger("ID"));

    Table personTable = schema.getTable("Person");
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
            .select(s("Father").select("First Name").select("Last Name"))
            .filter("Last Name", EQUALS, "Duck")
            .filter("Father", f("Last Name", EQUALS, "Duck"));

    StopWatch.print("created query");

    List<Row> rows = query1.getRows();
    for (Row r : rows) {
      System.out.println(r);
    }

    StopWatch.print("query complete");

    query1 =
        schema
            .query("Person")
            .select("First Name")
            .select("Last Name")
            .select(s("Father").select("Last Name").select("First Name"))
            .filter("Last Name", EQUALS, "Duck")
            .filter("Father", f("Last Name", EQUALS, "Duck"));

    rows = query1.getRows();
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
    q.select("name").select(s("components").select("name").select(s("parts").select("name")));

    List<Row> rows = q.getRows();
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
    q2.select("name").select(s("components").select("name").select(s("parts").select("name")));

    // todo query expansion! q2.where("components", "parts",
    // "weight").eq(50).and("name").eq("explorer", "navigator");

    StopWatch.print("created query (needed to get metadata from disk)");

    List<Row> rows2 = q2.getRows();
    assertEquals(3, rows2.size());
    for (Row r : rows2) {
      System.out.println(r);
    }

    StopWatch.print("queried again, cached so for free");
  }
}
