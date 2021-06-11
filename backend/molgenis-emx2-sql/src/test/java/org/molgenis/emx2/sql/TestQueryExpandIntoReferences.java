package org.molgenis.emx2.sql;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.examples.ProductComponentPartsExample;
import org.molgenis.emx2.utils.StopWatch;

public class TestQueryExpandIntoReferences {
  static Database db;

  @BeforeClass
  public static void setUp() throws SQLException {
    db = TestDatabaseFactory.getTestDatabase();

    // createColumn a schema to test with
    Schema schema = db.dropCreateSchema("TestQueryWithRefArray");

    // createColumn some tables with contents
    String PERSON = "Person";
    schema
        .getMetadata()
        .create(
            TableMetadata.table(PERSON)
                .add(Column.column("ID").setType(ColumnType.INT).setPkey())
                .add(Column.column("First_Name").setKey(2).setRequired(true))
                .add(Column.column("Father").setType(ColumnType.REF).setRefTable(PERSON))
                .add(Column.column("Last_Name").setKey(2).setRequired(true)));

    Row father =
        new Row().setInt("ID", 1).setString("First_Name", "Donald").setString("Last_Name", "Duck");
    Row child =
        new Row()
            .setInt("ID", 2)
            .setString("First_Name", "Kwik")
            .setString("Last_Name", "Duck")
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
            .select(
                SelectColumn.s("First_Name"),
                SelectColumn.s("Last_Name"),
                SelectColumn.s("Father", SelectColumn.s("First_Name"), SelectColumn.s("Last_Name")))
            .where(
                FilterBean.f("Last_Name", Operator.EQUALS, "Duck"),
                FilterBean.f("Father", FilterBean.f("Last_Name", Operator.EQUALS, "Duck")));

    StopWatch.print("created query");

    List<Row> rows = query1.retrieveRows();
    for (Row r : rows) {
      System.out.println(r);
    }

    StopWatch.print("query complete");

    query1 =
        schema
            .query("Person")
            .select(
                SelectColumn.s("First_Name"),
                SelectColumn.s("Last_Name"),
                SelectColumn.s("Father").select("Last_Name").select("First_Name"))
            .where(FilterBean.f("Last_Name", Operator.EQUALS, "Duck"))
            .where(FilterBean.f("Father", FilterBean.f("Last_Name", Operator.EQUALS, "Duck")));

    rows = query1.retrieveRows();
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
    q.select(
        SelectColumn.s("name"),
        SelectColumn.s(
            "components", SelectColumn.s("name"), SelectColumn.s("parts", SelectColumn.s("name"))));

    List<Row> rows = q.retrieveRows();
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
    q2.select(
        SelectColumn.s("name"),
        SelectColumn.s(
            "components", SelectColumn.s("name"), SelectColumn.s("parts", SelectColumn.s("name"))));

    // todo query expansion! q2.where("components", "parts",
    // "weight").eq(50).and("name").eq("explorer", "navigator");

    StopWatch.print("created query (needed to get metadata from disk)");

    List<Row> rows2 = q2.retrieveRows();
    assertEquals(3, rows2.size());
    for (Row r : rows2) {
      System.out.println(r);
    }

    StopWatch.print("queried again, cached so for free");
  }
}
