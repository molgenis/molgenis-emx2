package org.molgenis.emx2.sql;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Setting;
import org.molgenis.emx2.Table;

public class TestSettings {
  private static Database db;

  @BeforeClass
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testSchemaSettings() {
    Schema s = db.dropCreateSchema("testSchemaSettings");
    s.getMetadata().setSetting("key", "value");

    assertEquals("key", s.getMetadata().getSettings().get(0).getKey());
    assertEquals("value", s.getMetadata().getSettings().get(0).getValue());

    assertEquals(
        "key", db.getSchema("testSchemaSettings").getMetadata().getSettings().get(0).getKey());
    assertEquals(
        "value", db.getSchema("testSchemaSettings").getMetadata().getSettings().get(0).getValue());

    db.clearCache();

    assertEquals(
        "key", db.getSchema("testSchemaSettings").getMetadata().getSettings().get(0).getKey());
    assertEquals(
        "value", db.getSchema("testSchemaSettings").getMetadata().getSettings().get(0).getValue());
  }

  @Test
  public void testTableSettings() {
    Schema s = db.dropCreateSchema("testTableSettings");

    Table t = s.create(table("test").add(column("test")));
    t.getMetadata().setSetting("key", "value");

    db.clearCache();

    List<Setting> test =
        db.getSchema("testTableSettings").getTable("test").getMetadata().getSettings();
    assertEquals(1, test.size());
    assertEquals("key", test.get(0).getKey());
    assertEquals("value", test.get(0).getValue());

    assertEquals(
        "key",
        db.getSchema("testTableSettings")
            .getTable("test")
            .getMetadata()
            .getSettings()
            .get(0)
            .getKey());
    assertEquals(
        "value",
        db.getSchema("testTableSettings")
            .getTable("test")
            .getMetadata()
            .getSettings()
            .get(0)
            .getValue());
  }
}
