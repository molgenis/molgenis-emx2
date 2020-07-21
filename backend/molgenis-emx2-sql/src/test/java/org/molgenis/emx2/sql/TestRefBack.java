package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

public class TestRefBack {

  private static Schema schema;

  @BeforeClass
  public static void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestRefBack.class.getSimpleName());
  }

  @Test
  public void restRefArrayBack() {

    Table parts = schema.create(table("Parts").add(column("partname").pkey()));

    Table products =
        schema.create(
            table("Products")
                .add(column("productname").pkey())
                .add(column("parts").type(REF_ARRAY).refTable("Parts").nullable(true)));

    parts
        .getMetadata()
        .add(column("products").type(REFBACK).refTable("Products").mappedBy("parts"));

    parts.insert(new Row().set("partname", "smallscreen"));
    parts.insert(new Row().set("partname", "bigscreen"));
    parts.insert(new Row().set("partname", "smallbutton"));
    parts.insert(new Row().set("partname", "battery"));

    // ref_array entry via 'products', business as usual
    products.insert(
        new Row()
            .set("productname", "smallphone")
            .set("parts", new String[] {"smallscreen", "smallbutton"}));

    // refback entry update, i.e. via 'products'
    products.insert(new Row().set("productname", "bigphone"));

    // update,   bigphone should have bigsreen+battery, smallphone should have battery added
    // part, i.e.
    parts.update(
        new Row().set("partname", "bigscreen").set("products", "bigphone"),
        new Row()
            .set("partname", "battery")
            .set("products", new String[] {"smallphone", "bigphone"}));

    // via insert, bigphone and smallphone products should now have headphones as part, i.e.
    // bigphone=bigscreen,battery,headphone,
    // smallphone=battery+headphone+smallscreen+smallbutton
    parts.update(new Row().set("partname", "headphones").set("products", "bigphone,smallphone"));

    List<Row> pTest = products.getRows();
    assertEquals(2, pTest.size());
    assertEquals("smallphone", pTest.get(0).getString("productname"));
    assertEquals(4, pTest.get(0).getStringArray("parts").length);
    assertEquals("bigphone", pTest.get(1).getString("productname"));
    assertEquals(3, pTest.get(1).getStringArray("parts").length);
    assertEquals(4, products.getRows().get(0).getStringArray("parts").length);

    Query query =
        products.select(
            s("data", s("productname"), s("parts", s("partname")), s("parts_agg", s("count"))));
    System.out.println(query.retrieveJSON());
    query =
        parts.select(
            s(
                "data",
                s("partname"),
                s("products", s("productname")),
                s("products_agg", s("count"))));
    System.out.println(query.retrieveJSON());

    query = parts.select(s("data_agg", s("count")));
    assertTrue(query.retrieveJSON().contains("\"count\":5"));

    query = products.select(s("data_agg", s("count")));
    assertTrue(query.retrieveJSON().contains("\"count\":2"));

    query =
        products
            .select(s("data", s("parts_agg", s("count"))))
            .filter(f("productname").add(EQUALS, "bigphone"));

    assertTrue(query.retrieveJSON().contains("\"count\":3"));

    query =
        parts
            .select(s("data", s("products_agg", s("count"))))
            .filter(f("partname").add(EQUALS, "battery"));

    assertTrue(query.retrieveJSON().contains("\"count\":2"));

    query = products.select(s("data", s("parts", s("name"))));
    System.out.println(query.retrieveJSON());

    // delete
    parts.delete(new Row().set("partname", "headphones"));
    assertEquals(3, products.getRows().get(0).getStringArray("parts").length);

    // delete
    products.delete(new Row().set("productname", "bigphone"));

    // check
    assertEquals(1, products.getRows().size());
  }

  @Test
  public void testRefBack() {

    Table users = schema.create(table("User").add(column("username").pkey()));

    Table posts =
        schema.create(
            table("Posts")
                .add(column("title").pkey())
                .add(column("user").type(REF).refTable(users.getName()).nullable(true)));

    users
        .getMetadata()
        .add(column("posts").type(REFBACK).refTable(posts.getName()).mappedBy("user"));

    users.insert(new Row().set("username", "jack"));
    users.insert(new Row().set("username", "joe"));

    posts.insert(new Row().set("title", "joes post").set("user", "joe"));

    // now the magic
    users.update(new Row().set("username", "jack").set("posts", "joes post"));

    // check via query we have now post for jack
    assertEquals(1, posts.query().filter("user", EQUALS, "jack").getRows().size());

    // add another post for jack, now the 'posts' should be updated also
    posts.insert(new Row().set("title", "jacks post").set("user", "jack"));

    // check select on posts
    assertEquals(
        2,
        users
            .query()
            .filter("username", EQUALS, "jack")
            .getRows()
            .get(0)
            .getStringArray("posts")
            .length);

    // check filter on posts
    assertEquals(
        1, users.query().filter("posts", f("title", EQUALS, "jacks post")).getRows().size());

    // check graph query
    Query query = users.select(s("data_agg", s("count")));
    assertTrue(query.retrieveJSON().contains("\"count\":2"));

    query =
        users
            .select(s("data", s("username"), s("posts", s("title"))))
            .filter(f("posts", f("title", EQUALS, "jacks post")));
    assertTrue(query.retrieveJSON().contains("jacks post"));

    query =
        users
            .select(s("data_agg", s("count")))
            .filter(f("posts", f("title", EQUALS, "jacks post")));
    assertTrue(query.retrieveJSON().contains("\"count\":1"));

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
            .filter("username", EQUALS, "jack")
            .getRows()
            .get(0)
            .getStringArray("posts")
            .length);
  }
}
