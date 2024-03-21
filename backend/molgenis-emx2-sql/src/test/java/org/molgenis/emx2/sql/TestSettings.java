package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Privileges.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestSettings {
  private static Database database;

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testSchemaSettings() {

    Schema schema = database.dropCreateSchema("testSchemaSettings");

    // set roles
    // viewer should only be able to see, not change
    // manager should be able to set values
    schema.addMember("testsettingseditor", EDITOR.toString());
    schema.addMember("testsettingsmanager", MANAGER.toString());

    SqlDatabase editorDb = new SqlDatabase("testsettingseditor");
    try {
      schema = editorDb.getSchema("testSchemaSettings"); // reload schema
      schema.getMetadata().setSetting("key", "value");
      fail("editors should not be able to change schema settings");
    } catch (Exception e) {
      // failed correctly
    }

    SqlDatabase managerDb = new SqlDatabase("testsettingsmanager");
    try {
      schema = managerDb.getSchema("testSchemaSettings"); // reload schema
      schema.getMetadata().setSetting("key", "value");
    } catch (Exception e) {
      fail("managers should  be able to change schema settings, got error: " + e.getMessage());
    }

    assertEquals("value", schema.getMetadata().getSetting("key"));

    database.clearCache();

    assertEquals("value", database.getSchema("testSchemaSettings").getMetadata().getSetting("key"));
  }

  @Test
  public void testTableSettings() {
    Schema s = database.dropCreateSchema("testTableSettings");

    // set roles
    // viewer should only be able to see, not change
    // editor should be able to set values
    s.addMember("testtablesettingsviewer", VIEWER.toString());
    s.addMember("testtablesettingseditor", EDITOR.toString());

    s.create(table("test").add(column("test")));

    SqlDatabase viewerDb = new SqlDatabase("testtablesettingsviewer");
    try {
      Table t = viewerDb.getSchema("testTableSettings").getTable("test");
      t.getMetadata().setSetting("key", "value");
      fail("viewers should not be able to change schema settings");
    } catch (Exception e) {
      // failed correctly
    }

    SqlDatabase editorDb = new SqlDatabase("testtablesettingseditor");
    try {
      Table t = editorDb.getSchema("testTableSettings").getTable("test");
      t.getMetadata().setSetting("key", "value");
    } catch (Exception e) {
      e.printStackTrace();
      fail("managers should  be able to change schema settings");
    }

    database.clearCache();
    Map<String, String> test =
        database.getSchema("testTableSettings").getTable("test").getMetadata().getSettings();
    assertEquals(1, test.size());
    assertEquals("value", test.get("key"));

    assertEquals(
        "value",
        database.getSchema("testTableSettings").getTable("test").getMetadata().getSetting("key"));
  }

  @Test
  public void testDatabaseSetting() {
    database.setSetting("it-db-setting-key", "it-db-setting-value");
    assertEquals("it-db-setting-value", database.getSetting("it-db-setting-key"));
  }

  @Test
  public void testDatabaseSettingCanNotBeSetByNonAdmin() {
    assertThrows(
        MolgenisException.class,
        () ->
            new SqlDatabase("testsettingsmanager")
                .setSetting("it-db-setting-key", "it-db-setting-value"));
  }

  @Test
  public void testDeleteDatabaseSetting() {
    // setup
    database.setSetting("delete-me", "life is short");

    // note, we refresh session on these changes so reload db
    database.clearCache();
    assertEquals(database.getSetting("delete-me"), "life is short");

    // execute
    database.removeSetting("delete-me");

    database = TestDatabaseFactory.getTestDatabase();
    assertNull(database.getSetting("delete-me"));
  }

  @Test
  public void testDeleteDatabaseSettingCanNotBeSetByNonAdmin() {
    assertThrows(
        MolgenisException.class,
        () -> {
          new SqlDatabase("testsettingsmanager")
              .tx(
                  db -> {
                    db.removeSetting("delete-me");
                  });
        });
  }

  @Test
  public void testUserSettings() {
    User user = database.addUser("SettingsTestUser");
    user.setSetting("my-setting", "user setting");
    database.saveUser(user);
    assertEquals(database.getUser("SettingsTestUser").getSetting("my-setting"), "user setting");
    user = database.getUser("SettingsTestUser").removeSetting("my-setting");
    database.saveUser(user);
    assertNull(database.getUser("SettingsTestUser").getSetting("my-setting"));
  }
}
