package org.molgenis.emx2.sql;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.utils.StopWatch;

public class TestQueryJsonGraph {

  static Database db;
  static Schema schema;

  @BeforeClass
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();

    schema = db.dropCreateSchema(TestQueryJsonGraph.class.getSimpleName());

    PetStoreExample.create(schema.getMetadata());
    PetStoreExample.populate(schema);

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
    assertTrue(result.contains("\"children\":[{\"name\":\"kind\"}]}"));

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
  public void testGroupBy() throws JsonProcessingException {
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

    ObjectMapper mapper = new ObjectMapper();

    Map<String, Map<String, List<Map<String, Object>>>> result =
        mapper.readValue(
            schema.agg("Test").select(s("groupBy", s("count"), s("tag"))).retrieveJSON(),
            Map.class);
    assertEquals(2, result.get("Test_agg").get("groupBy").get(1).get("count"));

    result =
        mapper.readValue(
            schema.agg("Test").select(s("groupBy", s("count"), s("tag_array"))).retrieveJSON(),
            Map.class);
    assertEquals(2, result.get("Test_agg").get("groupBy").get(0).get("count"));

    result =
        mapper.readValue(
            schema
                .agg("Test")
                .select(s("groupBy", s("count"), s("tag_array"), s("tag_array2")))
                .retrieveJSON(),
            Map.class);
    assertEquals(3, result.get("Test_agg").get("groupBy").get(0).get("count"));
  }
}
