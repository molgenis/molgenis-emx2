package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;

import java.util.List;

import static junit.framework.TestCase.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.sql.Filter.f;
import static org.molgenis.emx2.sql.SelectColumn.s;

public class TestRefBack {

  private static Schema schema;

  @BeforeClass
  public static void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.createSchema(TestRefBack.class.getSimpleName());
  }

  @Test
  public void restRefArrayBack() {

    Table parts = schema.create(table("Parts").addColumn(column("partname").pkey(true)));

    Table products =
        schema.create(
            table("Products")
                .addColumn(column("productname").pkey(true))
                .addColumn(column("parts").type(REF_ARRAY).refTable("Parts").nullable(true)));

    parts
        .getMetadata()
        .addColumn(column("products").type(REFBACK).refTable("Products").mappedBy("parts"));

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

    // update
    parts.update(
        new Row().set("partname", "bigscreen").set("products", "bigphone"),
        new Row()
            .set("partname", "battery")
            .set("products", new String[] {"smallphone", "bigphone"}));

    // via insert
    parts.update(new Row().set("partname", "headphones").set("products", "bigphone,smallphone"));

    List<Row> pTest = products.retrieve();
    assertEquals(2, pTest.size());
    assertEquals("smallphone", pTest.get(0).getString("productname"));
    assertEquals(4, products.retrieve().get(0).getStringArray("parts").length);

    SqlGraphQuery query =
        new SqlGraphQuery(products)
            .select(
                s("data", s("productname"), s("parts", s("partname")), s("parts_agg", s("count"))));
    System.out.println(query.retrieve());
    query =
        new SqlGraphQuery(parts)
            .select(
                s(
                    "data",
                    s("partname"),
                    s("products", s("productname")),
                    s("products_agg", s("count"))));
    System.out.println(query.retrieve());

    query = new SqlGraphQuery(parts).select(s("data_agg", s("count")));
    assertTrue(query.retrieve().contains("\"count\":5"));

    query = new SqlGraphQuery(products).select(s("data_agg", s("count")));
    assertTrue(query.retrieve().contains("\"count\":2"));

    query =
        new SqlGraphQuery(products)
            .select(s("data", s("parts_agg", s("count"))))
            .filter(f("productname").add(EQUALS, "bigphone"));

    assertTrue(query.retrieve().contains("\"count\":3"));

    query =
        new SqlGraphQuery(parts)
            .select(s("data", s("products_agg", s("count"))))
            .filter(f("partname").add(EQUALS, "battery"));

    assertTrue(query.retrieve().contains("\"count\":2"));

    query = new SqlGraphQuery(products).select(s("data", s("parts", s("name"))));
    System.out.println(query.retrieve());

    // delete
    parts.delete(new Row().set("partname", "headphones"));
    assertEquals(3, products.retrieve().get(0).getStringArray("parts").length);

    // delete
    products.delete(new Row().set("productname", "bigphone"));

    // check
    assertEquals(1, products.retrieve().size());
  }

  @Test
  public void testRefBack() {

    Table users =
        schema.create(table("User").addColumn(column("username")).setPrimaryKey("username"));

    Table posts =
        schema.create(
            table("Posts")
                .addColumn(column("title"))
                .addColumn(
                    column("user")
                        .type(REF)
                        .refTable(users.getName())
                        .refColumn("username")
                        .nullable(true))
                .setPrimaryKey("title"));

    users
        .getMetadata()
        .addColumn(column("posts").type(REFBACK).refTable(posts.getName()).mappedBy("user"));

    users.insert(new Row().set("username", "jack"));
    users.insert(new Row().set("username", "joe"));

    posts.insert(new Row().set("title", "joes post").set("username", "joe"));

    // now the magic
    users.update(new Row().set("username", "jack").set("posts", "joes post"));

    // check via query we have now post for jack
    assertEquals(1, posts.query().where("user", EQUALS, "jack").retrieve().size());

    // add another post for jack, now the 'posts' should be updated also
    posts.insert(new Row().set("title", "jacks post").set("user", "jack"));

    // check select on posts
    assertEquals(
        2,
        users
            .query()
            .where("username", EQUALS, "jack")
            .retrieve()
            .get(0)
            .getStringArray("posts")
            .length);

    // check filter on posts
    assertEquals(1, users.query().where("posts", EQUALS, "jacks post").retrieve().size());

    // check graph query
    SqlGraphQuery query = new SqlGraphQuery(users).select(s("data_agg", s("count")));
    assertTrue(query.retrieve().contains("\"count\":2"));

    query =
        new SqlGraphQuery(users)
            .select(s("data", s("username"), s("posts", s("title"))))
            .filter(f("posts", f("title", EQUALS, "jacks post")));
    assertTrue(query.retrieve().contains("jacks post"));

    query =
        new SqlGraphQuery(users)
            .select(s("data_agg", s("count")))
            .filter(f("posts", f("title", EQUALS, "jacks post")));
    assertTrue(query.retrieve().contains("\"count\":1"));

    // delete of user should fail as long as there are posts refering to this user, unless cascading
    // delete
    try {
      users.delete(new Row().set("username", "jack"));
      fail("delete of user with reference from post should fail");
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
            .where("username", EQUALS, "jack")
            .retrieve()
            .get(0)
            .getStringArray("posts")
            .length);
  }
}
