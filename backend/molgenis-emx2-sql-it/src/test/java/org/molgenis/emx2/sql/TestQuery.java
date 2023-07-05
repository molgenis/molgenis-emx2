package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.StopWatch;

public class TestQuery {
  static Database database;
  static Schema schema;
  static final String PERSON = "Person";

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();

    // createColumn a schema to test with
    schema = database.dropCreateSchema("TestQuery");

    // createColumn some tables with contents
    Table person =
        schema.create(
            table(PERSON)
                .add(column("ID").setType(INT).setPkey())
                .add(column("First_Name").setKey(2).setRequired(true))
                .add(column("Last_Name").setKey(2).setRequired(true))
                .add(column("Father").setType(REF).setRefTable(PERSON))
                .add(column("Mother").setType(REF).setRefTable(PERSON)));

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
            s("First_Name"),
            s("Last_Name"),
            s("Father", s("First_Name"), s("Last_Name")));
    q.where(f("Last_Name", EQUALS, "Duck"), f("Father", f("First_Name", EQUALS, "Donald")));

    System.out.println(q.retrieveJSON());

    assertEquals(3, q.retrieveRows().size());
  }

  @Test
  public void testQuery1() {

    StopWatch.start("testQuery1");

    Schema s = database.getSchema("TestQuery");

    StopWatch.print("got schema");

    Query q = s.getTable("Person").query();
    q.select(s("First_Name"), s("Last_Name"), s("Father", s("First_Name"), s("Last_Name")));
    q.where(f("Last_Name", EQUALS, "Duck"), f("Father", f("First_Name", EQUALS, "Donald")));

    StopWatch.print("created query");

    List<Row> rows = q.retrieveRows();
    for (Row r : rows) {
      System.out.println(r);
    }

    // count kwik, kwek and kwak
    assertEquals(3, rows.size());

    StopWatch.print("query complete");

    q = s.getTable("Person").query();
    q.select(s("First_Name"), s("Last_Name"), s("Father", s("Last_Name"), s("First_Name")));
    q.where(f("Last_Name", EQUALS, "Duck"), f("Father", f("First_Name", EQUALS, "Donald")));

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
                s("ID"),
                s("First_Name"),
                s("Last_Name"),
                s("Mother").select("ID", "First_Name", "Last_Name"))
            .where(f("Mother", f("ID", EQUALS, 2)))
            .limit(1)
            .offset(1)
            .retrieveRows();

    assertEquals(1, rows.size());
    assertEquals((Integer) 2, rows.get(0).getInteger("Mother-ID"));
  }

  @Test
  void  orderByRefColumnAsc() {
    final String unorderd = schema
            .getTable(PERSON)
            .select(

                    s("ID"),
                    s("First_Name"),
                    s("Last_Name"))

            .retrieveRows()
            .stream()
            .map(r -> r.getString("First_Name"))
            .collect(Collectors.joining(","));


    final String orderdbyRefDefaultOrder = schema
            .getTable(PERSON)
            .select(
                s("ID"),
                s("First_Name"),
                s("Last_Name"))
            .orderBy("Mother")
            .retrieveRows()
                .stream()
                .map(r -> r.getString("First_Name"))
                .collect(Collectors.joining(","));


    final String orderdbyRefAsc = schema
            .getTable(PERSON)
            .select(
                    s("ID"),
                    s("First_Name"),
                    s("Last_Name"))
            .orderBy("Mother", Order.ASC)
            .retrieveRows()
            .stream()
            .map(r -> r.getString("First_Name"))
            .collect(Collectors.joining(","));


    final String orderdbyRefDesc = schema
            .getTable(PERSON)
            .select(
                    s("ID"),
                    s("First_Name"),
                    s("Last_Name"))
            .orderBy("Mother", Order.DESC)
            .retrieveRows()
            .stream()
            .map(r -> r.getString("First_Name"))
            .collect(Collectors.joining(","));

    assertEquals("Donald,Katrien,Kwik,Kwek,Kwak", unorderd);
    assertEquals("Kwik,Kwek,Kwak,Donald,Katrien", orderdbyRefDefaultOrder);
    assertEquals("Kwik,Kwek,Kwak,Donald,Katrien", orderdbyRefAsc);
    assertEquals("Donald,Katrien,Kwik,Kwek,Kwak", orderdbyRefDesc);
  }




}
