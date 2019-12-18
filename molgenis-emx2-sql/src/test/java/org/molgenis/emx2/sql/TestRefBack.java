package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.molgenis.emx2.Operator.EQUALS;

public class TestRefBack {

  private static Schema schema;

  @BeforeClass
  public static void setUp() {
    Database database = DatabaseFactory.getTestDatabase();
    schema = database.createSchema(TestRefBack.class.getSimpleName());
  }

  @Test
  public void testRefBack() {

    Table users = schema.createTableIfNotExists("User");
    users.getMetadata().addColumn("username").primaryKey();

    Table posts = schema.createTableIfNotExists("Posts");
    posts.getMetadata().addColumn("title").primaryKey();
    posts
        .getMetadata()
        .addRef("user", users.getName(), "username")
        .setReverseReference("posts", "title");

    users.insert(new Row().set("username", "jack"));
    users.insert(new Row().set("username", "joe"));

    posts.insert(new Row().set("title", "joes post").set("username", "joe"));

    // now the magic
    users.update(new Row().set("username", "jack").set("posts", "joes post"));

    // check via query we have now post for jack
    assertEquals(1, posts.query().where("user", EQUALS, "jack").retrieve().size());

    // add another post for jack, now the 'posts' should be updated also
    posts.insert(new Row().set("title", "jacks post").set("user", "jack"));

    // check, should now have two posts in posts array
    assertEquals(
        2,
        users
            .query()
            .where("username", EQUALS, "jack")
            .retrieve()
            .get(0)
            .getStringArray("posts")
            .length);

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
