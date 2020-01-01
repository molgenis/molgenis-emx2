package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;

import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.StopWatch;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.SelectColumn.s;

public class TestQuery {
  static Database database;
  static Schema schema;
  static final String PERSON = "Person";

  @BeforeClass
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();

    // createColumn a schema to test with
    schema = database.createSchema("TestQuery");

    // createColumn some tables with contents
    Table person =
        schema.create(
            table(PERSON)
                .addColumn(column("ID").type(INT))
                .addColumn(column("First Name"))
                .addColumn(column("Last Name"))
                .addColumn(column("Father").type(REF).refTable(PERSON).nullable(true))
                .addColumn(column("Mother").type(REF).refTable(PERSON).nullable(true))
                .addUnique("First Name", "Last Name")
                .setPrimaryKey("ID"));

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
    q.select(s("First Name"), s("Last Name"), s("Father", s("First Name"), s("Last Name")));
    q.filter("Last Name", EQUALS, "Duck").filter("Father", f("First Name", EQUALS, "Donald"));

    StopWatch.print("created query");

    List<Row> rows = q.getRows();
    for (Row r : rows) {
      System.out.println(r);
    }

    // count kwik, kwek and kwak
    assertEquals(3, rows.size());

    StopWatch.print("query complete");

    q = s.getTable("Person").query();
    q.select("First Name")
        .select("Last Name")
        .select(s("Father").select("Last Name").select("First Name"));
    q.filter("Last Name", EQUALS, "Duck").filter("Father", f("First Name", EQUALS, "Donald"));

    rows = q.getRows();
    assertEquals(3, rows.size());

    StopWatch.print("created query second time, to check caching effects");
  }

  @Test
  public void newQueryTest() {
    for (Row row :
        schema
            .getTable(PERSON)
            .select(
                s("ID"),
                s("First Name"),
                s("Last Name"),
                s("Mother", s("ID"), s("First Name"), s("Last Name")))
            .filter(f("Mother", f("ID", EQUALS, 2)))
            .setLimit(1)
            .setOffset(1)
            .getRows()) {
      StopWatch.print(row.toString());
    }
  }
}
