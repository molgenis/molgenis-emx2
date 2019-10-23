package org.molgenis.emx2.io;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.stores.RowStoreForCsvFilesDirectory;
import org.molgenis.emx2.sql.DatabaseFactory;

import java.io.File;

import static junit.framework.TestCase.assertEquals;

public class TestLegacyImport {
  static Database db;

  @BeforeClass
  public static void setup() {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testLegacyImport() {

    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("bbmri-nl-complete").getFile());
    RowStoreForCsvFilesDirectory store = new RowStoreForCsvFilesDirectory(file.toPath(), ',');

    Schema schema = db.createSchema("testImportLegacyFormat");
    MolgenisImport.executeImport(store, schema);

    assertEquals(22, schema.getTableNames().size());

    System.out.println("search GrONingen");
    for (Row r :
        schema
            .getTable("biobanks")
            .select(
                "name",
                "contact_person/full_name",
                "principal_investigators/full_name",
                "juristic_person/name")
            .search("GrONingen")
            .retrieve()) {
      System.out.println(r.getString("name"));
    }

    System.out.println("search groningen");
    for (Row r : schema.getTable("biobanks").search("groningen").retrieve()) {
      System.out.println(r.getString("name"));
    }

    System.out.println("search rotterdam");
    for (Row r : schema.getTable("biobanks").search("rotterdam").retrieve()) {
      System.out.println(r.getString("name"));
    }
  }
}
