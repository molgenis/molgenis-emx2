package org.molgenis.emx2.io;

import java.io.IOException;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.examples.CompareTools;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.examples.ProductComponentPartsExample;
import org.molgenis.emx2.examples.synthetic.ArrayTypeTestExample;
import org.molgenis.emx2.examples.synthetic.RefAndRefArrayTestExample;
import org.molgenis.emx2.examples.synthetic.SimpleTypeTestExample;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestImportExportAllExamples {

  static Database db;

  @BeforeClass
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
  public void testPetStore() throws IOException {
    SchemaMetadata schema1 = new SchemaMetadata("7");
    PetStoreExample.create(schema1);
    executeCompare(schema1);
  }

  public void executeCompare(SchemaMetadata schema1) throws IOException, MolgenisException {
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

    } catch (MolgenisException e) {
      System.out.println(e.getDetails());
      throw e;
    }
  }
}
