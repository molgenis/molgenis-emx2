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
    db = SqlTestHelper.getEmptyDatabase();
  }

  @Test
  public void testSearch() throws MolgenisException {

    // setup
    Schema s = db.createSchema("TestSearch");
    Table t = s.createTable("TestSearch");
    t.addColumn("subject", Column.Type.STRING);
    t.addColumn("body", Column.Type.TEXT);
    t.addColumn("year", Column.Type.INT);
    t.enableSearch();

    t.insert(
        new RowBean()
            .setString("subject", "test subject")
            .setString("body", "test body")
            .setInt("year", 1976),
        new RowBean()
            .setString("subject", "another subject")
            .setString("body", "another body")
            .setInt("year", 1977),
        new RowBean()
            .setString("subject", "hgvs")
            .setString("body", "c.19239T>G")
            .setInt("year", 1977),
        new RowBean()
            .setString("subject", "some disease")
            .setString("body", "neoplasm cancer")
            .setInt("year", 1977));

    // search
    assertEquals(1, t.query().search("test").retrieve().size());
  }
}
