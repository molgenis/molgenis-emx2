package org.molgenis.emx2.io;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Setting;
import org.molgenis.emx2.io.emx2.Emx2Settings;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestEmx2Settings {
  private static Schema schema;

  @BeforeClass
  public static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestEmx2Settings.class.getSimpleName());
  }

  @Test
  public void testRolesIO() {
    // create settings
    schema.getMetadata().setSetting("foo", "bar");

    // export
    TableStore store = new TableStoreForCsvInMemory();
    Emx2Settings.outputSettings(store, schema);

    // empty the database, verify
    schema = schema.getDatabase().dropCreateSchema(TestEmx2Settings.class.getSimpleName());
    assertEquals(0, schema.getMetadata().getSettings().size());

    // import and see if consistent
    Emx2Settings.inputSettings(store, schema);
    List<Setting> settings = schema.getMetadata().getSettings();
    assertEquals("foo", settings.get(0).getKey());
    assertEquals("bar", settings.get(0).getValue());
  }
}
