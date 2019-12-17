package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

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

    // add another post for jack, now the 'posts' should be updated also
    posts.insert(new Row().set("title", "jacks post").set("user", "jack"));
  }
}
