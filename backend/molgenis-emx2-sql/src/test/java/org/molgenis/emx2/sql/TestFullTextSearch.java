package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.TEXT;
import static org.molgenis.emx2.TableMetadata.table;

public class TestFullTextSearch {
  private static Database db;

  @BeforeClass
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testSearch() {

    // setup
    Schema schema = db.dropCreateSchema("TestFullTextSearch");
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
}
