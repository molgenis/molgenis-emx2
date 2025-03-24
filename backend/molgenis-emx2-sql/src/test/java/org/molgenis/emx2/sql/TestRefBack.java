package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.*;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestRefBack {

  private static Schema schema;

  @BeforeAll
  public static void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestRefBack.class.getSimpleName());
  }

  @Test
  public void testRefArrayBack() {
    execute(REF_ARRAY);
  }

  public void execute(ColumnType refArrayOrMref) {

    // Table Parts(partname)
    Table parts = schema.create(table("Parts").add(column("partname").setPkey()));

    // Table Products(productname, parts->ref(Parts))
    Table products =
        schema.create(
            table("Products")
                .add(column("productname").setPkey())
                .add(column("parts").setType(refArrayOrMref).setRefTable("Parts")));

    parts.insert(new Row().set("partname", "smallscreen"));
    parts.insert(new Row().set("partname", "bigscreen"));
    parts.insert(new Row().set("partname", "smallbutton"));
    parts.insert(new Row().set("partname", "battery"));

    // ref_array entry via 'products', business as usual
    products.insert(new Row().set("productname", "bigphone"));
    products.insert(
        new Row()
            .set("productname", "smallphone")
            .set("parts", new String[] {"smallscreen", "smallbutton"}));

    // add refback, Table Parts(partname,products->refback(product))
    parts
        .getMetadata()
        .add(column("products").setType(REFBACK).setRefTable("Products").setRefBack("parts"));

    // if refback is set to null all are remove
    parts.save(new Row().set("partname", "bigscreen").set("products", null));

    // so now bigphone.parts = [] or null
    assertNull(
        products
            .query()
            .where(f("productname", EQUALS, "bigphone"))
            .retrieveRows()
            .get(0)
            .getStringArray("parts"),
        "bigscreen");

    // contains_any
    String result =
        products
            .select(s("productname"), s("parts", s("partname")))
            .where(f("parts", MATCH_ANY, "smallscreen"))
            .retrieveJSON();
    assertTrue(result.contains("smallphone"));
    assertFalse(result.contains("bigphone"));

    result =
        parts
            .select(s("partname"), s("products", s("productname")))
            .where(f("products", MATCH_ANY, "smallphone"))
            .retrieveJSON();
    assertTrue(result.contains("smallscreen"));
    assertTrue(result.contains("smallbutton"));
    assertFalse(result.contains("bigscreen"));

    result =
        parts
            .select(s("partname"), s("products", s("productname")))
            .where(f("products", MATCH_ANY, "bigphone"))
            .retrieveJSON();
    assertTrue(result.contains("null"));

    // now multiple
    parts.update(
        new Row().set("partname", "bigscreen").set("products", "bigphone"),
        new Row()
            .set("partname", "battery")
            .set("products", new String[] {"smallphone", "bigphone"}));

    // via insert, bigphone and smallphone products should now have headphones as part, i.e.
    // bigphone=bigscreen,battery,headphone,
    // smallphone=battery+headphone+smallscreen+smallbutton
    parts.save(new Row().set("partname", "headphones").set("products", "bigphone,smallphone"));

    List<Row> pTest = products.query().orderBy("productname").retrieveRows();

    assertEquals(2, pTest.size());
    assertEquals("bigphone", pTest.get(0).getString("productname"));
    assertEquals("smallphone", pTest.get(1).getString("productname"));
    assertEquals(2, pTest.get(1).getStringArray("parts").length);

    Query query =
        products.select(s("productname"), s("parts", s("partname")), s("parts_agg", s("count")));
    System.out.println(query.retrieveJSON());
    query =
        parts.select(s("partname"), s("products", s("productname")), s("products_agg", s("count")));

    result =
        parts
            .select(s("partname"), s("products", s("productname")))
            .where(f("products", MATCH_ALL, "smallphone"))
            .retrieveJSON();
    assertTrue(result.contains("smallscreen"));
    assertTrue(result.contains("smallbutton"));
    assertFalse(result.contains("battery"));

    System.out.println(query.retrieveJSON());

    query = parts.agg(s("count"));
    assertTrue(query.retrieveJSON().contains("\"count\": 5"));

    query = products.agg(s("count"));
    assertTrue(query.retrieveJSON().contains("\"count\": 2"));

    query = products.select(s("parts_agg", s("count"))).where(f("productname", EQUALS, "bigphone"));

    query = products.select(s("parts", s("partname")));
    System.out.println(query.retrieveJSON());

    // delete
    parts.delete(new Row().set("partname", "headphones"));
    assertEquals(
        2,
        products
            .query()
            .orderBy("productname")
            .retrieveRows()
            .get(1)
            .getStringArray("parts")
            .length);

    // delete
    products.delete(new Row().set("productname", "bigphone"));

    // check
    assertEquals(1, products.query().orderBy("productname").retrieveRows().size());
  }

  @Test
  public void testRefBack() {

    Table users = schema.create(table("User").add(column("username").setPkey()));

    Table posts =
        schema.create(
            table("Posts")
                .add(column("title").setPkey())
                .add(column("tags").setType(ONTOLOGY).setRefTable("Tags"))
                .add(column("user").setType(REF).setRefTable(users.getName())));

    users
        .getMetadata()
        .add(column("posts").setType(REFBACK).setRefTable(posts.getName()).setRefBack("user"));

    users.insert(new Row().set("username", "jack"));
    users.insert(new Row().set("username", "joe"));
    schema.getTable("Tags").insert(new Row().set("name", "green"));
    posts.insert(new Row().set("title", "joes post").set("user", "joe").set("tags", "green"));

    // add another post for jack, should result in 'posts(user=jack,title=jacks post)
    posts.insert(new Row().set("title", "jacks post").set("user", "jack"));

    // check select on posts
    assertEquals(
        1, // expect 'jacks post'
        users
            .query()
            .select(s("username"), s("posts"))
            .where(f("username", EQUALS, "jack"))
            .retrieveRows()
            .get(0)
            .getStringArray("posts")
            .length);

    // check filter on posts
    assertEquals(
        1, users.query().where(f("posts", f("title", EQUALS, "jacks post"))).retrieveRows().size());

    String result =
        users
            .select(s("username"), s("posts", s("title")))
            .where(f("posts", MATCH_ANY, "jacks post"))
            .retrieveJSON();
    assertTrue(result.contains("jacks"));

    result =
        users
            .select(s("username"), s("posts", s("title")))
            .where(f("posts", MATCH_ALL, "jacks post"))
            .retrieveJSON();
    assertTrue(result.contains("jacks"));

    // check graph query
    Query query = users.agg(s("count"));
    assertTrue(query.retrieveJSON().contains("\"count\": 2"));

    query =
        users
            .select(s("username"), s("posts", s("title")))
            .where(f("posts", f("title", EQUALS, "jacks post")));
    assertTrue(query.retrieveJSON().contains("jacks post"));

    query =
        users.select(
            s("username"), s("posts", s("title")).where(f("tags", f("name", EQUALS, "green"))));
    assertTrue(query.retrieveJSON().contains("joes post"));
    assertFalse(query.retrieveJSON().contains("jacks post"));

    query = users.agg(s("count")).where(f("posts", f("title", EQUALS, "jacks post")));
    assertTrue(query.retrieveJSON().contains("\"count\": 1"));

    // delete of user should fail as long as there are posts refering to this user, unless cascading
    // delete
    try {
      // users.delete(new Row().set("username", "jack"));
      // should not fail? fail("delete of user with reference from post should fail");
    } catch (Exception e) {
      // ok
    }

    // delete of a post should also remove it from users.posts
    posts.delete(new Row().set("title", "joes post"));

    // check, should now have one posts in posts array
    assertEquals(
        1,
        users
            .query()
            .select(s("username"), s("posts"))
            .where(f("username", EQUALS, "jack"))
            .retrieveRows()
            .get(0)
            .getStringArray("posts")
            .length);
  }

  @Test
  public void testRefBackUsingSubclasses() {
    schema.create(
        table("subject", column("id").setPkey()),
        table(
            "treatments",
            column("id").setPkey(),
            column("partOfSubject").setType(REF).setRefTable("subject")));
    // inherit
    schema.create(table("treatmentxyz", column("xyz")).setInheritName("treatments"));
    // add the refback
    schema
        .getTable("subject")
        .getMetadata()
        .add(
            column("treatxyz")
                .setType(REFBACK)
                .setRefTable("treatmentxyz")
                .setRefBack("partOfSubject"));
    schema.getTable("subject").insert(row("id", "s1"));
    schema.getTable("treatmentxyz").insert(row("id", "t1", "partOfSubject", "s1"));
    List<Row> subjects = schema.query("subject").retrieveRows();
    assertEquals("t1", subjects.get(0).getString("treatxyz"));
  }

  @Test
  void testRefBackAndFile_fix4703() {
    Table test =
        schema.create(
            table(
                "test4703",
                column("id").setPkey(),
                column("parent").setType(REF).setRefTable("test4703"),
                column("uncles").setType(REF_ARRAY).setRefTable("test4703"),
                column("children").setType(REFBACK).setRefTable("test4703").setRefBack("parent"),
                column("cousins").setType(REFBACK).setRefTable("test4703").setRefBack("uncles"),
                column("photo").setType(FILE)));

    test.insert(
        row("id", "1"),
        row("id", "2", "parent", "1"),
        row("id", "3", "parent", "1"),
        row("id", "4", "parent", "1"),
        row("id", "5", "parent", "2", "uncles", "3,4"),
        row("id", "6", "parent", "2", "uncles", "3,4"));

    List<Row> result =
        test.query()
            .select(
                s("id"),
                s("parent", s("id")),
                s("children", s("id")),
                s("uncles", s("id")),
                s("cousins", s("id")),
                s("photo", s("id"), s("filename"), s("mimetype")))
            .retrieveRows();
    assertEquals(6, result.size());
  }
}
