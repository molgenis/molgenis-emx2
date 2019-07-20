package org.molgenis.sql.psql.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.beans.RowBean;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public class TestSearch {
  private static Database db;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    db = DatabaseFactory.getDatabase();
  }

  @Test
  public void testSearch() throws MolgenisException {

    // setup
    Schema s = db.createSchema("TestSearch");
    Table t = s.createTable("TestSearch");
    t.addColumn("sub", Column.Type.STRING);
    t.addColumn("body", Column.Type.TEXT);
    t.addColumn("year", Column.Type.INT);
    t.enableSearch();

    t.insert(
        new RowBean()
            .setString("sub", "test subject")
            .setString("body", "test body")
            .setInt("year", 1976),
        new RowBean()
            .setString("sub", "another subject")
            .setString("body", "another body")
            .setInt("year", 1977),
        new RowBean().setString("sub", "hgvs").setString("body", "c.19239T>G").setInt("year", 1977),
        new RowBean()
            .setString("sub", "some disease")
            .setString("body", "neoplasm cancer")
            .setInt("year", 1977));

    // search in one table
    assertEquals(1, t.query().search("test").retrieve().size());

    assertEquals(2, t.query().search("test").or().search("another").retrieve().size());

    // search accross join of xref
  }
}
