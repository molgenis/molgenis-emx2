package org.molgenis.emx2.typescript;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class GeneratorTest {

  private static Database db;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  void generateTypes() throws IOException {

    File f = new File(this.getClass().getClassLoader().getResource("generateTypes.ts").getFile());
    Schema schema = db.dropCreateSchema(GeneratorTest.class.getSimpleName() + "-PetStore");

    PetStoreLoader petStoreLoader = new PetStoreLoader(schema, false);
    petStoreLoader.run();
    new Generator().generate(schema, f.getPath());

    // now compare generated with expected
    File expected =
        new File(this.getClass().getClassLoader().getResource("expected-types.ts").getFile());
    List<String> lines = fileToLines(expected);

    File generated =
        new File(this.getClass().getClassLoader().getResource("generateTypes.ts").getFile());

    List<String> generatedLines = fileToLines(generated);

    assertEquals(lines, generatedLines);
  }

  @Test
  @Tag("slow")
  void generateCatalogueTypes() throws IOException {
    File f =
        new File(
            this.getClass().getClassLoader().getResource("generated-catalogue-types.ts").getFile());
    String schemaName = GeneratorTest.class.getSimpleName() + "Catalogue";
    Schema schema = db.getSchema(schemaName);
    if (schema == null) {
      schema = db.createSchema(schemaName);
    } else {
      schema = db.dropCreateSchema(schema.getName());
    }

    DataModels.Profile.DATA_CATALOGUE.getImportTask(schema, false).run();
    new Generator().generate(schema, f.getPath());

    // now compare generated with expected
    File expected =
        new File(
            this.getClass().getClassLoader().getResource("expected-catalogue-types.ts").getFile());
    List<String> lines = fileToLines(expected);

    File generated =
        new File(
            this.getClass().getClassLoader().getResource("generated-catalogue-types.ts").getFile());

    List<String> generatedLines = fileToLines(generated);

    assertEquals(lines, generatedLines);
  }

  private List<String> fileToLines(File file) throws FileNotFoundException {
    Scanner scanner = new Scanner(file);
    // skip first line
    scanner.nextLine();
    List<String> lines = new ArrayList<String>();
    while (scanner.hasNextLine()) lines.add(scanner.nextLine().trim());
    scanner.close();
    return lines;
  }
}
