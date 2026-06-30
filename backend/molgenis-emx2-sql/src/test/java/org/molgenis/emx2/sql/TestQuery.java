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

class TestQuery {

  static final String PERSON = "Person";

  static Database database;
  static Schema schema;

  @BeforeAll
  static void setUp() {
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

    person.insert(donald, katrien, kwik, kwek, kwak);
  }

  @Test
  void testQuery0() {
    StopWatch.start("testQuery1");

    Schema s = database.getSchema("TestQuery");

    StopWatch.print("got schema");

    Query q = s.query(PERSON, s("First_Name"), s("Last_Name"), s("Father"));
    q.where(f("Last_Name", EQUALS, "Duck"), f("Father", f("First_Name", EQUALS, "Donald")));

    System.out.println(q.retrieveJSON());

    assertEquals(3, q.retrieveRows().size());
  }

  @Test
  void testQuery1() {

    StopWatch.start("testQuery1");

    Schema s = database.getSchema("TestQuery");

    StopWatch.print("got schema");

    Query q = s.getTable(PERSON).query();
    q.select(s("First_Name"), s("Last_Name"), s("Father"));
    q.where(f("Last_Name", EQUALS, "Duck"), f("Father", f("First_Name", EQUALS, "Donald")));

    StopWatch.print("created query");

    List<Row> rows = q.retrieveRows();
    for (Row r : rows) {
      System.out.println(r);
    }

    // count kwik, kwek and kwak
    assertEquals(3, rows.size());

    StopWatch.print("query complete");

    q = s.getTable(PERSON).query();
    q.select(s("First_Name"), s("Last_Name"), s("Father"));
    q.where(f("Last_Name", EQUALS, "Duck"), f("Father", f("First_Name", EQUALS, "Donald")));

    rows = q.retrieveRows();
    assertEquals(3, rows.size());

    StopWatch.print("created query second time, to check caching effects");
  }

  @Test
  void newQueryTest() {
    List<Row> rows =
        schema
            .getTable(PERSON)
            .select(s("ID"), s("First_Name"), s("Last_Name"), s("Mother"))
            .where(f("Mother", f("ID", EQUALS, 2)))
            .limit(1)
            .offset(1)
            .retrieveRows();

    assertEquals(1, rows.size());
    assertEquals((Integer) 2, rows.get(0).getInteger("Mother"));

    rows =
        schema
            .getTable(PERSON)
            .select(s("ID"), s("First_Name"), s("Last_Name"), s("Mother"))
            .where(f("Mother", f("First_Name", EQUALS, "Katrien ")))
            .limit(1)
            .offset(1)
            .retrieveRows();
    assertEquals(1, rows.size());
    assertEquals((Integer) 2, rows.get(0).getInteger("Mother"));
  }

  @Test
  void orderByRefColumnAsc() {
    final String unordered =
        schema
            .getTable(PERSON)
            .select(s("ID"), s("First_Name"), s("Last_Name"))
            .retrieveRows()
            .stream()
            .map(r -> r.getString("First_Name"))
            .collect(Collectors.joining(","));

    final String orderedByRefDefaultOrder =
        schema
            .getTable(PERSON)
            .select(s("ID"), s("First_Name"), s("Last_Name"))
            .orderBy("Mother")
            .retrieveRows()
            .stream()
            .map(r -> r.getString("First_Name"))
            .collect(Collectors.joining(","));

    final String orderedByRefAsc =
        schema
            .getTable(PERSON)
            .select(s("ID"), s("First_Name"), s("Last_Name"))
            .orderBy("Mother", Order.ASC)
            .retrieveRows()
            .stream()
            .map(r -> r.getString("First_Name"))
            .collect(Collectors.joining(","));

    final String orderedByRefDesc =
        schema
            .getTable(PERSON)
            .select(s("ID"), s("First_Name"), s("Last_Name"))
            .orderBy("Mother", Order.DESC)
            .retrieveRows()
            .stream()
            .map(r -> r.getString("First_Name"))
            .collect(Collectors.joining(","));

    assertEquals("Donald,Katrien,Kwik,Kwek,Kwak", unordered);
    assertEquals("Kwik,Kwek,Kwak,Donald,Katrien", orderedByRefDefaultOrder);
    assertEquals("Kwik,Kwek,Kwak,Donald,Katrien", orderedByRefAsc);
    assertEquals("Donald,Katrien,Kwik,Kwek,Kwak", orderedByRefDesc);
  }
}
