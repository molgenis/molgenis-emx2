package org.molgenis.emx2.sql;

import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.examples.PetStoreExample;

public class TestFullTextSearch {
  private static Database db;

  @BeforeClass
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testSearch() {

    // setup
    Schema schema = db.dropCreateSchema(TestFullTextSearch.class.getSimpleName());
    Table aTable =
        schema.create(
            TableMetadata.table("TestFullTextSearch")
                .add(Column.column("sub").setPkey())
                .add(Column.column("body").setType(ColumnType.TEXT))
                .add(Column.column("year").setType(ColumnType.INT)));
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
    Assert.assertEquals(1, aTable.query().search("test").retrieveRows().size());

    Assert.assertEquals(1, aTable.query().search("another").retrieveRows().size());

    Assert.assertEquals(0, aTable.query().search("test").search("another").retrieveRows().size());

    Assert.assertEquals(1, aTable.query().search("c.19239T>G").retrieveRows().size());

    // match by position
    // assertEquals(1, aTable.query().search("19239").retrieve().size());

    // assertEquals(1, aTable.query().search("c.19239").retrieve().size());

    // match by mutation
    Assert.assertEquals(1, aTable.query().search("T>G").retrieveRows().size());

    // don't match other mutation
    // assertEquals(0, aTable.query().search("c.19239T>C").getRows().size());

    // search accross join of xref
  }

  @Test
  public void nestedSearch() {
    Schema schema = db.dropCreateSchema(TestFullTextSearch.class.getSimpleName() + "nested");
    PetStoreExample.create(schema.getMetadata());
    PetStoreExample.populate(schema);

    List<Row> result =
        schema.query("Order").where(FilterBean.f(Operator.TEXT_SEARCH, "Delivered")).retrieveRows();
    Assert.assertEquals(result.size(), 1);

    // nesting example 3
    result =
        schema
            .query("Order")
            .where(
                FilterBean.or(
                    FilterBean.f(Operator.TRIGRAM_SEARCH, "approved"),
                    FilterBean.f("pet", FilterBean.f(Operator.TRIGRAM_SEARCH, "cat"))))
            .retrieveRows();
    Assert.assertEquals(2, result.size());

    result = schema.query("Order").where(FilterBean.f(Operator.TEXT_SEARCH, "cat")).retrieveRows();
    Assert.assertEquals(0, result.size());

    // nesting example 1
    result =
        schema
            .query("Order")
            .where(FilterBean.f("pet", FilterBean.f(Operator.TEXT_SEARCH, "cat")))
            .retrieveRows();
    Assert.assertEquals(1, result.size());

    String json =
        schema
            .query("Order")
            .where(FilterBean.f("pet", FilterBean.f(Operator.TEXT_SEARCH, "cat")))
            .retrieveJSON();
    // would exclude approved order
    Assert.assertTrue(!json.contains("approved"));

    // nesting example 2
    json =
        schema
            .query("Order")
            .where(
                FilterBean.or(
                    FilterBean.f(Operator.TEXT_SEARCH, "approved"),
                    FilterBean.f("pet", FilterBean.f(Operator.TEXT_SEARCH, "cat"))))
            .retrieveJSON();
    // would include approved
    Assert.assertTrue(json.contains("approved"));
  }
}
