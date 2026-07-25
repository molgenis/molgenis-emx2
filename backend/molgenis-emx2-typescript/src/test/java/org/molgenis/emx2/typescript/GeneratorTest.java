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
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.datamodels.test.ProductComponentPartsExample;
import org.molgenis.emx2.datamodels.test.SimpleTypeTestExample;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GeneratorTest {

  private static final String PET_STORE_SCHEMA = GeneratorTest.class.getSimpleName() + "-PetStore";
  private static final String TYPE_TEST_SCHEMA = GeneratorTest.class.getSimpleName() + "TypeTest";
  private static final String MODULE_SCHEMA = GeneratorTest.class.getSimpleName() + "Module";

  private static Database db;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    db.dropSchemaIfExists(TYPE_TEST_SCHEMA);
    db.dropSchemaIfExists(PET_STORE_SCHEMA);
    db.dropSchemaIfExists(MODULE_SCHEMA);
  }

  @Test
  @Order(1)
  void generateTypes() throws IOException {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);

    DataModels.Profile.PET_STORE.getImportTask(db, PET_STORE_SCHEMA, "", false).run();
    Schema schema = db.getSchema(PET_STORE_SCHEMA);
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
    final Schema schema = db.dropCreateSchema(TYPE_TEST_SCHEMA);

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
    final Schema schema = db.getSchema(TYPE_TEST_SCHEMA);
    schema.create(
        table("CrossSchemaRef")
            .add(
                column("id").setPkey(),
                column("ref")
                    .setType(ColumnType.REF)
                    .setRefSchemaName(PET_STORE_SCHEMA)
                    .setRefTable("Category"),
                column("ref_arr")
                    .setType(ColumnType.REF_ARRAY)
                    .setRefSchemaName(PET_STORE_SCHEMA)
                    .setRefTable("Category")));

    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    new Generator().generate(schema, printWriter, false);

    assertTrue(stringWriter.toString().contains("PetStore_"));
  }

  @Test
  @Order(4)
  void generateModuleColumnAsFieldOnRootInterface() {
    Schema schema = db.dropCreateSchema(MODULE_SCHEMA);
    schema.create(table("Subject").add(column("id").setPkey(), column("subject name")));
    schema.create(
        table("Sampling")
            .setTableType(TableType.MODULE)
            .setInheritNames("Subject")
            .add(column("sample count").setType(ColumnType.INT)));

    StringWriter stringWriter = new StringWriter();
    new Generator().generate(schema, new PrintWriter(stringWriter), false);

    assertTrue(
        interfaceDeclaration(stringWriter.toString(), "ISubject").contains("sampleCount?: number;"),
        "root interface must expose the module-contributed column");
  }

  private String interfaceDeclaration(String generated, String interfaceName) {
    int start = generated.indexOf("export interface " + interfaceName + " extends");
    assertTrue(start >= 0, interfaceName + " must be generated");
    return generated.substring(start, generated.indexOf('}', start));
  }

  private String fileToString(String file) throws IOException {
    return Files.readString(Path.of(file));
  }
}
