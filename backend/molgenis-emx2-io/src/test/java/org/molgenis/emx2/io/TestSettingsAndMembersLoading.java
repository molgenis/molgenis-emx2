package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestSettingsAndMembersLoading {
  @Test
  public void testExcelTypesCast() {

    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(TestSettingsAndMembersLoading.class.getSimpleName());

    ClassLoader classLoader = getClass().getClassLoader();
    Path path = new File(classLoader.getResource("settings_and_members.xlsx").getFile()).toPath();

    new ImportExcelTask(path, schema, true).run();

    assertEquals("value1", schema.getTable("table1").getMetadata().getSetting("key1"));

    assertEquals("value2", schema.getMetadata().getSetting("key2"));

    assertEquals(1, schema.getMembers().size());
    assertEquals("anonymous", schema.getMembers().get(0).getUser());
    assertEquals("Viewer", schema.getMembers().get(0).getRole());

    database.dropSchema(schema.getName());
  }
}
