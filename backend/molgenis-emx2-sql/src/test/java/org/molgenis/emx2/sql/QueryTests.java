package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Operator.NOT_EQUALS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;

public class QueryTests {

  static Database database;

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  void testQueryByRef() throws IOException {
    var schema = database.dropCreateSchema("test_query_by");
    var resources =
        schema.create(table("Resources", column("id", STRING).setKey(1), column("name", STRING)));
    var datasets =
        schema.create(
            table(
                "Datasets",
                column("id", STRING).setPkey(),
                column("resource", REF).setPkey().setRefTable("Resources")));
    resources.insert(row("id", "1", "name", "resource1"), row("id", "2", "name", "resource2"));
    datasets.insert(row("id", "1", "resource", "1"), row("id", "2", "resource", "2"));
    var query = datasets.query();
    query.where(f("resource", EQUALS, "1"));
    var rows = query.retrieveRows();
    assertEquals(1, rows.size(), "Expected one row");
    database.dropSchema(schema.getName());
  }

  @Test
  void testQueryNotRef() throws IOException {
    var schema = database.dropCreateSchema("test_query_by");
    var resources =
        schema.create(table("Resources", column("id", STRING).setKey(1), column("name", STRING)));
    var datasets =
        schema.create(
            table(
                "Datasets",
                column("id", STRING).setPkey(),
                column("resource", REF).setPkey().setRefTable("Resources")));
    resources.insert(row("id", "1", "name", "resource1"), row("id", "2", "name", "resource2"));
    datasets.insert(row("id", "1", "resource", "1"), row("id", "2", "resource", "2"));
    var query = datasets.query();
    query.where(f("resource", NOT_EQUALS, "1"));
    var rows = query.retrieveRows();
    assertEquals(1, rows.size(), "Expected one row");
    database.dropSchema(schema.getName());
  }

  @Test
  void testQueryByRefCompound() throws IOException {
    var schema = database.dropCreateSchema("test_query_by");
    var resources =
        schema.create(
            table("Resources", column("id", STRING).setPkey(), column("name", STRING).setPkey()));
    var datasets =
        schema.create(
            table(
                "Datasets",
                column("id", STRING).setPkey(),
                column("resource", REF).setPkey().setRefTable("Resources")));
    resources.insert(row("id", "1", "name", "resource1"), row("id", "2", "name", "resource2"));
    datasets.insert(
        row("id", "1", "resource.id", "1", "resource.name", "resource1"),
        row("id", "2", "resource.id", "2", "resource.name", "resource2"));
    var query = datasets.query();
    query.where(f("resource.id", EQUALS, "1"));
    var rows = query.retrieveRows();
    assertEquals(1, rows.size(), "Expected one row");
    database.dropSchema(schema.getName());
  }

  @Test
  void testQueryByRefCompound2() throws IOException {
    var schema = database.dropCreateSchema("test_query_by");
    var resources =
        schema.create(
            table("Resources", column("id", STRING).setPkey(), column("name", STRING).setPkey()));
    var datasets =
        schema.create(
            table(
                "Datasets",
                column("id", STRING).setPkey(),
                column("resource", REF).setPkey().setRefTable("Resources")));
    resources.insert(row("id", "1", "name", "resource1"), row("id", "2", "name", "resource2"));
    datasets.insert(
        row("id", "1", "resource.id", "1", "resource.name", "resource1"),
        row("id", "2", "resource.id", "2", "resource.name", "resource2"));
    var query = datasets.query();
    var expectedException = false;
    try {
      query.where(f("resource", EQUALS, "1"));
      var rows = query.retrieveRows();
    } catch (Exception ex) {
      expectedException = true;
    }
    assertTrue(
        expectedException,
        "Expected an exception when querying by field resource instead of resource.id.");
    database.dropSchema(schema.getName());
  }
}
