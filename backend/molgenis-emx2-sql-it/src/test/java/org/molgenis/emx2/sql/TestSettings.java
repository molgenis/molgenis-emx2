package org.molgenis.emx2.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Privileges.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Setting;
import org.molgenis.emx2.Table;

public class TestSettings {
  private static Database database;

  @BeforeClass
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testSchemaSettings() {

    database.tx(
        // prevent side effect of user changes on other tests using tx
        db -> {
          Schema s = db.dropCreateSchema("testSchemaSettings");

          // set roles
          // viewer should only be able to see, not change
          // manager should be able to set values
          s.addMember("testsettingseditor", EDITOR.toString());
          s.addMember("testsettingsmanager", MANAGER.toString());

          db.setActiveUser("testsettingseditor");
          try {
            s = db.getSchema("testSchemaSettings"); // reload schema
            s.getMetadata().setSetting("key", "value");
            fail("editors should not be able to change schema settings");
          } catch (Exception e) {
            // failed correctly
          }

          db.setActiveUser("testsettingsmanager");
          try {
            s = db.getSchema("testSchemaSettings"); // reload schema
            s.getMetadata().setSetting("key", "value");
          } catch (Exception e) {
            e.printStackTrace();
            fail("managers should  be able to change schema settings");
          }

          assertEquals("key", s.getMetadata().getSettings().get(0).key());
          assertEquals("value", s.getMetadata().getSettings().get(0).value());

          assertEquals(
              "key", db.getSchema("testSchemaSettings").getMetadata().getSettings().get(0).key());
          assertEquals(
              "value",
              db.getSchema("testSchemaSettings").getMetadata().getSettings().get(0).value());

          db.clearCache();

          assertEquals(
              "key", db.getSchema("testSchemaSettings").getMetadata().getSettings().get(0).key());
          assertEquals(
              "value",
              db.getSchema("testSchemaSettings").getMetadata().getSettings().get(0).value());

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
          List<Setting> test =
              db.getSchema("testTableSettings").getTable("test").getMetadata().getSettings();
          assertEquals(1, test.size());
          assertEquals("key", test.get(0).key());
          assertEquals("value", test.get(0).value());

          assertEquals(
              "key",
              db.getSchema("testTableSettings")
                  .getTable("test")
                  .getMetadata()
                  .getSettings()
                  .get(0)
                  .key());
          assertEquals(
              "value",
              db.getSchema("testTableSettings")
                  .getTable("test")
                  .getMetadata()
                  .getSettings()
                  .get(0)
                  .value());

          db.becomeAdmin();
        });
  }

  @Test
  public void testDatabaseSetting() {
    database.tx(
        db -> {
          db.setActiveUser("admin");
          db.createSetting("it-db-setting-key", "it-db-setting-value");
          var settings = db.getSettings();
          var setting = new Setting("it-db-setting-key", "it-db-setting-value");
          assertTrue(settings.contains(setting));
        });
  }

  @Test(expected = MolgenisException.class)
  public void testDatabaseSettingCanNotBeSetByNonAdmin() {
    database.tx(
        db -> {
          db.setActiveUser("testsettingsmanager");
          db.createSetting("it-db-setting-key", "it-db-setting-value");
        });
  }

  @Test
  public void testDeleteDatabaseSetting() {
    // setup
    database.tx(
        db -> {
          db.setActiveUser("admin");
          db.createSetting("delete-me", "life is short");
        });
    // execute
    database.tx(
        db -> {
          db.setActiveUser("admin");
          db.deleteSetting("delete-me");
          // verify
          var settings = db.getSettings();
          var setting = new Setting("delete-me", "life is short");
          assertFalse(settings.contains(setting));
        });
  }

  @Test(expected = MolgenisException.class)
  public void testDeleteDatabaseSettingCanNotBeSetByNonAdmin() {
    database.tx(
        db -> {
          db.setActiveUser("testsettingsmanager");
          db.deleteSetting("delete-me");
        });
  }
}
