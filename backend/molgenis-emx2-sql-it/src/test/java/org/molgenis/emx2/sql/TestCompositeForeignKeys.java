package org.molgenis.emx2.sql;

import static graphql.Assert.assertTrue;
import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

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
        TableMetadata.table(
            "Person",
            Column.column("firstName").setPkey(),
            Column.column("lastName").setPkey(),
            Column.column("uncle", ColumnType.REF).setRefTable("Person")));

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

    schema.create(TableMetadata.table("Student").setInherit("Person"));
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
                SelectColumn.s("firstName"),
                SelectColumn.s("lastName"),
                SelectColumn.s("uncle", SelectColumn.s("firstName"), SelectColumn.s("lastName")),
                SelectColumn.s("uncle", SelectColumn.s("firstName"), SelectColumn.s("lastName")))
            .retrieveJSON();
    System.out.println(result);

    result =
        schema
            .query("Student")
            .select(SelectColumn.s("firstName"), SelectColumn.s("lastName"))
            .where(
                FilterBean.or(
                    FilterBean.and(
                        FilterBean.f("firstName", Operator.EQUALS, "Donald"),
                        FilterBean.f("lastName", Operator.EQUALS, "Duck")),
                    FilterBean.and(
                        FilterBean.f("firstName", Operator.EQUALS, "Mickey"),
                        FilterBean.f("lastName", Operator.EQUALS, "Mouse"))))
            .retrieveJSON();

    System.out.println(result);
    assertTrue(result.contains("Mouse"));
    assertFalse(result.contains("Duck"));

    result =
        schema
            .query("Person")
            .select(SelectColumn.s("firstName"), SelectColumn.s("lastName"))
            .where(
                FilterBean.or(
                    FilterBean.and(
                        FilterBean.f("firstName", Operator.EQUALS, "Donald"),
                        FilterBean.f("lastName", Operator.EQUALS, "Duck")),
                    FilterBean.and(
                        FilterBean.f("firstName", Operator.EQUALS, "Mickey"),
                        FilterBean.f("lastName", Operator.EQUALS, "Mouse"))))
            .retrieveJSON();

    System.out.println(result);
    assertTrue(result.contains("Mouse"));
    assertTrue(result.contains("Duck"));

    // composite key filter
    result =
        schema
            .query("Person")
            .select(SelectColumn.s("firstName"), SelectColumn.s("lastName"))
            // composite filter, should result in 'donald duck' OR 'mickey mouse'
            .where(
                FilterBean.f(
                    "uncle",
                    FilterBean.or(
                        FilterBean.and(
                            FilterBean.f("firstName", Operator.EQUALS, "Donald"),
                            FilterBean.f("lastName", Operator.EQUALS, "Duck")),
                        FilterBean.and(
                            FilterBean.f("firstName", Operator.EQUALS, "Mickey"),
                            FilterBean.f("lastName", Operator.EQUALS, "Mouse")))))
            .retrieveJSON();

    System.out.println(result);
    assertTrue(result.contains("Kwik"));
    assertFalse(result.contains("Mouse"));

    // refback
    schema
        .getTable("Person")
        .getMetadata()
        .add(
            Column.column("nephew")
                .setType(ColumnType.REFBACK)
                .setRefTable("Person")
                .setRefBack("uncle"));

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
                        SelectColumn.s("firstName"),
                        SelectColumn.s("lastName"),
                        SelectColumn.s(
                            "nephew", SelectColumn.s("firstName"), SelectColumn.s("lastName")),
                        SelectColumn.s(
                            "uncle", SelectColumn.s("firstName"), SelectColumn.s("lastName")))
                    .where(FilterBean.f("firstName", Operator.EQUALS, "Katrien"))
                    .retrieveRows()
                    .get(0)
                    .getStringArray("nephew-firstName"))
            .contains("Kwik"));
    assertTrue(
        List.of(
                p.query()
                    .select(
                        SelectColumn.s("firstName"),
                        SelectColumn.s("lastName"),
                        SelectColumn.s(
                            "nephew", SelectColumn.s("firstName"), SelectColumn.s("lastName")),
                        SelectColumn.s(
                            "uncle", SelectColumn.s("firstName"), SelectColumn.s("lastName")))
                    .where(FilterBean.f("firstName", Operator.EQUALS, "Kwik"))
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
        TableMetadata.table(
            "Person",
            Column.column("firstName").setPkey(),
            Column.column("lastName").setPkey(),
            Column.column("cousins", ColumnType.REF_ARRAY).setRefTable("Person")));

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

    schema.create(TableMetadata.table("Student").setInherit("Person"));
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
            .select(
                SelectColumn.s("firstName"),
                SelectColumn.s("lastName"),
                SelectColumn.s("cousins", SelectColumn.s("firstName"), SelectColumn.s("lastName")))
            .retrieveJSON();

    System.out.println(result);

    // refback
    schema
        .getTable("Person")
        .getMetadata()
        .add(
            Column.column("uncles")
                .setType(ColumnType.REFBACK)
                .setRefTable("Person")
                .setRefBack("cousins"));

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
                        SelectColumn.s("firstName"),
                        SelectColumn.s("lastName"),
                        SelectColumn.s(
                            "uncles", SelectColumn.s("firstName"), SelectColumn.s("lastName")),
                        SelectColumn.s(
                            "cousins", SelectColumn.s("firstName"), SelectColumn.s("lastName")))
                    .where(FilterBean.f("firstName", Operator.EQUALS, "Kwok"))
                    .retrieveRows()
                    .get(0)
                    .getStringArray("uncles-firstName"))
            .contains("Donald"));
    assertTrue(
        List.of(
                p.query()
                    .select(
                        SelectColumn.s("firstName"),
                        SelectColumn.s("lastName"),
                        SelectColumn.s(
                            "cousins", SelectColumn.s("firstName"), SelectColumn.s("lastName")),
                        SelectColumn.s(
                            "uncles", SelectColumn.s("firstName"), SelectColumn.s("lastName")))
                    .where(FilterBean.f("firstName", Operator.EQUALS, "Donald"))
                    .retrieveRows()
                    .get(1) //
                    .getStringArray("cousins-firstName")) // TODO should be array?
            .contains("Kwok"));
  }

  //  @Test
  //  public void testCompositeRefArrayWithInheritance() {
  //
  //    String schemaName = TestCompositeForeignKeys.class.getSimpleName() +
  // "RefArrayWithInheritance";
  //    Schema schema = database.dropCreateSchema(schemaName);
  //
  //    schema.create(table("AllVariables"));
  //  }
}
