package org.molgenis.emx2.sql;

import static graphql.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.FilterBean.*;
import static org.molgenis.emx2.Operator.*;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestCompositeForeignKeys {
  private Database database;

  @BeforeEach
  public void setUp() {
    database = TestDatabaseFactory.getTestDatabase();

    // create target table

  }

  @Test
  public void testCompositeRef() throws JsonProcessingException {
    Schema schema =
        database.dropCreateSchema(TestCompositeForeignKeys.class.getSimpleName() + "Ref");

    schema.create(
        table(
            "Person",
            column("firstName").setPkey(),
            column("lastName").setPkey(),
            column("uncle", REF).setRefTable("Person")));

    Table p = schema.getTable("Person");

    p.insert(new Row().setString("firstName", "Donald").setString("lastName", "Duck"));

    try {
      p.insert(
          new Row()
              .setString("firstName", "Kwik")
              .setString("lastName", "Duck")
              .setString("uncle.firstName", "Donald")
              .setString("uncle.lastName", "MISSING"));
      fail("should have failed on missing foreign key");
    } catch (Exception e) {
      System.out.println("errored correctly: " + e);
    }

    try {
      p.insert(
          new Row()
              .setString("firstName", "Kwik")
              .setString("lastName", "Duck")
              .setString("uncle.firstName", "Donald"));
      // missing completely.setString("uncle.lastName", "MISSING"));
      fail("should have failed when part for composite foreign key is null, regression #3876");
    } catch (Exception e) {
      assertEquals(
          "Update into table 'Person' failed.: Transaction failed: Key (uncle.firstName,uncle.lastName)=(Donald,NULL) not present in table \"Person\"",
          e.getMessage());
      System.out.println("errored correctly: " + e);
    }

    p.insert(
        new Row()
            .setString("firstName", "Kwik")
            .setString("lastName", "Duck")
            .setString("uncle.firstName", "Donald")
            .setString("uncle.lastName", "Duck"));

    p.insert(
        new Row()
            .setString("firstName", "Kwek")
            .setString("lastName", "Duck")
            .setString("uncle.firstName", "Donald")
            .setString("uncle.lastName", "Duck"));

    p.insert(
        new Row()
            .setString("firstName", "Kwak")
            .setString("lastName", "Duck")
            .setString("uncle.firstName", "Donald")
            .setString("uncle.lastName", "Duck"));

    try {
      p.delete(new Row().setString("firstName", "Donald").setString("lastName", "Duck"));
      fail("should have failed on foreign key (Donald is used in foreign key)");
    } catch (Exception e) {
      System.out.println("errored correctly: " + e);
    }

    schema.create(table("Student").setInheritName("Person"));
    Table s = schema.getTable("Student");
    s.insert(
        new Row()
            .setString("firstName", "Mickey")
            .setString("lastName", "Mouse")
            .setString("uncle.firstName", "Kwik")
            .setString("uncle.lastName", "Duck"));

    String result =
        schema
            .query(
                "Student",
                s("firstName"),
                s("lastName"),
                s("uncle", s("firstName"), s("lastName")),
                s("uncle", s("firstName"), s("lastName")))
            .retrieveJSON();
    System.out.println(result);

    result =
        schema
            .query("Student")
            .select(s("firstName"), s("lastName"))
            .where(
                or(
                    and(f("firstName", EQUALS, "Donald"), f("lastName", EQUALS, "Duck")),
                    and(f("firstName", EQUALS, "Mickey"), f("lastName", EQUALS, "Mouse"))))
            .retrieveJSON();

    System.out.println(result);
    assertTrue(result.contains("Mouse"));
    assertFalse(result.contains("Duck"));

    result =
        schema
            .query("Person")
            .select(s("firstName"), s("lastName"))
            .where(
                or(
                    and(f("firstName", EQUALS, "Donald"), f("lastName", EQUALS, "Duck")),
                    and(f("firstName", EQUALS, "Mickey"), f("lastName", EQUALS, "Mouse"))))
            .retrieveJSON();

    System.out.println(result);
    assertTrue(result.contains("Mouse"));
    assertTrue(result.contains("Duck"));

    // composite key filter
    Query q =
        schema
            .query("Person")
            .select(s("firstName"), s("lastName"))
            // composite filter, should result in 'donald duck' OR 'mickey mouse'
            .where(
                f(
                    "uncle",
                    or(
                        and(f("firstName", EQUALS, "Donald"), f("lastName", EQUALS, "Duck")),
                        and(f("firstName", EQUALS, "Mickey"), f("lastName", EQUALS, "Mouse")))));
    result = q.retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("Kwik"));
    assertFalse(result.contains("Mouse"));

    List<Row> rows = q.retrieveRows(); // test that nested queries also work
    assertEquals(3, rows.size());

    assertTrue(
        p.query()
            .select(s("firstName"), s("lastName"), s("uncle", s("firstName"), s("lastName")))
            .where(f("uncle", MATCH_ANY, row("firstName", "Mickey", "lastName", "Mouse")))
            .retrieveJSON()
            .contains("Person\": null"));

    assertTrue(
        p.query()
            .select(s("firstName"), s("lastName"), s("uncle", s("firstName"), s("lastName")))
            .where(
                f(
                    "uncle",
                    MATCH_ANY,
                    row("firstName", "Kwik", "lastName", "Duck"),
                    row("firstName", "Donald", "lastName", "Duck")))
            .retrieveJSON()
            .contains("{\"uncle\": {\"lastName\": \"Duck\", \"firstName\": \"Kwik\"}"));

    // refback
    schema
        .getTable("Person")
        .getMetadata()
        .add(column("nephew").setType(REFBACK).setRefTable("Person").setRefBack("uncle"));

    // test order by for refback
    p.query()
        .select(
            s("firstName"),
            s("lastName"),
            s("nephew", s("firstName"), s("lastName")),
            s("uncle", s("firstName"), s("lastName")))
        .orderBy("nephew")
        .retrieveJSON();

    // filter by refback
    result =
        p.query()
            .select(
                s("firstName"),
                s("lastName"),
                s("nephew", s("firstName"), s("lastName")),
                s("uncle", s("firstName"), s("lastName")))
            .where(f("nephew", MATCH_ANY, row("firstName", "Kwik", "lastName", "Duck")))
            .retrieveJSON();
    assertTrue(result.contains("Donald"));

    result =
        p.query()
            .select(
                s("firstName"),
                s("lastName"),
                s("nephew", s("firstName"), s("lastName")),
                s("uncle", s("firstName"), s("lastName")))
            .where(
                f(
                    "nephew",
                    MATCH_ALL,
                    row("firstName", "Kwik", "lastName", "Duck"),
                    row("firstName", "Kwek", "lastName", "Duck")))
            .retrieveJSON();
    assertTrue(result.contains("Donald"));

    // empty
    result =
        p.query()
            .select(
                s("firstName"),
                s("lastName"),
                s("nephew", s("firstName"), s("lastName")),
                s("uncle", s("firstName"), s("lastName")))
            .where(f("nephew", MATCH_ANY, row("firstName", "Donald", "lastName", "Duck")))
            .retrieveJSON();
    assertFalse(result.contains("Kwik"));

    // test group by ref
    // Kwik = Katrien
    // Kwek, Kwak = Donald
    // Mickey = Kwik
    ObjectMapper mapper = new ObjectMapper();
    Map<String, List<Map<String, Object>>> map =
        mapper.readValue(
            p.groupBy()
                .select(s("count"), s("uncle", s("firstName"), s("lastName")))
                .retrieveJSON(),
            Map.class);
    assertEquals(3, map.get("Person_groupBy").get(0).get("count"));
    assertEquals(
        "Donald",
        ((Map<String, String>) map.get("Person_groupBy").get(0).get("uncle")).get("firstName"));
    assertEquals(1, map.get("Person_groupBy").get(1).get("count"));
    assertEquals(
        "Kwik",
        ((Map<String, String>) map.get("Person_groupBy").get(1).get("uncle")).get("firstName"));

    // test group by refback
    map =
        mapper.readValue(
            p.groupBy()
                .select(s("count"), s("nephew", s("firstName"), s("lastName")))
                .retrieveJSON(),
            Map.class);
    assertEquals(3, map.get("Person_groupBy").get(4).get("count"));
    assertEquals(1, map.get("Person_groupBy").get(0).get("count"));
    assertEquals(
        "Kwak",
        ((Map<String, String>) map.get("Person_groupBy").get(0).get("nephew")).get("firstName"));
  }

  @Test
  public void testCompositeRefArray() throws JsonProcessingException {
    Schema schema =
        database.dropCreateSchema(TestCompositeForeignKeys.class.getSimpleName() + "RefArray");

    schema.create(
        table(
            "Person",
            column("firstName").setPkey(),
            column("lastName").setPkey(),
            column("cousins", REF_ARRAY).setRefTable("Person")));

    Table p = schema.getTable("Person");

    p.insert(new Row().setString("firstName", "Kwik").setString("lastName", "Duck"));

    p.insert(
        new Row()
            .setString("firstName", "Donald")
            .setString("lastName", "Duck")
            .setString("cousins.firstName", "Kwik")
            .setString("cousins.lastName", "Duck"));

    try {
      p.insert(
          new Row()
              .setString("firstName", "Donald")
              .setString("lastName", "Duck2")
              .setString("cousins.firstName", "Kwik"));
      fail("should fail when composite foreign key has a null value, regression #3876");
    } catch (Exception e) {
      System.out.println("errored correctly: " + e);
    }

    try {
      p.delete(new Row().setString("firstName", "Kwik").setString("lastName", "Duck"));
      fail("should have failed on foreign key error");
    } catch (Exception e) {
      System.out.println("errored correctly: " + e);
    }

    schema.create(table("Student").setInheritName("Person"));
    Table s = schema.getTable("Student");
    s.insert(
        new Row()
            .setString("firstName", "Mickey")
            .setString("lastName", "Mouse")
            .setString("cousins.firstName", "Kwik")
            .setString("cousins.lastName", "Duck"));

    String result =
        schema
            .query("Student")
            .select(s("firstName"), s("lastName"), s("cousins", s("firstName"), s("lastName")))
            .retrieveJSON();

    System.out.println(result);

    // refback
    schema
        .getTable("Person")
        .getMetadata()
        .add(column("uncles").setType(REFBACK).setRefTable("Person").setRefBack("cousins"));

    assertTrue(
        List.of(
                p.query()
                    .select(
                        s("firstName"),
                        s("lastName"),
                        s("cousins", s("firstName"), s("lastName")),
                        s("uncles", s("firstName"), s("lastName")))
                    .where(f("firstName", EQUALS, "Donald"))
                    .retrieveRows()
                    .get(0) //
                    .getStringArray("cousins.firstName")) // TODO should be array?
            .contains("Kwik"));

    assertTrue(
        List.of(
                p.query()
                    .where(f("firstName", EQUALS, "Donald"))
                    .retrieveRows()
                    .get(0) //
                    .getStringArray("cousins.firstName")) // TODO should be array?
            .contains("Kwik"));

    assertTrue(
        p.query()
            .select(
                s("firstName"),
                s("lastName"),
                s("cousins", s("firstName"), s("lastName")),
                s("uncles", s("firstName"), s("lastName")))
            .where(
                f(
                    "cousins",
                    MATCH_ANY,
                    row("firstName", "Kwik", "lastName", "Duck"),
                    row("firstName", "Mickey", "Mouse", "Duck")))
            .retrieveJSON()
            .contains("Kwik"));

    assertTrue(
        p.query()
            .select(
                s("firstName"),
                s("lastName"),
                s("cousins", s("firstName"), s("lastName")),
                s("uncles", s("firstName"), s("lastName")))
            .where(f("cousins", MATCH_ANY, row("firstName", "Mickey", "Mouse", "Duck")))
            .retrieveJSON()
            .contains("Person\": null"));

    assertFalse(
        p.query()
            .select(
                s("firstName"),
                s("lastName"),
                s("cousins", s("firstName"), s("lastName")),
                s("uncles", s("firstName"), s("lastName")))
            .where(
                f(
                    "cousins",
                    MATCH_ALL,
                    row("firstName", "Kwik", "lastName", "Duck"),
                    row("firstName", "Mickey", "lastName", "Mouse")))
            .retrieveJSON()
            .contains("Kwik"));

    assertTrue(
        p.query()
            .select(
                s("firstName"),
                s("lastName"),
                s("cousins", s("firstName"), s("lastName")),
                s("uncles", s("firstName"), s("lastName")))
            .where(f("cousins", MATCH_ALL, row("firstName", "Kwik", "lastName", "Duck")))
            .retrieveJSON()
            .contains("Kwik"));

    // check we can sort on ref_array
    p.query()
        .select(
            s("firstName"),
            s("lastName"),
            s("cousins", s("firstName"), s("lastName")),
            s("uncles", s("firstName"), s("lastName")))
        .orderBy("cousins")
        .retrieveJSON();

    // check we can sort on refback to a ref_array
    p.query()
        .select(
            s("firstName"),
            s("lastName"),
            s("cousins", s("firstName"), s("lastName")),
            s("uncles", s("firstName"), s("lastName")))
        .orderBy("uncles")
        .retrieveJSON();

    // test group by ref_array
    // kwik = Micky, Donald
    // Kwok = Donald
    // so mickey has 1 newphew, and donald 2
    ObjectMapper mapper = new ObjectMapper();
    Map<String, List<Map<String, Object>>> map =
        mapper.readValue(
            p.groupBy()
                .select(s("count"), s("uncles", s("firstName"), s("lastName")))
                .retrieveJSON(),
            Map.class);
    assertEquals(1, map.get("Person_groupBy").get(0).get("count"));
    assertEquals(
        "Donald",
        ((Map<String, String>) map.get("Person_groupBy").get(0).get("uncles")).get("firstName"));
    assertEquals(1, map.get("Person_groupBy").get(1).get("count"));
    assertEquals(
        "Mickey",
        ((Map<String, String>) map.get("Person_groupBy").get(1).get("uncles")).get("firstName"));

    // and kwik = 2 uncles, and kwok = 1 uncles
    map =
        mapper.readValue(
            p.groupBy()
                .select(s("count"), s("cousins", s("firstName"), s("lastName")))
                .retrieveJSON(),
            Map.class);
    assertEquals(2, map.get("Person_groupBy").get(0).get("count"));
    assertEquals(
        "Kwik",
        ((Map<String, String>) map.get("Person_groupBy").get(0).get("cousins")).get("firstName"));
    assertEquals(1, map.get("Person_groupBy").get(1).get("count"));
  }

  @Test
  public void testGroupByWithCompositeRefKey() throws JsonProcessingException {
    Schema schema =
        database.dropCreateSchema(
            TestCompositeForeignKeys.class.getSimpleName() + "GroupByCompositeRef");

    schema.create(
        table("Resource", column("name").setPkey()),
        table("Tag", column("name").setPkey()),
        table(
            "Event",
            column("resource", REF).setRefTable("Resource").setPkey(),
            column("name").setPkey(),
            column("tags", REF_ARRAY).setRefTable("Tag")));

    schema.getTable("Resource").insert(row("name", "hall"), row("name", "field"));

    schema
        .getTable("Tag")
        .insert(row("name", "fun"), row("name", "outdoor"), row("name", "indoor"));

    Table events = schema.getTable("Event");
    events.insert(
        new Row()
            .setString("resource", "hall")
            .setString("name", "game1")
            .setStringArray("tags", "fun", "outdoor"));
    events.insert(
        new Row()
            .setString("resource", "hall")
            .setString("name", "game2")
            .setStringArray("tags", "indoor"));
    events.insert(
        new Row()
            .setString("resource", "field")
            .setString("name", "concert1")
            .setStringArray("tags", "fun"));

    ObjectMapper mapper = new ObjectMapper();
    String result = schema.groupBy("Event").select(s("count"), s("tags", s("name"))).retrieveJSON();

    Map<String, List<Map<String, Object>>> map = mapper.readValue(result, Map.class);
    List<Map<String, Object>> rows = map.get("Event_groupBy");

    assertEquals(3, rows.size());
    assertEquals(2, rows.get(0).get("count"));
    assertEquals("fun", ((Map<String, String>) rows.get(0).get("tags")).get("name"));
    assertEquals(1, rows.get(1).get("count"));
    assertEquals("indoor", ((Map<String, String>) rows.get(1).get("tags")).get("name"));
    assertEquals(1, rows.get(2).get("count"));
    assertEquals("outdoor", ((Map<String, String>) rows.get(2).get("tags")).get("name"));

    schema.create(
        table(
            "Agent",
            column("resource", REF).setRefTable("Resource").setPkey(),
            column("agentId").setPkey()));

    schema
        .getTable("Agent")
        .insert(
            new Row().setString("resource", "hall").setString("agentId", "alice"),
            new Row().setString("resource", "hall").setString("agentId", "bob"),
            new Row().setString("resource", "field").setString("agentId", "carol"));

    schema.create(
        table(
            "Event2",
            column("resource", REF).setRefTable("Resource").setPkey(),
            column("name").setPkey(),
            column("creators", REF_ARRAY).setRefTable("Agent")));

    Table events2 = schema.getTable("Event2");
    events2.insert(
        new Row()
            .setString("resource", "hall")
            .setString("name", "party")
            .setStringArray("creators.resource", "hall", "hall")
            .setStringArray("creators.agentId", "alice", "bob"));
    events2.insert(
        new Row()
            .setString("resource", "field")
            .setString("name", "race")
            .setStringArray("creators.resource", "field", "hall")
            .setStringArray("creators.agentId", "carol", "alice"));

    String result2 =
        schema
            .groupBy("Event2")
            .select(s("count"), s("creators", s("resource", s("name")), s("agentId")))
            .retrieveJSON();

    Map<String, List<Map<String, Object>>> map2 = mapper.readValue(result2, Map.class);
    List<Map<String, Object>> rows2 = map2.get("Event2_groupBy");

    assertEquals(3, rows2.size());
    assertEquals(2, rows2.get(0).get("count"));
    assertEquals("alice", ((Map<String, String>) rows2.get(0).get("creators")).get("agentId"));
    assertEquals(1, rows2.get(1).get("count"));
    assertEquals("bob", ((Map<String, String>) rows2.get(1).get("creators")).get("agentId"));
    assertEquals(1, rows2.get(2).get("count"));
    assertEquals("carol", ((Map<String, String>) rows2.get(2).get("creators")).get("agentId"));

    schema.create(
        table(
            "Organisation",
            column("resource", REF).setRefTable("Resource").setPkey(),
            column("orgId").setPkey()));

    schema
        .getTable("Organisation")
        .insert(
            new Row().setString("resource", "hall").setString("orgId", "orgA"),
            new Row().setString("resource", "hall").setString("orgId", "orgB"),
            new Row().setString("resource", "field").setString("orgId", "orgC"));

    schema.create(
        table(
            "Event3",
            column("resource", REF).setRefTable("Resource").setPkey(),
            column("name").setPkey(),
            column("orgs", REF_ARRAY).setRefTable("Organisation")));

    Table events3 = schema.getTable("Event3");
    events3.insert(
        new Row()
            .setString("resource", "hall")
            .setString("name", "meeting")
            .setStringArray("orgs.resource", "hall", "hall")
            .setStringArray("orgs.orgId", "orgA", "orgB"));
    events3.insert(
        new Row()
            .setString("resource", "hall")
            .setString("name", "party")
            .setStringArray("orgs.resource", "hall")
            .setStringArray("orgs.orgId", "orgA"));
    events3.insert(
        new Row()
            .setString("resource", "field")
            .setString("name", "outdoor")
            .setStringArray("orgs.resource", "field")
            .setStringArray("orgs.orgId", "orgC"));

    String result3 =
        schema
            .groupBy("Event3")
            .select(s("count"), s("orgs", s("resource", s("name")), s("orgId")))
            .retrieveJSON();

    Map<String, List<Map<String, Object>>> map3 = mapper.readValue(result3, Map.class);
    List<Map<String, Object>> rows3 = map3.get("Event3_groupBy");

    assertEquals(3, rows3.size());
    assertEquals(2, rows3.get(0).get("count"));
    assertEquals("orgA", ((Map<String, String>) rows3.get(0).get("orgs")).get("orgId"));
    assertEquals(1, rows3.get(1).get("count"));
    assertEquals("orgB", ((Map<String, String>) rows3.get(1).get("orgs")).get("orgId"));
    assertEquals(1, rows3.get(2).get("count"));
    assertEquals("orgC", ((Map<String, String>) rows3.get(2).get("orgs")).get("orgId"));
  }
}
