package org.molgenis.emx2.io;

import static org.junit.Assert.*;
import static spark.Service.ignite;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.junit.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.tablestore.TableStoreForURL;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.Service;

public class TestTableStoreForURL {

  @Test
  public void testTableStoreForURL() throws MalformedURLException, InterruptedException {
    Service http = ignite().port(8081);
    try {
      http.staticFiles.location("/TestImportTableTask");
      http.get("/test.csv", (req, res) -> "name,description\ntest,some description");
      http.awaitInitialization();

      // test store
      TableStoreForURL store = new TableStoreForURL(new URL("http://localhost:8081/"));
      assertTrue(store.containsTable("test"));
      assertFalse(store.containsTable("test2"));

      // test task
      Schema schema =
          TestDatabaseFactory.getTestDatabase()
              .dropCreateSchema(TestTableStoreForURL.class.getSimpleName());

      MolgenisIO.fromURL(new URL("http://localhost:8081"), schema, false);

      // check if success
      assertNotNull(schema.getMetadata().getTableMetadata("test"));
      assertEquals("a", schema.getTable("test").retrieveRows().get(0).getString("col1"));

      // close
    } finally {
      http.stop();
    }
  }

  @Test
  public void testImportSchemaList() throws MalformedURLException {
    Database database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(TestTableStoreForURL.class.getSimpleName() + 1);
    database.dropSchemaIfExists(TestTableStoreForURL.class.getSimpleName() + 2);

    List<SchemaDeclaration> schemas =
        List.of(
            new SchemaDeclaration(
                TestTableStoreForURL.class.getSimpleName() + 1,
                "",
                List.of(new URL("http://localhost:8081"))),
            new SchemaDeclaration(
                TestTableStoreForURL.class.getSimpleName() + 2,
                "",
                List.of(new URL("http://localhost:8081"))));

    Service http = ignite().port(8081);
    try {
      http.staticFiles.location("/TestImportTableTask");
      http.get("/test.csv", (req, res) -> "name,description\ntest,some description");
      http.awaitInitialization();

      MolgenisIO.fromSchemaList(TestDatabaseFactory.getTestDatabase(), schemas, false);

    } finally {
      http.stop();
    }
  }
}
