package org.molgenis.emx2.sql;

import static junit.framework.TestCase.*;
import static org.molgenis.emx2.ColumnType.*;

import java.util.List;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;

public class TestRefBack {

  private static Schema schema;

  @BeforeClass
  public static void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestRefBack.class.getSimpleName());
  }

  @Test
  public void testRefArrayBack() {
    execute(ColumnType.REF_ARRAY);
  }

  //  @Test
  //  public void testMrefBack() {
  //    execute(MREF);
  //  }

  public void execute(ColumnType refArrayOrMref) {

    // Table Parts(partname)
    Table parts =
        schema.create(TableMetadata.table("Parts").add(Column.column("partname").setPkey()));

    // Table Products(productname, parts->ref(Parts))
    Table products =
        schema.create(
            TableMetadata.table("Products")
                .add(Column.column("productname").setPkey())
                .add(Column.column("parts").setType(refArrayOrMref).setRefTable("Parts")));

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
        .add(
            Column.column("products")
                .setType(ColumnType.REFBACK)
                .setRefTable("Products")
                .setRefBack("parts"));

    // use refback to update indirectly
    parts.save(new Row().set("partname", "bigscreen").set("products", "bigphone"));

    // so now bigphone.parts = [bigscreen]
    TestCase.assertEquals(
        "bigscreen",
        products
            .query()
            .where(FilterBean.f("productname", Operator.EQUALS, "bigphone"))
            .retrieveRows()
            .get(0)
            .getStringArray("parts")[0]);

    // if refback is not updated then nothing happens
    parts.save(new Row().set("partname", "bigscreen"));

    // so now bigphone.parts = [bigscreen]
    TestCase.assertEquals(
        "bigscreen",
        products
            .query()
            .where(FilterBean.f("productname", Operator.EQUALS, "bigphone"))
            .retrieveRows()
            .get(0)
            .getStringArray("parts")[0]);

    // if refback is set to null all are remove
    parts.save(new Row().set("partname", "bigscreen").set("products", null));

    // so now bigphone.parts = [] or null
    TestCase.assertNull(
        "bigscreen",
        products
            .query()
            .where(FilterBean.f("productname", Operator.EQUALS, "bigphone"))
            .retrieveRows()
            .get(0)
            .getStringArray("parts"));

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

    TestCase.assertEquals("bigphone", pTest.get(0).getString("productname"));
    TestCase.assertEquals(3, pTest.get(0).getStringArray("parts").length);

    TestCase.assertEquals("smallphone", pTest.get(1).getString("productname"));
    TestCase.assertEquals(4, pTest.get(1).getStringArray("parts").length);

    Query query =
        products.select(
            SelectColumn.s("productname"),
            SelectColumn.s("parts", SelectColumn.s("partname")),
            SelectColumn.s("parts_agg", SelectColumn.s("count")));
    System.out.println(query.retrieveJSON());
    query =
        parts.select(
            SelectColumn.s("partname"),
            SelectColumn.s("products", SelectColumn.s("productname")),
            SelectColumn.s("products_agg", SelectColumn.s("count")));

    System.out.println(query.retrieveJSON());

    query = parts.agg(SelectColumn.s("count"));
    TestCase.assertTrue(query.retrieveJSON().contains("\"count\":5"));

    query = products.agg(SelectColumn.s("count"));
    TestCase.assertTrue(query.retrieveJSON().contains("\"count\":2"));

    query =
        products
            .select(SelectColumn.s("parts_agg", SelectColumn.s("count")))
            .where(FilterBean.f("productname", Operator.EQUALS, "bigphone"));

    TestCase.assertTrue(query.retrieveJSON().contains("\"count\":3"));

    query =
        parts
            .select(SelectColumn.s("products_agg", SelectColumn.s("count")))
            .where(FilterBean.f("partname", Operator.EQUALS, "battery"));

    TestCase.assertTrue(query.retrieveJSON().contains("\"count\":2"));

    query = products.select(SelectColumn.s("parts", SelectColumn.s("partname")));
    System.out.println(query.retrieveJSON());

    // delete
    parts.delete(new Row().set("partname", "headphones"));
    TestCase.assertEquals(
        3,
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
    TestCase.assertEquals(1, products.query().orderBy("productname").retrieveRows().size());
  }

  @Test
  public void testRefBack() {

    Table users =
        schema.create(TableMetadata.table("User").add(Column.column("username").setPkey()));

    Table posts =
        schema.create(
            TableMetadata.table("Posts")
                .add(Column.column("title").setPkey())
                .add(Column.column("user").setType(ColumnType.REF).setRefTable(users.getName())));

    users
        .getMetadata()
        .add(
            Column.column("posts")
                .setType(ColumnType.REFBACK)
                .setRefTable(posts.getName())
                .setRefBack("user"));

    users.insert(new Row().set("username", "jack"));
    users.insert(new Row().set("username", "joe"));
    posts.insert(new Row().set("title", "joes post").set("user", "joe"));

    // now the magic, using refback update posts.user => 'jack'
    users.save(new Row().set("username", "jack").set("posts", "joes post"));

    // check via query we have now post for jack
    TestCase.assertEquals(
        "joes post",
        posts
            .query()
            .where(FilterBean.f("user", FilterBean.f("username", Operator.EQUALS, "jack")))
            .retrieveRows()
            .get(0)
            .getString("title"));

    // add another post for jack, should result in 'posts(user=jack,title=jacks post)
    posts.insert(new Row().set("title", "jacks post").set("user", "jack"));

    // check select on posts
    TestCase.assertEquals(
        2, // expect two posts, 'joes post' and 'jacks post'
        users
            .query()
            .select(SelectColumn.s("username"), SelectColumn.s("posts"))
            .where(FilterBean.f("username", Operator.EQUALS, "jack"))
            .retrieveRows()
            .get(0)
            .getStringArray("posts")
            .length);

    // check filter on posts
    TestCase.assertEquals(
        1,
        users
            .query()
            .where(FilterBean.f("posts", FilterBean.f("title", Operator.EQUALS, "jacks post")))
            .retrieveRows()
            .size());

    // check graph query
    Query query = users.agg(SelectColumn.s("count"));
    TestCase.assertTrue(query.retrieveJSON().contains("\"count\":2"));

    query =
        users
            .select(SelectColumn.s("username"), SelectColumn.s("posts", SelectColumn.s("title")))
            .where(FilterBean.f("posts", FilterBean.f("title", Operator.EQUALS, "jacks post")));
    TestCase.assertTrue(query.retrieveJSON().contains("jacks post"));

    query =
        users
            .agg(SelectColumn.s("count"))
            .where(FilterBean.f("posts", FilterBean.f("title", Operator.EQUALS, "jacks post")));
    TestCase.assertTrue(query.retrieveJSON().contains("\"count\":1"));

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
    TestCase.assertEquals(
        1,
        users
            .query()
            .select(SelectColumn.s("username"), SelectColumn.s("posts"))
            .where(FilterBean.f("username", Operator.EQUALS, "jack"))
            .retrieveRows()
            .get(0)
            .getStringArray("posts")
            .length);
  }
}
