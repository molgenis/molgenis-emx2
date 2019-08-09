package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public class TestSearch {
  private static Database db;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testSearch() throws MolgenisException {

    // setup
    Schema schema = db.createSchema("TestSearch");
    Table aTable = schema.createTableIfNotExists("TestSearch");
    aTable.addColumn("sub", Type.STRING);
    aTable.addColumn("body", Type.TEXT);
    aTable.addColumn("year", Type.INT);
    aTable.enableSearch();

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
