package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.utils.StopWatch;

import java.util.List;

import static org.molgenis.emx2.sql.Filter.f;

public class TestGraphJsonQuery {

  static Database db;

  @BeforeClass
  public static void setup() {
    db = DatabaseFactory.getTestDatabase();
  }

  @Test
  public void testQuery() {

    Schema schema = db.createSchema("TestJsonQuery");

    PetStoreExample.create(schema.getMetadata());
    PetStoreExample.populate(schema);

    SqlGraphJsonQuery s =
        new SqlGraphJsonQuery((SqlTableMetadata) schema.getTable("Pet").getMetadata());
    s.select(
        List.of(
            "items",
            List.of("name", "status", "category", List.of("name")))); // , "tag", List.of("name"));
    System.out.println(s.retrieve());

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

    StopWatch.print("begin");
    s = new SqlGraphJsonQuery((SqlTableMetadata) schema.getTable("Person").getMetadata());

    s.select(
        List.of(
            "items",
            List.of(
                "name",
                "father",
                List.of("name", "father", List.of("name"), "mother", List.of("name")),
                "mother",
                List.of("name", "father", List.of("name"), "mother", List.of("name")),
                "children",
                List.of(
                    "count",
                    "items",
                    List.of("name", "children", List.of("items", List.of("name")))),
                "cousins",
                List.of("items", List.of("name", "cousins", List.of("items", List.of("name")))))));

    System.out.println(s.retrieve());

    s.search("opa");
    System.out.println("search for 'opa':\n " + s.retrieve());

    StopWatch.print("complete");

    s.filter(
            f("name").eq("opa1"),
            f("children", f("children", f("name").eq("kind")), f("name").eq("ma")).limit(1))
        .limit(10);

    System.out.println(s.retrieve());

    s.filter(f("children", f("children", f("name").eq("kind"))));

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
  }
}
