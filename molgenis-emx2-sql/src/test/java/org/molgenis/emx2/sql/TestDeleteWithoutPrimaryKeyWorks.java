package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

public class TestDeleteWithoutPrimaryKeyWorks {
  private static Database db;

  @BeforeClass
  public static void setUp() throws SQLException {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testDelete() {
    Schema schema = db.createSchema(getClass().getSimpleName());

    Table table = schema.create(table("Test").addColumn(column("Col1")));

    Row row = new Row().setString("Col1", "blaat");
    table.insert(row);

    assertEquals(1, table.getRows().size());
    table.delete(row);
    assertEquals(0, table.getRows().size());
  }
}
