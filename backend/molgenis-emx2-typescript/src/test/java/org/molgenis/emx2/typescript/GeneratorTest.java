package org.molgenis.emx2.typescript;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.datamodels.test.ProductComponentPartsExample;
import org.molgenis.emx2.datamodels.test.SimpleTypeTestExample;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class GeneratorTest {

  private static Database db;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  void generateTypes() throws IOException {

    Schema schema = db.dropCreateSchema(GeneratorTest.class.getSimpleName() + "-PetStore");
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);

    DataModels.Profile.PET_STORE.getImportTask(schema, false).run();
    new Generator().generate(schema, printWriter, false);

    // now compare generated with expected
    String generated = stringWriter.toString();
    String expected =
        fileToString(this.getClass().getClassLoader().getResource("expected-types.ts").getFile());

    assertEquals(expected, generated);
  }

  @Test
  void generateTypeTest() throws IOException {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    String schemaName = GeneratorTest.class.getSimpleName() + "TypeTest";

    Schema schema = db.getSchema(schemaName);
    if (schema == null) {
      schema = db.createSchema(schemaName);
    } else {
      schema = db.dropCreateSchema(schema.getName());
    }

    SimpleTypeTestExample.createSimpleTypeTest(schema.getMetadata());
    ProductComponentPartsExample.create(schema.getMetadata());

    new Generator().generate(schema, printWriter, false);

    // now compare generated with expected
    String expected =
        fileToString(
            this.getClass().getClassLoader().getResource("expected-typetest-types.ts").getFile());

    String generated = stringWriter.toString();

    assertEquals(expected, generated);
  }

  private String fileToString(String file) throws IOException {
    return Files.readString(Path.of(file));
  }
}
