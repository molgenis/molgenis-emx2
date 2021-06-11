package org.molgenis.emx2.sql;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;

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

    Assert.assertEquals("key", s.getMetadata().getSettings().get(0).getKey());
    Assert.assertEquals("value", s.getMetadata().getSettings().get(0).getValue());

    Assert.assertEquals(
        "key", db.getSchema("testSchemaSettings").getMetadata().getSettings().get(0).getKey());
    Assert.assertEquals(
        "value", db.getSchema("testSchemaSettings").getMetadata().getSettings().get(0).getValue());

    db.clearCache();

    Assert.assertEquals(
        "key", db.getSchema("testSchemaSettings").getMetadata().getSettings().get(0).getKey());
    Assert.assertEquals(
        "value", db.getSchema("testSchemaSettings").getMetadata().getSettings().get(0).getValue());
  }

  @Test
  public void testTableSettings() {
    Schema s = db.dropCreateSchema("testTableSettings");

    Table t = s.create(TableMetadata.table("test").add(Column.column("test")));
    t.getMetadata().setSetting("key", "value");

    db.clearCache();

    List<Setting> test =
        db.getSchema("testTableSettings").getTable("test").getMetadata().getSettings();
    assertEquals(1, test.size());
    Assert.assertEquals("key", test.get(0).getKey());
    Assert.assertEquals("value", test.get(0).getValue());

    Assert.assertEquals(
        "key",
        db.getSchema("testTableSettings")
            .getTable("test")
            .getMetadata()
            .getSettings()
            .get(0)
            .getKey());
    Assert.assertEquals(
        "value",
        db.getSchema("testTableSettings")
            .getTable("test")
            .getMetadata()
            .getSettings()
            .get(0)
            .getValue());
  }
}
