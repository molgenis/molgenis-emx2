package org.molgenis.emx2.io;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.rowstore.TableStoreForCsvFilesDirectory;
import org.molgenis.emx2.sql.TestDatabaseFactory;

import java.io.File;

import static junit.framework.TestCase.assertEquals;
import static org.molgenis.emx2.SelectColumn.s;

public class TestLegacyImport {
  static Database db;

  @BeforeClass
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testLegacyImport() {

    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("bbmri-nl-complete").getFile());
    TableStoreForCsvFilesDirectory store = new TableStoreForCsvFilesDirectory(file.toPath(), ',');

    Schema schema = db.dropCreateSchema("testImportLegacyFormat");
    SchemaImport.executeImport(store, schema);

    assertEquals(22, schema.getTableNames().size());

    System.out.println("search GrONingen");
    for (Row r :
        schema
            .getTable("biobanks")
            .select(
                s("name"),
                s("contact_person", s("full_name")),
                s("principal_investigators", s("full_name")),
                s("juristic_person", s("name")))
            .search("GrONingen")
            .getRows()) {
      System.out.println(r.getString("name"));
    }

    System.out.println("search groningen");
    for (Row r : schema.getTable("biobanks").search("groningen").getRows()) {
      System.out.println(r.getString("name"));
    }

    System.out.println("search rotterdam");
    for (Row r : schema.getTable("biobanks").search("rotterdam").getRows()) {
      System.out.println(r.getString("name"));
    }
  }
}
