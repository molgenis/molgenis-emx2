package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.TEXT;
import static org.molgenis.emx2.TableMetadata.table;

public class TestFullTextSearch {
  private static Database db;

  @BeforeClass
  public static void setUp() throws SQLException {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testSearch() {

    // setup
    Schema schema = db.dropCreateSchema("TestFullTextSearch");
    Table aTable =
        schema.create(
            table("TestFullTextSearch")
                .add(column("sub"))
                .pkey("sub")
                .add(column("body").type(TEXT))
                .add(column("year").type(INT)));
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
    assertEquals(1, aTable.query().search("test").getRows().size());

    assertEquals(1, aTable.query().search("another").getRows().size());

    assertEquals(0, aTable.query().search("test").search("another").getRows().size());

    assertEquals(1, aTable.query().search("c.19239T>G").getRows().size());

    // match by position
    // assertEquals(1, aTable.query().search("19239").retrieve().size());

    // assertEquals(1, aTable.query().search("c.19239").retrieve().size());

    // match by mutation
    assertEquals(1, aTable.query().search("T>G").getRows().size());

    // don't match other mutation
    // assertEquals(0, aTable.query().search("c.19239T>C").getRows().size());

    // search accross join of xref
  }
}
