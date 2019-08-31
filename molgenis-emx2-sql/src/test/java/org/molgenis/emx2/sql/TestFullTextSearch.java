package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.Type;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.utils.MolgenisException;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public class TestFullTextSearch {
  private static Database db;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testSearch() throws MolgenisException {

    // setup
    Schema schema = db.createSchema("TestFullTextSearch");
    Table aTable = schema.createTableIfNotExists("TestFullTextSearch");
    aTable
        .getMetadata()
        .addColumn("sub", Type.STRING)
        .addColumn("body", Type.TEXT)
        .addColumn("year", Type.INT);
    aTable.getMetadata().enableSearch();

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

    assertEquals(2, aTable.query().search("test").or().search("another").retrieve().size());

    // search accross join of xref
  }
}
