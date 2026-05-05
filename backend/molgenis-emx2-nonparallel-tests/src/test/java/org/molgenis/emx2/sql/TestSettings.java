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

    database.tx(
        // prevent side effect of user changes on other tests using tx
        db -> {
          Schema schema = db.dropCreateSchema("testSchemaSettings");

          // set roles
          // viewer should only be able to see, not change
          // manager should be able to set values
          schema.addMember("testsettingseditor", EDITOR.toString());
          schema.addMember("testsettingsmanager", MANAGER.toString());

          db.setActiveUser("testsettingseditor");
          try {
            schema = db.getSchema("testSchemaSettings"); // reload schema
            schema.getMetadata().setSetting("key", "value");
            fail("editors should not be able to change schema settings");
          } catch (Exception e) {
            // failed correctly
          }

          db.setActiveUser("testsettingsmanager");
          try {
            schema = db.getSchema("testSchemaSettings"); // reload schema
            schema.getMetadata().setSetting("key", "value");
          } catch (Exception e) {
            e.printStackTrace();
            fail("managers should  be able to change schema settings");
          }

          assertEquals("value", schema.getMetadata().getSetting("key"));

          assertEquals("value", db.getSchema("testSchemaSettings").getMetadata().getSetting("key"));

          db.clearCache();

          assertEquals("value", db.getSchema("testSchemaSettings").getMetadata().getSetting("key"));

          db.becomeAdmin();
        });
  }

  @Test
  public void testTableSettings() {
    database.tx(
        // prevent side effect of user changes on other tests using tx
        db -> {
          Schema s = db.dropCreateSchema("testTableSettings");

          // set roles
          // viewer should only be able to see, not change
          // editor should be able to set values
          s.addMember("testtablesettingsviewer", VIEWER.toString());
          s.addMember("testtablesettingseditor", EDITOR.toString());

          s.create(table("test").add(column("test")));

          db.setActiveUser("testtablesettingsviewer");
          try {
            Table t = db.getSchema("testTableSettings").getTable("test");
            t.getMetadata().setSetting("key", "value");
            fail("viewers should not be able to change schema settings");
          } catch (Exception e) {
            // failed correctly
          }

          db.setActiveUser("testtablesettingseditor");
          try {
            Table t = db.getSchema("testTableSettings").getTable("test");
            t.getMetadata().setSetting("key", "value");
          } catch (Exception e) {
            e.printStackTrace();
            fail("managers should  be able to change schema settings");
          }

          db.clearCache();
          Map<String, String> test =
              db.getSchema("testTableSettings").getTable("test").getMetadata().getSettings();
          assertEquals(1, test.size());
          assertEquals("value", test.get("key"));

          assertEquals(
              "value",
              db.getSchema("testTableSettings").getTable("test").getMetadata().getSetting("key"));

          db.becomeAdmin();
        });
  }

  @Test
  public void testDatabaseSetting() {
    database.tx(
        db -> {
          db.setActiveUser("admin");
          db.setSetting("it-db-setting-key", "it-db-setting-value");
          assertEquals("it-db-setting-value", db.getSetting("it-db-setting-key"));
        });
  }

  @Test
  public void testDatabaseSettingCanNotBeSetByNonAdmin() {
    assertThrows(
        MolgenisException.class,
        () -> {
          database.tx(
              db -> {
                db.setActiveUser("testsettingsmanager");
                db.setSetting("it-db-setting-key", "it-db-setting-value");
              });
        });
  }

  @Test
  public void testDeleteDatabaseSetting() {
    // setup
    database.tx(
        db -> {
          db.setActiveUser("admin");
          db.setSetting("delete-me", "life is short");
        });

    // note, we refresh session on these changes so reload db
    database = TestDatabaseFactory.getTestDatabase();
    assertEquals(database.getSetting("delete-me"), "life is short");

    // execute
    database.tx(
        db -> {
          db.setActiveUser("admin");
          db.removeSetting("delete-me");
        });

    database = TestDatabaseFactory.getTestDatabase();
    assertNull(database.getSetting("delete-me"));
  }

  @Test
  public void testDeleteDatabaseSettingCanNotBeSetByNonAdmin() {
    assertThrows(
        MolgenisException.class,
        () -> {
          database.tx(
              db -> {
                db.setActiveUser("testsettingsmanager");
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
