package org.molgenis.emx2.sql;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;

public class TestSqlRawQueryForSchema {

  private static Database database;

  @BeforeClass
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testSql() {
    Schema schema = database.getSchema("pet store");
    List<Row> rows = schema.retrieveSql("Select * from \"Pet\",\"Order\"");
    assertEquals(8, rows.size());
  }
}
