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
    List<Setting> settings = List.of(new Setting("key", "value"));

    Table t = s.create(table("test").add(column("test")));
    t.getMetadata().setSettings(settings);

    assertEquals(settings, t.getMetadata().getSettings());
    assertEquals(
        settings, db.getSchema("testTableSettings").getTable("test").getMetadata().getSettings());
    db.clearCache();
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
