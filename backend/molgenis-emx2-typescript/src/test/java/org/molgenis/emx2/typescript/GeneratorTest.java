package org.molgenis.emx2.typescript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.datamodels.test.ProductComponentPartsExample;
import org.molgenis.emx2.datamodels.test.SimpleTypeTestExample;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GeneratorTest {

  private static Database db;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  @Order(1)
  void generateTypes() throws IOException {
    String schemaName = GeneratorTest.class.getSimpleName() + "-PetStore";
    db.dropSchemaIfExists(schemaName);
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);

    DataModels.Profile.PET_STORE.getImportTask(db, schemaName, "", false).run();
    Schema schema = db.getSchema(schemaName);
    new Generator().generate(schema, printWriter, false);

    // now compare generated with expected
    String generated = stringWriter.toString();
    String expected =
        fileToString(this.getClass().getClassLoader().getResource("expected-types.ts").getFile());

    assertEquals(expected, generated);
  }

  @Test
  @Order(2)
  void generateTypeTest() throws IOException {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    String schemaName = GeneratorTest.class.getSimpleName() + "TypeTest";
    final Schema schema = db.dropCreateSchema(schemaName);

    SimpleTypeTestExample.createSimpleTypeTest(schema.getMetadata());
    ProductComponentPartsExample.create(schema.getMetadata());

    new Generator().generate(schema, printWriter, false);

    // now compare generated with expected
    String expected =
        fileToString(
            this.getClass().getClassLoader().getResource("expected-typetest-types.ts").getFile());

    String generated = stringWriter.toString();

    assertEquals(expected, generated);

    // to make sonar happy also check the file generate
    File tempFile = File.createTempFile("myTempFile", ".tmp");
    tempFile.deleteOnExit();
    new Generator().generate(schema, tempFile.getAbsolutePath());
  }

  @Test
  @Order(3)
  void generateCrossSchemaTest() throws IOException {
    String schemaName = GeneratorTest.class.getSimpleName() + "TypeTest";
    final Schema schema = db.getSchema(schemaName);
    schema.create(
        table("CrossSchemaRef")
            .add(
                column("id").setPkey(),
                column("ref")
                    .setType(ColumnType.REF)
                    .setRefSchemaName(GeneratorTest.class.getSimpleName() + "-PetStore")
                    .setRefTable("Category"),
                column("ref_arr")
                    .setType(ColumnType.REF_ARRAY)
                    .setRefSchemaName(GeneratorTest.class.getSimpleName() + "-PetStore")
                    .setRefTable("Category")));

    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    new Generator().generate(schema, printWriter, false);

    assertTrue(stringWriter.toString().contains("PetStore_"));
  }

  private String fileToString(String file) throws IOException {
    return Files.readString(Path.of(file));
  }
}
