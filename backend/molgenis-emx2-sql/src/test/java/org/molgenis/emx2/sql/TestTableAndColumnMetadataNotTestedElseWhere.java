package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestTableAndColumnMetadataNotTestedElseWhere {
  private static Database db;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testAlterColumnName() {
    try {
      Schema s = db.dropCreateSchema("testAlterColumnName");
      Table t = s.create(table("test").add(column("test")));
      System.out.println(t);

      t.getMetadata().alterColumn("test", column("test2"));
      assertNull(t.getMetadata().getColumn("test"));
      assertNotNull(t.getMetadata().getColumn("test2"));

      t.insert(new Row().set("test", "value").set("test2", "value"));
      assertNull(t.retrieveRows().get(0).getString("test"));
      assertEquals("value", t.retrieveRows().get(0).getString("test2"));
    } catch (MolgenisException me) {
      System.out.println("Error unexpected:\n" + me);
    }
  }

  @Test
  public void testColumnPosition() {
    Schema s = db.dropCreateSchema("testColumnPosition");
    TableMetadata t = s.create(table("test", column("col1"), column("col2"))).getMetadata();

    assertEquals("col1", new ArrayList<>(t.getColumnNames()).get(0));
    t.add(column("col3"));
    assertEquals((Integer) 2, t.getColumn("col3").getPosition());
    db.clearCache();

    t = db.getSchema("testColumnPosition").getTable("test").getMetadata();
    assertEquals("col1", new ArrayList<>(t.getColumnNames()).get(0));
    assertEquals((Integer) 2, t.getColumn("col3").getPosition());

    t.alterColumn("col2", column("col2").setPosition(0));
    t.alterColumn("col3", column("col3").setPosition(1));

    // when alter without position given then position should be untouched
    t.alterColumn("col3", column("col3").setType(ColumnType.TEXT));
    db.clearCache();
    t = db.getSchema("testColumnPosition").getTable("test").getMetadata();
    assertEquals((Integer) 1, t.getColumn("col3").getPosition());

    t.alterColumn("col1", column("col1").setPosition(2));
    assertEquals(new ArrayList<>(t.getColumnNames()).get(0), "col2");
    db.clearCache();
    t = db.getSchema("testColumnPosition").getTable("test").getMetadata();
    assertEquals(new ArrayList<>(t.getColumnNames()).get(0), "col2");
  }
}
