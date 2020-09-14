package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

import java.util.Map;

import static org.junit.Assert.assertEquals;

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
    db.clearCache();
    assertEquals(settings, db.getSchema("testSchemaSettings").getMetadata().getSettings());
  }
}
