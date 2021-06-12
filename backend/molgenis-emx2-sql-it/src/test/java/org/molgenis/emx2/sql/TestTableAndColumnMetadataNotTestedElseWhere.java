package org.molgenis.emx2.sql;

import static junit.framework.TestCase.*;

import java.util.ArrayList;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;

public class TestTableAndColumnMetadataNotTestedElseWhere {
  private static Database db;

  @BeforeClass
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testAlterColumnName() {
    try {
      Schema s = db.dropCreateSchema("testAlterColumnName");
      Table t = s.create(TableMetadata.table("test").add(Column.column("test")));
      System.out.println(t);

      t.getMetadata().alterColumn("test", Column.column("test2"));
      TestCase.assertNull(t.getMetadata().getColumn("test"));
      TestCase.assertNotNull(t.getMetadata().getColumn("test2"));

      t.insert(new Row().set("test", "value").set("test2", "value"));
      TestCase.assertNull(t.retrieveRows().get(0).getString("test"));
      TestCase.assertEquals("value", t.retrieveRows().get(0).getString("test2"));
    } catch (MolgenisException me) {
      System.out.println("Error unexpected:\n" + me);
    }
  }

  @Test
  public void testColumnPosition() {
    Schema s = db.dropCreateSchema("testColumnPosition");
    TableMetadata t =
        s.create(TableMetadata.table("test", Column.column("col1"), Column.column("col2")))
            .getMetadata();

    TestCase.assertEquals("col1", new ArrayList<>(t.getColumnNames()).get(0));
    t.add(Column.column("col3"));
    TestCase.assertEquals((Integer) 2, t.getColumn("col3").getPosition());
    db.clearCache();

    t = db.getSchema("testColumnPosition").getTable("test").getMetadata();
    TestCase.assertEquals("col1", new ArrayList<>(t.getColumnNames()).get(0));
    TestCase.assertEquals((Integer) 2, t.getColumn("col3").getPosition());

    t.alterColumn("col2", Column.column("col2").setPosition(0));
    TestCase.assertEquals(new ArrayList<>(t.getColumnNames()).get(0), "col2");
    db.clearCache();
    t = db.getSchema("testColumnPosition").getTable("test").getMetadata();
    TestCase.assertEquals(new ArrayList<>(t.getColumnNames()).get(0), "col2");
  }
}
