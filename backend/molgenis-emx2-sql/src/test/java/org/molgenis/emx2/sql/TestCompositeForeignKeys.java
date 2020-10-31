package org.molgenis.emx2.sql;

import org.junit.Before;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

import java.util.List;

import static graphql.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.FilterBean.*;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

public class TestCompositeForeignKeys {
  private Database database;

  @Before
  public void setUp() {
    database = TestDatabaseFactory.getTestDatabase();

    // create target table

  }

  @Test
  public void testCompositeRef() {
    Schema schema =
        database.dropCreateSchema(TestCompositeForeignKeys.class.getSimpleName() + "Ref");

    schema.create(
        table(
            "Person",
            column("firstName").setPkey(),
            column("lastName").setPkey(),
            column("uncle_firstName", REF)
                .setRefTable("Person")
                .setRefColumn("firstName")
                .setRefName("uncle")
                .setNullable(true),
            column("uncle_lastName", REF)
                .setRefTable("Person")
                .setRefColumn("lastName")
                .setRefName("uncle")
                .setNullable(true)));

    Table p = schema.getTable("Person");

    p.insert(new Row().setString("firstName", "Donald").setString("lastName", "Duck"));

    try {
      p.insert(
          new Row()
              .setString("firstName", "Kwik")
              .setString("lastName", "Duck")
              .setString("uncle_firstName", "Donald")
              .setString("uncle_lastName", "MISSING"));
      fail("should have failed on missing foreign key");
    } catch (Exception e) {
      System.out.println("errored correctly: " + e);
    }

    p.insert(
        new Row()
            .setString("firstName", "Kwik")
            .setString("lastName", "Duck")
            .setString("uncle_firstName", "Donald")
            .setString("uncle_lastName", "Duck"));

    p.insert(
        new Row()
            .setString("firstName", "Kwek")
            .setString("lastName", "Duck")
            .setString("uncle_firstName", "Donald")
            .setString("uncle_lastName", "Duck"));

    p.insert(
        new Row()
            .setString("firstName", "Kwak")
            .setString("lastName", "Duck")
            .setString("uncle_firstName", "Donald")
            .setString("uncle_lastName", "Duck"));

    try {
      p.delete(new Row().setString("firstName", "Donald").setString("lastName", "Duck"));
      fail("should have failed on foreign key (Donald is used in foreign key)");
    } catch (Exception e) {
      System.out.println("errored correctly: " + e);
    }

    schema.create(table("Student").setInherit("Person"));
    Table s = schema.getTable("Student");
    s.insert(
        new Row()
            .setString("firstName", "Mickey")
            .setString("lastName", "Mouse")
            .setString("uncle_firstName", "Kwik")
            .setString("uncle_lastName", "Duck"));

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
        .add(
            column("nephew_firstName")
                .setType(REFBACK)
                .setRefTable("Person")
                .setRefColumn("firstName")
                .setMappedBy("uncle_firstName")
                .setRefName("nephew"),
            column("nephew_lastName")
                .setType(REFBACK)
                .setRefTable("Person")
                .setRefColumn("lastName")
                .setMappedBy("uncle_lastName")
                .setRefName("nephew"));

    s.insert(
        new Row()
            .setString("firstName", "Katrien")
            .setString("lastName", "Duck")
            .setString("nephew_firstName", "Kwik")
            .setString("nephew_lastName", "Duck")); // I know, not true

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
  }

  @Test
  public void testCompositeRefArray() {
    Schema schema =
        database.dropCreateSchema(TestCompositeForeignKeys.class.getSimpleName() + "RefArray");

    schema.create(
        table(
            "Person",
            column("firstName").setPkey(),
            column("lastName").setPkey(),
            column("cousins_firstName", REF_ARRAY)
                .setRefName("cousins")
                .setRefTable("Person")
                .setRefColumn("firstName")
                .setNullable(true),
            column("cousins_lastName", REF_ARRAY)
                .setRefName("cousins")
                .setRefTable("Person")
                .setRefColumn("lastName")
                .setNullable(true)));

    Table p = schema.getTable("Person");

    p.insert(new Row().setString("firstName", "Kwik").setString("lastName", "Duck"));

    p.insert(
        new Row()
            .setString("firstName", "Donald")
            .setString("lastName", "Duck")
            .setString("cousins_firstName", "Kwik")
            .setString("cousins_lastName", "Duck"));

    try {
      p.delete(new Row().setString("firstName", "Kwik").setString("lastName", "Duck"));
      fail("should have failed on foreign key error");
    } catch (Exception e) {
      System.out.println("errored correctly: " + e);
    }

    schema.create(table("Student").setInherit("Person"));
    Table s = schema.getTable("Student");
    s.insert(
        new Row()
            .setString("firstName", "Mickey")
            .setString("lastName", "Mouse")
            .setString("cousins_firstName", "Kwik")
            .setString("cousins_lastName", "Duck"));

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
        .add(
            column("uncles_firstName")
                .setType(REFBACK)
                .setRefTable("Person")
                .setRefColumn("firstName")
                .setRefName("uncles")
                .setMappedBy("cousins_firstName"),
            column("uncles_lastName")
                .setType(REFBACK)
                .setRefTable("Person")
                .setRefColumn("lastName")
                .setRefName("uncles")
                .setMappedBy("cousins_lastName"));

    s.insert(
        new Row()
            .setString("firstName", "Kwok") // doesn't exist
            .setString("lastName", "Duck")
            .setString("uncles_firstName", "Donald")
            .setString("uncles_lastName", "Duck"));

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
                    .getStringArray("uncles_firstName"))
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
                    .getStringArray("cousins_firstName")) // TODO should be array?
            .contains("Kwok"));
  }

  @Test
  public void testCompositeMref() {
    Schema schema =
        database.dropCreateSchema(TestCompositeForeignKeys.class.getSimpleName() + "Mref");

    schema.create(
        table(
            "Person2",
            column("firstName").setPkey(),
            column("lastName").setPkey(),
            column("father_firstName", REF)
                .setRefTable("Person2")
                .setRefColumn("firstName")
                .setRefName("father"),
            column("father_lastName", REF)
                .setRefTable("Person2")
                .setRefColumn("lastName")
                .setRefName("father"),
            column("mother_firstName", REF)
                .setRefTable("Person2")
                .setRefColumn("firstName")
                .setRefName("mother"),
            column("mother_lastName", REF)
                .setRefTable("Person2")
                .setRefColumn("lastName")
                .setRefName("mother"),
            column("children_firstName", MREF)
                .setRefTable("Person2")
                .setRefName("children")
                .setRefColumn("firstName"),
            column("children_lastName", MREF)
                .setRefTable("Person2")
                .setRefName("children")
                .setRefColumn("lastName"))); // .with("lastName", "lastName")));
  }

  @Test
  public void testCompositeRefWithLinkToOtherColumn() {
    Schema schema =
        database.dropCreateSchema(TestCompositeForeignKeys.class.getSimpleName() + "LinkedRef");

    schema.create(table("Collection", column("name").setPkey()));
    schema.create(
        table(
            "Table",
            column("name").setPkey(),
            column("collection").setRefTable("Collection").setPkey()));
    schema.create(
        table(
            "Variable",
            column("name").setPkey(),
            column("collection")
                .setType(REF)
                .setRefTable("Table")
                .setRefColumn("table")
                .setRefName("table"),
            column("table")
                .setType(REF)
                .setRefTable("Table")
                .setRefColumn("name")
                .setRefName("table")
                .setRefLink(new String[] {"collection"})));

    schema.getTable("Collection").insert(new Row().set("name", "LifeCycle"));
    schema.getTable("Table").insert(new Row().set("name", "Table1").set("collection", "LifeCycle"));
    schema
        .getTable("Variable")
        .insert(
            new Row()
                .set("name", "Variable1")
                .set("collection", "LifeCycle")
                .set("table", "Table1"));

    try {
      schema
          .getTable("Variable")
          .insert(
              new Row()
                  .set("name", "Variable1")
                  .set("collection", "LifeCycle")
                  .set("table", "Table2"));
      fail("should have failed");
    } catch (Exception e) {
      System.out.println("Error correct: " + e.getMessage());
    }

    assertEquals(
        "Table1", schema.getTable("Variable").query().retrieveRows().get(0).getString("table"));
  }
}
