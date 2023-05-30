package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.emx2.Emx2Settings;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestEmx2Settings {
  private static Schema schema;

  @BeforeAll
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
    Map<String, String> settings = schema.getMetadata().getSettings();
    assertEquals("bar", settings.get("foo"));
  }
}
