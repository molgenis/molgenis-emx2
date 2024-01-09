package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.datamodels.test.ArrayTypeTestExample;
import org.molgenis.emx2.datamodels.test.ProductComponentPartsExample;
import org.molgenis.emx2.datamodels.test.RefAndRefArrayTestExample;
import org.molgenis.emx2.datamodels.test.SimpleTypeTestExample;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@Tag("slow")
public class TestImportExportAllExamples {

  static Database db;

  @BeforeAll
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testArrayTypeTestExample() throws IOException {
    SchemaMetadata schema1 = new SchemaMetadata("1");
    ArrayTypeTestExample.createSimpleTypeTest(schema1);
    executeCompare(schema1);
  }

  @Test
  public void testRefAndRefArrayExample() throws IOException {
    SchemaMetadata schema1 = new SchemaMetadata("4");
    RefAndRefArrayTestExample.createRefAndRefArrayTestExample(schema1);
    executeCompare(schema1);
  }

  @Test
  public void testSimpleTypeTestExample() throws IOException {
    SchemaMetadata schema1 = new SchemaMetadata("5");
    SimpleTypeTestExample.createSimpleTypeTest(schema1);
    executeCompare(schema1);
  }

  @Test
  public void testProductComponentPartsExample() throws IOException {
    SchemaMetadata schema1 = new SchemaMetadata("6");
    ProductComponentPartsExample.create(schema1);
    executeCompare(schema1);
  }

  @Test
  public void testPetStoreExample() throws IOException {
    SchemaMetadata schema1 = new SchemaMetadata("7");
    schema1.create(
        new PetStoreLoader().getSchemaMetadata().getTables().toArray(new TableMetadata[0]));
    executeCompare(schema1);
  }

  @Test
  public void testDefaultValuesMetadata() throws IOException {
    SchemaMetadata schema1 = new SchemaMetadata("8");
    schema1.create(table("test", column("id").setDefaultValue("bla")));
    Schema result = executeCompare(schema1);
    assertEquals(
        "bla", result.getMetadata().getTableMetadata("test").getColumn("id").getDefaultValue());
  }

  public Schema executeCompare(SchemaMetadata schema1) throws IOException, MolgenisException {
    try {
      // now write it out and fromReader back and compare
      List<Row> contents = Emx2.toRowList(schema1);
      for (Row r : contents) {
        System.out.println(r);
      }

      SchemaMetadata schema2 = Emx2.fromRowList(contents);

      CompareTools.assertEquals(schema1, schema2);

      Schema schema3 = db.dropCreateSchema(getClass().getSimpleName() + schema1.getName());
      schema3.migrate(schema2);
      return schema3;

    } catch (MolgenisException e) {
      System.out.println(e.getDetails());
      throw e;
    }
  }
}
