package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

public class TestSettings {
  private static Database db;

  @BeforeClass
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testSchemaSettings() {
    Schema s = db.dropCreateSchema("testSchemaSettings");
    Map<String, String> settings = Map.of("key", "value");
    s.getMetadata().setSettings(settings);
    assertEquals(settings, s.getMetadata().getSettings());
    assertEquals(settings, db.getSchema("testSchemaSettings").getMetadata().getSettings());
    db.clearCache();
    assertEquals(settings, db.getSchema("testSchemaSettings").getMetadata().getSettings());
  }

  @Test
  public void testTableSettings() {
    Schema s = db.dropCreateSchema("testTableSettings");
    Map<String, String> settings = Map.of("key", "value");

    Table t = s.create(table("test").add(column("test")));
    t.getMetadata().setSettings(settings);

    assertEquals(settings, t.getMetadata().getSettings());
    assertEquals(
        settings, db.getSchema("testTableSettings").getTable("test").getMetadata().getSettings());
    db.clearCache();
    assertEquals(
        settings, db.getSchema("testTableSettings").getTable("test").getMetadata().getSettings());
  }
}
