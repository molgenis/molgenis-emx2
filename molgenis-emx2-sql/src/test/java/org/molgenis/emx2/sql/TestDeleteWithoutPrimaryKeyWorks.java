package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.utils.MolgenisException;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public class TestDeleteWithoutPrimaryKeyWorks {
  private static Database db;

  @BeforeClass
  public static void setUp() , SQLException {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testDelete()  {
    Schema schema = db.createSchema(getClass().getSimpleName());

    Table table = schema.createTableIfNotExists("Test");
    table.getMetadata().addColumn("Col1");

    Row row = new Row().setString("Col1", "blaat");
    table.insert(row);

    assertEquals(1, table.retrieve().size());
    table.delete(row);
    assertEquals(0, table.retrieve().size());
  }
}
