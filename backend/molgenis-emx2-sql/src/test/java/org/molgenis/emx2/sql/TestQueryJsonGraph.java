package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Operator.TRIGRAM_SEARCH;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.utils.StopWatch;

public class TestQueryJsonGraph {

  static Database db;
  static Schema schema;

  @BeforeAll
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();

    schema = db.dropCreateSchema(TestQueryJsonGraph.class.getSimpleName());

    new PetStoreLoader().load(schema, true);

    schema.create(
        table("Person")
            .add(column("name").setPkey())
            .add(column("father").setType(REF).setRefTable("Person"))
            .add(column("mother").setType(REF).setRefTable("Person"))
            .add(column("children").setType(REF_ARRAY).setRefTable("Person"))
            .add(column("cousins").setType(REF_ARRAY).setRefTable("Person")));

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

    /*
    <pre>
    name | father | mother | children | cousins
    opa1 |        |        | ma,pa    | opa2
    opa2
    oma1
    oma2
    ma   | opa2   | oma2   | kind
    pa   | opa1   | opa1   | kind
    kind | pa     | ma
    </pre>
     */
  }

  @Test
  public void testQuery() {

    StopWatch.print("begin");

    Query s = schema.getTable("Pet").query();
    s.select(s("name"), s("status"), s("category", s("name")));
    String result = s.retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("spike"));

    s = schema.getTable("Person").query();

    s.select(
        s("name"),
        s("father", s("name"), s("father", s("name")), s("mother", s("name"))),
        s("mother", s("name"), s("father", s("name")), s("mother", s("name"))),
        s("children", s("name"), s("children", s("name"))),
        s("children_agg", s("count")),
        s("cousins", s("name"), s("cousins", s("name"))));

    result = s.retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("\"children\": [{\"name\": \"kind\"}]}"));

    // smoke test limit offset
    s = schema.getTable("Person").query();
    s.limit(2).offset(1).retrieveJSON();

    StopWatch.print("complete");
  }

  @Test
  public void testSearch() {
    Query s = this.schema.getTable("Person").query();
    s.select(s("name"));
    s.search("opa");

    String result = s.retrieveJSON();
    System.out.println("search for 'opa':\n " + result);
    assertTrue(result.contains("opa"));

    s = schema.getTable("Person").query();
    s.select(s("name"), s("children", s("name"), s("children", s("name"))), s("father", s("name")));

    s.search("opa");

    result = s.retrieveJSON();
    System.out.println("search for 'opa' also in grandparents:\n " + result);
    assertTrue(result.contains("opa"));

    StopWatch.print("complete");

    s.where(
        f("name", EQUALS, "opa1"),
        f("children", f("name", EQUALS, "ma"), f("children", f("name", EQUALS, "kind"))));

    result = s.retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("opa1"));

    s.where(f("children", f("children", f("name", EQUALS, "kind"))));
    result = s.retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("kind"));

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

    s = schema.query("Person");
    s.select(s("name"));
    s.where(f("name", TRIGRAM_SEARCH, "opa"));

    result = s.retrieveJSON();
    System.out.println();
    assertTrue(result.contains("opa"));
  }

  @Test
  public void testAgg() {
    Schema schema = db.dropCreateSchema(TestQueryJsonGraph.class.getSimpleName() + "_testAgg");
    new PetStoreLoader().load(schema, true);

    String json = schema.query("Order_agg", s("max", s("quantity"))).retrieveJSON();
    assertTrue(json.contains("{\"Order_agg\": {\"max\": {\"quantity\": 7}}}"));
  }

  @Test
  public void testGroupBy() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();

    //  test against pet store for references
    Schema petStore = db.getSchema(TestQueryJsonGraph.class.getSimpleName());

    try {
      petStore.groupBy("Pet").select(s("category", s("name"))).retrieveJSON();
      fail("should fail if no count provided");
    } catch (Exception e) {
      // correct
    }

    Map<String, List<Map<String, Object>>> result =
        mapper.readValue(
            petStore.groupBy("Pet").select(s("count"), s("category", s("name"))).retrieveJSON(),
            Map.class);
    assertEquals(1, result.get("Pet_groupBy").get(0).get("count"));
    assertEquals("ant", ((Map) result.get("Pet_groupBy").get(0).get("category")).get("name"));
    result =
        mapper.readValue(
            petStore.groupBy("Pet").select(s("count"), s("tags", s("name"))).retrieveJSON(),
            Map.class);
    assertEquals(1, result.get("Pet_groupBy").get(4).get("count"));
    assertEquals(null, ((Map) result.get("Pet_groupBy").get(4).get("tags")));
    assertEquals(1, result.get("Pet_groupBy").get(0).get("count"));
    assertEquals("blue", ((Map) result.get("Pet_groupBy").get(0).get("tags")).get("name"));
    assertEquals(3, result.get("Pet_groupBy").get(1).get("count"));
    assertEquals("green", ((Map) result.get("Pet_groupBy").get(1).get("tags")).get("name"));

    // tests below use non-reference types, do we want to enable group by on those??
    Schema schema = db.dropCreateSchema(TestQueryJsonGraph.class.getSimpleName() + "_testGroupBy");
    schema.create(
        table(
            "Test",
            column("id").setPkey(),
            column("tag"),
            column("tag_array").setType(STRING_ARRAY),
            column("tag_array2").setType(STRING_ARRAY)));
    schema
        .getTable("Test")
        .insert(
            row(
                "id",
                1,
                "tag",
                "blue",
                "tag_array",
                new String[] {"blue", "green"},
                "tag_array2",
                new String[] {"yellow", "red"}),
            row(
                "id",
                2,
                "tag",
                "blue",
                "tag_array",
                new String[] {"green", "blue"},
                "tag_array2",
                new String[] {"yellow", "red"}),
            row(
                "id",
                3,
                "tag",
                "green",
                "tag_array",
                new String[] {"blue"},
                "tag_array2",
                new String[] {"yellow", "red"}));

    // group by tag
    result =
        mapper.readValue(
            schema.groupBy("Test").select(s("count"), s("tag")).retrieveJSON(), Map.class);
    assertEquals(1, result.get("Test_groupBy").get(1).get("count"));

    // group by the elements of tag_array
    result =
        mapper.readValue(
            schema.groupBy("Test").select(s("count"), s("tag_array")).retrieveJSON(), Map.class);
    assertEquals(3, result.get("Test_groupBy").get(0).get("count"));

    // group by multiple columns, with tag_array and tag_array2
    result =
        mapper.readValue(
            schema
                .groupBy("Test")
                .select(s("count"), s("tag_array"), s("tag_array2"))
                .retrieveJSON(),
            Map.class);
    assertEquals(3, result.get("Test_groupBy").get(0).get("count"));

    // create a simple test table to pet store just to make sure
    Table table =
        petStore.create(
            table("testGroupBy")
                .add(column("id").setPkey())
                .add(column("col1").setType(REF).setRefTable("Category"))
                .add(column("col2").setType(REF).setRefTable("Category")));

    table.insert(row("id", "1", "col1", "cat", "col2", "cat"));
    table.insert(row("id", "2", "col1", "cat", "col2", "dog"));
    table.insert(row("id", "3", "col1", "dog", "col2", "cat"));
    table.insert(row("id", "4", "col1", "dog", "col2", "dog"));
    table.insert(row("id", "5", "col1", "dog", "col2", "dog"));

    result =
        mapper.readValue(
            table.groupBy().select(s("count"), s("col1", s("name"))).retrieveJSON(), Map.class);

    assertEquals(2, result.get("TestGroupBy_groupBy").size());
    assertEquals("cat", ((Map) result.get("TestGroupBy_groupBy").get(0).get("col1")).get("name"));
    assertEquals(2, result.get("TestGroupBy_groupBy").get(0).get("count"));
    assertEquals("dog", ((Map) result.get("TestGroupBy_groupBy").get(1).get("col1")).get("name"));
    assertEquals(3, result.get("TestGroupBy_groupBy").get(1).get("count"));

    result =
        mapper.readValue(
            table
                .groupBy()
                .select(s("count"), s("col1", s("name")), s("col2", s("name")))
                .retrieveJSON(),
            Map.class);

    assertEquals(4, result.get("TestGroupBy_groupBy").size());

    assertEquals("cat", ((Map) result.get("TestGroupBy_groupBy").get(0).get("col1")).get("name"));
    assertEquals("cat", ((Map) result.get("TestGroupBy_groupBy").get(0).get("col2")).get("name"));
    assertEquals(1, result.get("TestGroupBy_groupBy").get(0).get("count"));

    assertEquals("cat", ((Map) result.get("TestGroupBy_groupBy").get(1).get("col1")).get("name"));
    assertEquals("dog", ((Map) result.get("TestGroupBy_groupBy").get(1).get("col2")).get("name"));
    assertEquals(1, result.get("TestGroupBy_groupBy").get(1).get("count"));

    assertEquals("dog", ((Map) result.get("TestGroupBy_groupBy").get(2).get("col1")).get("name"));
    assertEquals("cat", ((Map) result.get("TestGroupBy_groupBy").get(2).get("col2")).get("name"));
    assertEquals(1, result.get("TestGroupBy_groupBy").get(2).get("count"));

    assertEquals("dog", ((Map) result.get("TestGroupBy_groupBy").get(3).get("col1")).get("name"));
    assertEquals("dog", ((Map) result.get("TestGroupBy_groupBy").get(3).get("col2")).get("name"));
    assertEquals(2, result.get("TestGroupBy_groupBy").get(3).get("count"));
  }
}
