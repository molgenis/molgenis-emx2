package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.TEXT;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.FilterBean.or;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.PetStoreLoader;

public class TestFullTextSearch {
  private static Database db;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testSearch() {

    // setup
    Schema schema = db.dropCreateSchema(TestFullTextSearch.class.getSimpleName());
    Table aTable =
        schema.create(
            table("TestFullTextSearch")
                .add(column("sub").setPkey())
                .add(column("body").setType(TEXT))
                .add(column("year").setType(INT)));
    // aTable.getMetadata().enableSearch();

    aTable.insert(
        new Row()
            .setString("sub", "test subject")
            .setString("body", "test body")
            .setInt("year", 1976),
        new Row()
            .setString("sub", "another subject")
            .setString("body", "another body")
            .setInt("year", 1977),
        new Row().setString("sub", "hgvs").setString("body", "c.19239T>G").setInt("year", 1977),
        new Row()
            .setString("sub", "some disease")
            .setString("body", "neoplasm cancer")
            .setInt("year", 1977));

    // search in one table
    assertEquals(1, aTable.query().search("test").retrieveRows().size());

    assertEquals(1, aTable.query().search("another").retrieveRows().size());

    assertEquals(0, aTable.query().search("test").search("another").retrieveRows().size());

    assertEquals(1, aTable.query().search("c.19239T>G").retrieveRows().size());

    // match by position
    // assertEquals(1, aTable.query().search("19239").retrieve().size());

    // assertEquals(1, aTable.query().search("c.19239").retrieve().size());

    // match by mutation
    assertEquals(1, aTable.query().search("T>G").retrieveRows().size());

    // don't match other mutation
    // assertEquals(0, aTable.query().search("c.19239T>C").getRows().size());

    // search accross join of xref
  }

  @Test
  public void nestedSearch() {
    Schema schema = db.dropCreateSchema(TestFullTextSearch.class.getSimpleName() + "nested");
    new PetStoreLoader().load(schema, true);

    List<Row> result =
        schema.query("Order").where(f(Operator.TEXT_SEARCH, "Delivered")).retrieveRows();
    assertEquals(result.size(), 1);

    // nesting example 3
    result =
        schema
            .query("Order")
            .where(
                or(
                    f(Operator.TRIGRAM_SEARCH, "approved"),
                    f("pet", f(Operator.TRIGRAM_SEARCH, "cat"))))
            .retrieveRows();
    assertEquals(2, result.size());

    result = schema.query("Order").where(f(Operator.TEXT_SEARCH, "cat")).retrieveRows();
    assertEquals(0, result.size());

    // nesting example 1
    result = schema.query("Order").where(f("pet", f(Operator.TEXT_SEARCH, "cat"))).retrieveRows();
    assertEquals(1, result.size());

    String json =
        schema.query("Order").where(f("pet", f(Operator.TEXT_SEARCH, "cat"))).retrieveJSON();
    // would exclude approved order
    assertTrue(!json.contains("approved"));

    // nesting example 2
    json =
        schema
            .query("Order")
            .where(
                or(f(Operator.TEXT_SEARCH, "approved"), f("pet", f(Operator.TEXT_SEARCH, "cat"))))
            .retrieveJSON();
    // would include approved
    assertTrue(json.contains("approved"));
  }
}
