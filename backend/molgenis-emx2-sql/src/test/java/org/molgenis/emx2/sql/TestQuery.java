package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.StopWatch;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

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
            table(PERSON)
                .add(column("ID").type(INT).pkey())
                .add(column("First_Name").key(2))
                .add(column("Last_Name").key(2))
                .add(column("Father").type(REF).refTable(PERSON).nullable(true))
                .add(column("Mother").type(REF).refTable(PERSON).nullable(true)));

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
  public void testQuery1() {

    StopWatch.start("testQuery1");

    Schema s = database.getSchema("TestQuery");

    StopWatch.print("got schema");

    Query q = s.getTable("Person").query();
    q.select(s("First_Name"), s("Last_Name"), s("Father", s("First_Name"), s("Last_Name")));
    q.filter("Last_Name", EQUALS, "Duck").filter("Father", f("First_Name", EQUALS, "Donald"));

    StopWatch.print("created query");

    List<Row> rows = q.getRows();
    for (Row r : rows) {
      System.out.println(r);
    }

    // count kwik, kwek and kwak
    assertEquals(3, rows.size());

    StopWatch.print("query complete");

    q = s.getTable("Person").query();
    q.select("First_Name", "Last_Name")
        .select(s("Father").select("Last_Name").select("First_Name"));
    q.filter("Last_Name", EQUALS, "Duck").filter("Father", f("First_Name", EQUALS, "Donald"));

    rows = q.getRows();
    assertEquals(3, rows.size());

    StopWatch.print("created query second time, to check caching effects");
  }

  @Test
  public void newQueryTest() {
    List<Row> rows =
        schema
            .getTable(PERSON)
            .select(
                s("ID"),
                s("First_Name"),
                s("Last_Name"),
                s("Mother").select("ID", "First_Name", "Last_Name"))
            .filter("Mother", f("ID", EQUALS, 2))
            .setLimit(1)
            .setOffset(1)
            .getRows();

    assertEquals(1, rows.size());
    assertEquals((Integer) 2, rows.get(0).getInteger("Mother/ID"));
  }
}
