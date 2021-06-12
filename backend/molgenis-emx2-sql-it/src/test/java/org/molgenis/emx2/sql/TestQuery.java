package org.molgenis.emx2.sql;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.SelectColumn.s;

import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.StopWatch;

public class TestQuery {
  static Database database;
  static Schema schema;
  static final String PERSON = "Person";

  @BeforeClass
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();

    // createColumn a schema to test with
    schema = database.dropCreateSchema("TestQuery");

    // createColumn some tables with contents
    Table person =
        schema.create(
            TableMetadata.table(PERSON)
                .add(Column.column("ID").setType(ColumnType.INT).setPkey())
                .add(Column.column("First_Name").setKey(2).setRequired(true))
                .add(Column.column("Last_Name").setKey(2).setRequired(true))
                .add(Column.column("Father").setType(ColumnType.REF).setRefTable(PERSON))
                .add(Column.column("Mother").setType(ColumnType.REF).setRefTable(PERSON)));

    Row donald =
        new Row().setInt("ID", 1).setString("First_Name", "Donald").setString("Last_Name", "Duck");
    Row katrien =
        new Row().setInt("ID", 2).setString("First_Name", "Katrien").setString("Last_Name", "Duck");

    Row kwik =
        new Row()
            .setInt("ID", 3)
            .setString("First_Name", "Kwik")
            .setString("Last_Name", "Duck")
            .setInt("Father", 1)
            .setInt("Mother", 2);
    Row kwek =
        new Row()
            .setInt("ID", 4)
            .setString("First_Name", "Kwek")
            .setString("Last_Name", "Duck")
            .setInt("Father", 1)
            .setInt("Mother", 2);
    Row kwak =
        new Row()
            .setInt("ID", 5)
            .setString("First_Name", "Kwak")
            .setString("Last_Name", "Duck")
            .setInt("Father", 1)
            .setInt("Mother", 2);

    Row mickey =
        new Row().setInt("ID", 6).setString("First_Name", "Mickey").setString("Last_Name", "Mouse");
    Row minie =
        new Row().setInt("ID", 7).setString("First_Name", "Minie").setString("Last_Name", "Mouse");

    person.insert(donald, katrien, kwik, kwek, kwak);
  }

  @Test
  public void testQuery0() {
    StopWatch.start("testQuery1");

    Schema s = database.getSchema("TestQuery");

    StopWatch.print("got schema");

    Query q =
        s.query(
            "Person",
            SelectColumn.s("First_Name"),
            SelectColumn.s("Last_Name"),
            SelectColumn.s("Father", SelectColumn.s("First_Name"), SelectColumn.s("Last_Name")));
    q.where(
        FilterBean.f("Last_Name", Operator.EQUALS, "Duck"),
        FilterBean.f("Father", FilterBean.f("First_Name", Operator.EQUALS, "Donald")));

    System.out.println(q.retrieveJSON());

    Assert.assertEquals(3, q.retrieveRows().size());
  }

  @Test
  public void testQuery1() {

    StopWatch.start("testQuery1");

    Schema s = database.getSchema("TestQuery");

    StopWatch.print("got schema");

    Query q = s.getTable("Person").query();
    q.select(
        SelectColumn.s("First_Name"),
        SelectColumn.s("Last_Name"),
        SelectColumn.s("Father", SelectColumn.s("First_Name"), SelectColumn.s("Last_Name")));
    q.where(
        FilterBean.f("Last_Name", Operator.EQUALS, "Duck"),
        FilterBean.f("Father", FilterBean.f("First_Name", Operator.EQUALS, "Donald")));

    StopWatch.print("created query");

    List<Row> rows = q.retrieveRows();
    for (Row r : rows) {
      System.out.println(r);
    }

    // count kwik, kwek and kwak
    assertEquals(3, rows.size());

    StopWatch.print("query complete");

    q = s.getTable("Person").query();
    q.select(
        SelectColumn.s("First_Name"),
        SelectColumn.s("Last_Name"),
        SelectColumn.s("Father", SelectColumn.s("Last_Name"), SelectColumn.s("First_Name")));
    q.where(
        FilterBean.f("Last_Name", Operator.EQUALS, "Duck"),
        FilterBean.f("Father", FilterBean.f("First_Name", Operator.EQUALS, "Donald")));

    rows = q.retrieveRows();
    assertEquals(3, rows.size());

    StopWatch.print("created query second time, to check caching effects");
  }

  @Test
  public void newQueryTest() {
    List<Row> rows =
        schema
            .getTable(PERSON)
            .select(
                SelectColumn.s("ID"),
                SelectColumn.s("First_Name"),
                SelectColumn.s("Last_Name"),
                SelectColumn.s("Mother").select("ID", "First_Name", "Last_Name"))
            .where(FilterBean.f("Mother", FilterBean.f("ID", Operator.EQUALS, 2)))
            .limit(1)
            .offset(1)
            .retrieveRows();

    assertEquals(1, rows.size());
    Assert.assertEquals((Integer) 2, rows.get(0).getInteger("Mother-ID"));
  }
}
