package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.utils.StopWatch;

import static org.molgenis.emx2.sql.Filter.f;
import static org.molgenis.emx2.sql.SqlGraphQuery.s;

public class TestGraphJsonQuery {

  static Database db;
  static Schema schema;

  @BeforeClass
  public static void setup() {
    db = DatabaseFactory.getTestDatabase();

    schema = db.createSchema("TestJsonQuery");

    PetStoreExample.create(schema.getMetadata());
    PetStoreExample.populate(schema);

    TableMetadata person = schema.createTableIfNotExists("Person").getMetadata();
    person.addColumn("name").primaryKey();
    person.addRef("father", "Person");
    person.addRef("mother", "Person");
    person.addRefArray("children", "Person");
    person.addRefArray("cousins", "Person");

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

    SqlGraphQuery s = new SqlGraphQuery(schema.getTable("Pet"));
    s.select(s("items", s("name"), s("status"), s("category", s("name"))));
    System.out.println(s.retrieve());

    s = new SqlGraphQuery(schema.getTable("Person"));

    s.select(
        s(
            "items",
            s("name"),
            s("father", s("name"), s("father", s("name")), s("mother", s("name"))),
            s("mother", s("name"), s("father", s("name")), s("mother", s("name"))),
            s("children", s("count"), s("items", s("name"), s("children", s("items", s("name"))))),
            s("cousins", s("items", s("name"), s("cousins", s("items", s("name")))))));

    System.out.println(s.retrieve());

    StopWatch.print("complete");
  }

  @Test
  public void testSearch() {

    SqlGraphQuery s = new SqlGraphQuery(schema.getTable("Person"));
    s.select(s("items", s("name")));
    s.search("opa");
    System.out.println("search for 'opa':\n " + s.retrieve());

    s = new SqlGraphQuery(schema.getTable("Person"));
    s.select(s("items", s("name"), s("children", s("items", s("name"))), s("father", s("name"))));
    s.search("opa");
    System.out.println("search for 'opa' also in grandparents:\n " + s.retrieve());

    StopWatch.print("complete");

    s.filter(
            f("name").is("opa1"),
            f("children", f("children", f("name").is("kind")), f("name").is("ma")).limit(1))
        .limit(10);

    System.out.println(s.retrieve());

    s.filter(f("children", f("children", f("name").is("kind"))));

    System.out.println(s.retrieve());

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

    s = new SqlGraphQuery(schema.getTable("Person"));
    s.select(s("items", s("name")));
    s.filter(f("name").similar("opa"));

    System.out.println(s.retrieve());
  }
}
