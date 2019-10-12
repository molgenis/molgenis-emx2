package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.ColumnType;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public class TestFullTextSearch {
  private static Database db;

  @BeforeClass
  public static void setUp() throws SQLException {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testSearch() {

    // setup
    Schema schema = db.createSchema("TestFullTextSearch");
    Table aTable = schema.createTableIfNotExists("TestFullTextSearch");
    aTable
        .getMetadata()
        .addColumn("sub", ColumnType.STRING)
        .addColumn("body", ColumnType.TEXT)
        .addColumn("year", ColumnType.INT);
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
    assertEquals(1, aTable.query().search("test").retrieve().size());

    assertEquals(2, aTable.query().search("test").search("another").retrieve().size());

    assertEquals(1, aTable.query().search("c.19239T>G").retrieve().size());

    // match by position
    // assertEquals(1, aTable.query().search("19239").retrieve().size());

    // assertEquals(1, aTable.query().search("c.19239").retrieve().size());

    // match by mutation
    assertEquals(1, aTable.query().search("T>G").retrieve().size());

    // don't match other mutation
    assertEquals(0, aTable.query().search("c.19239T>C").retrieve().size());

    // search accross join of xref
  }
}
