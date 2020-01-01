package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.utils.StopWatch;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Operator.TRIGRAM_SEARCH;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.SelectColumn.s;

public class TestGraphJsonQuery {

  static Database db;
  static Schema schema;

  @BeforeClass
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();

    schema = db.createSchema("TestJsonQuery");

    PetStoreExample.create(schema.getMetadata());
    PetStoreExample.populate(schema);

    schema.create(
        table("Person")
            .addColumn(column("name"))
            .addColumn(column("father").type(REF).refTable("Person").nullable(true))
            .addColumn(column("mother").type(REF).refTable("Person").nullable(true))
            .addColumn(column("children").type(REF_ARRAY).refTable("Person"))
            .addColumn(column("cousins").type(REF_ARRAY).refTable("Person"))
            .setPrimaryKey("name"));

    schema
        .getTable("Person")
        .insert(
            new Row()
                .set("name", "opa1")
                .set("children", "ma,pa")
                .set("cousins", "opa2"), // sorry for this example
            new Row().set("name", "opa2"),
            new Row().set("name", "oma1"),
            new Row().set("name", "oma2"),
            new Row()
                .set("name", "ma")
                .set("father", "opa2")
                .set("mother", "oma2")
                .set("children", "kind"),
            new Row()
                .set("name", "pa")
                .set("father", "opa1")
                .set("mother", "oma1")
                .set("children", "kind"),
            new Row().set("name", "kind").set("father", "pa").set("mother", "ma"));
  }

  @Test
  public void testQuery() {

    StopWatch.print("begin");

    Query s = schema.getTable("Pet").query();
    s.select(s("items", s("name"), s("status"), s("category", s("name"))));
    System.out.println(s.retrieveJsonGraph());

    s = schema.getTable("Person").query();

    s.select(
        s(
            "items",
            s("name"),
            s("father", s("name"), s("father", s("name")), s("mother", s("name"))),
            s("mother", s("name"), s("father", s("name")), s("mother", s("name"))),
            s("children", s("count"), s("items", s("name"), s("children", s("items", s("name"))))),
            s("cousins", s("items", s("name"), s("cousins", s("items", s("name")))))));

    System.out.println(s.retrieveJsonGraph());

    StopWatch.print("complete");
  }

  @Test
  public void testSearch() {

    Query s = schema.getTable("Person").query();
    s.select(s("items", s("name")));
    s.search("opa");

    System.out.println("search for 'opa':\n " + s.retrieveJsonGraph());

    s = schema.getTable("Person").query();
    s.select(s("items", s("name"), s("children", s("items", s("name"))), s("father", s("name"))));
    s.search("opa");
    System.out.println("search for 'opa' also in grandparents:\n " + s.retrieveJsonGraph());

    StopWatch.print("complete");

    s.filter(
        f("name", EQUALS, "opa1"),
        f("children", f("children", f("name", EQUALS, "kind")), f("name", EQUALS, "ma")));

    System.out.println(s.retrieveJsonGraph());

    s.filter(f("children", f("children", f("name", EQUALS, "kind"))));

    System.out.println(s.retrieveJsonGraph());

    //
    //    s.search("opa");

    // {father : {name: { eq:[opa2]}, mother: {name: {eq: oma1}, father: {name: {eq: opa2}}
    //
    //    s.where(
    //        "name",
    //        eq("pa"),
    //        "father",
    //        Map.of("name", eq("opa2"), "mother", Map.of("name", eq("oma2"))));

    //    s.where("father", "name")
    //        .eq("opa1")
    //        .and("father", "mother", "name")
    //        .eq("opa2")
    //        .and("father", "father", "name")
    //        .eq("opa2");

    StopWatch.print("complete");

    s = schema.getTable("Person").query();
    s.select(s("items", s("name")));
    s.filter(f("name", TRIGRAM_SEARCH, "opa"));

    System.out.println(s.retrieveJsonGraph());
  }
}
