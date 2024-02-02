package org.molgenis.emx2.sql;

import static graphql.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.FilterBean.*;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

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
    result =
        schema
            .query("Person")
            .select(s("firstName"), s("lastName"))
            // composite filter, should result in 'donald duck' OR 'mickey mouse'
            .where(
                f(
                    "uncle",
                    or(
                        and(f("firstName", EQUALS, "Donald"), f("lastName", EQUALS, "Duck")),
                        and(f("firstName", EQUALS, "Mickey"), f("lastName", EQUALS, "Mouse")))))
            .retrieveJSON();

    System.out.println(result);
    assertTrue(result.contains("Kwik"));
    assertFalse(result.contains("Mouse"));

    // refback
    schema
        .getTable("Person")
        .getMetadata()
        .add(column("nephew").setType(REFBACK).setRefTable("Person").setRefBack("uncle"));

    s.insert(
        new Row()
            .setString("firstName", "Katrien")
            .setString("lastName", "Duck")
            .setString("nephew.firstName", "Kwik")
            .setString("nephew.lastName", "Duck")); // I know, not true

    assertTrue(
        List.of(
                s.query()
                    .select(
                        s("firstName"),
                        s("lastName"),
                        s("nephew", s("firstName"), s("lastName")),
                        s("uncle", s("firstName"), s("lastName")))
                    .where(f("firstName", EQUALS, "Katrien"))
                    .retrieveRows()
                    .get(0)
                    .getStringArray("nephew-firstName"))
            .contains("Kwik"));
    assertTrue(
        List.of(
                p.query()
                    .select(
                        s("firstName"),
                        s("lastName"),
                        s("nephew", s("firstName"), s("lastName")),
                        s("uncle", s("firstName"), s("lastName")))
                    .where(f("firstName", EQUALS, "Kwik"))
                    .retrieveRows()
                    .get(0)
                    .getStringArray("uncle-firstName"))
            .contains("Katrien"));

    // test order by for refback
    p.query()
        .select(
            s("firstName"),
            s("lastName"),
            s("nephew", s("firstName"), s("lastName")),
            s("uncle", s("firstName"), s("lastName")))
        .orderBy("nephew")
        .retrieveJSON();

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
    assertEquals(2, map.get("Person_groupBy").get(0).get("count"));
    assertEquals(
        "Donald",
        ((Map<String, String>) map.get("Person_groupBy").get(0).get("uncle")).get("firstName"));
    assertEquals(1, map.get("Person_groupBy").get(1).get("count"));
    assertEquals(
        "Katrien",
        ((Map<String, String>) map.get("Person_groupBy").get(1).get("uncle")).get("firstName"));

    // test group by refback
    map =
        mapper.readValue(
            p.groupBy()
                .select(s("count"), s("nephew", s("firstName"), s("lastName")))
                .retrieveJSON(),
            Map.class);
    assertEquals(3, map.get("Person_groupBy").get(6).get("count"));
    assertEquals(1, map.get("Person_groupBy").get(0).get("count"));
    assertEquals(
        "Donald",
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

    s.insert(
        new Row()
            .setString("firstName", "Kwok") // doesn't exist
            .setString("lastName", "Duck")
            .setString("uncles.firstName", "Donald")
            .setString("uncles.lastName", "Duck"));

    assertTrue(
        List.of(
                s.query()
                    .select(
                        s("firstName"),
                        s("lastName"),
                        s("uncles", s("firstName"), s("lastName")),
                        s("cousins", s("firstName"), s("lastName")))
                    .where(f("firstName", EQUALS, "Kwok"))
                    .retrieveRows()
                    .get(0)
                    .getStringArray("uncles-firstName"))
            .contains("Donald"));
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
                    .get(1) //
                    .getStringArray("cousins-firstName")) // TODO should be array?
            .contains("Kwok"));

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
    assertEquals(2, map.get("Person_groupBy").get(0).get("count"));
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
    assertEquals(
        "Kwok",
        ((Map<String, String>) map.get("Person_groupBy").get(1).get("cousins")).get("firstName"));
  }
}
