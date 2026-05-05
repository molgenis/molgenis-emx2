package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;

public class TestSettingsMerge {
  private static Database database;

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void setSettingsMergesOnTopOfExistingDbState() {
    database.tx(
        db -> {
          db.setActiveUser("admin");
          db.setSetting("merge-key-a", "1");
          db.setSetting("merge-key-b", "2");
        });

    database = TestDatabaseFactory.getTestDatabase();

    database.tx(
        db -> {
          db.setActiveUser("admin");
          db.setSettings(Map.of("merge-key-a", "99"));
          assertEquals("99", db.getSetting("merge-key-a"));
          assertEquals("2", db.getSetting("merge-key-b"));
        });

    database = TestDatabaseFactory.getTestDatabase();
    assertEquals("99", database.getSetting("merge-key-a"));
    assertEquals("2", database.getSetting("merge-key-b"));

    database.tx(
        db -> {
          db.setActiveUser("admin");
          db.removeSetting("merge-key-a");
          db.removeSetting("merge-key-b");
        });
  }

  @Test
  public void setSettingsMergeVerifiedAfterClearCache() {
    database.tx(
        db -> {
          db.setActiveUser("admin");
          db.setSetting("cache-test-key-a", "original");
          db.setSetting("cache-test-key-b", "keeper");
        });

    database = TestDatabaseFactory.getTestDatabase();

    database.tx(
        db -> {
          db.setActiveUser("admin");
          db.setSettings(Map.of("cache-test-key-a", "updated"));
        });

    database.clearCache();
    assertEquals("updated", database.getSetting("cache-test-key-a"));
    assertEquals("keeper", database.getSetting("cache-test-key-b"));

    database.tx(
        db -> {
          db.setActiveUser("admin");
          db.removeSetting("cache-test-key-a");
          db.removeSetting("cache-test-key-b");
        });
  }

  @Test
  public void removeSettingDeletesKeyFromDb() {
    database.tx(
        db -> {
          db.setActiveUser("admin");
          db.setSetting("remove-test-key", "to-be-gone");
        });

    database = TestDatabaseFactory.getTestDatabase();
    assertEquals("to-be-gone", database.getSetting("remove-test-key"));

    database.tx(
        db -> {
          db.setActiveUser("admin");
          db.removeSetting("remove-test-key");
        });

    database = TestDatabaseFactory.getTestDatabase();
    assertNull(database.getSetting("remove-test-key"));
  }
}
