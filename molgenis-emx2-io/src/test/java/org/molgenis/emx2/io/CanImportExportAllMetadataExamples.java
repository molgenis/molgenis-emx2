package org.molgenis.emx2.io;

import org.junit.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.examples.CompareTools;
import org.molgenis.emx2.examples.ProductComponentPartsExample;
import org.molgenis.emx2.io.emx2format.ConvertEmx2ToSchema;
import org.molgenis.emx2.io.emx2format.ConvertSchemaToEmx2;
import org.molgenis.emx2.examples.synthetic.*;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.Row;

import java.io.IOException;
import java.util.List;

public class CanImportExportAllMetadataExamples {

  @Test
  public void testArrayTypeTestExample() throws MolgenisException, IOException {
    SchemaMetadata schema1 = new SchemaMetadata("test");
    ArrayTypeTestExample.createSimpleTypeTest(schema1);
    executeCompare(schema1);
  }

  @Test
  public void testCompositePrimaryKeyExample() throws MolgenisException, IOException {
    SchemaMetadata schema1 = new SchemaMetadata("test");
    CompositePrimaryKeyExample.createCompositePrimaryKeyExample(schema1);
    executeCompare(schema1);
  }

  @Test
  public void testCompositeRefExample() throws MolgenisException, IOException {
    SchemaMetadata schema1 = new SchemaMetadata("test");
    CompositeRefExample.createCompositeRefExample(schema1);
    executeCompare(schema1);
  }

  @Test
  public void testRefAndRefArrayExample() throws MolgenisException, IOException {
    SchemaMetadata schema1 = new SchemaMetadata("test");
    RefAndRefArrayTestExample.createRefAndRefArrayTestExample(schema1);
    executeCompare(schema1);
  }

  @Test
  public void testSimpleTypeTestExample() throws MolgenisException, IOException {
    SchemaMetadata schema1 = new SchemaMetadata("test");
    SimpleTypeTestExample.createSimpleTypeTest(schema1);
    executeCompare(schema1);
  }

  @Test
  public void tesProductComponentPartsExample() throws MolgenisException, IOException {
    SchemaMetadata schema1 = new SchemaMetadata("test");
    ProductComponentPartsExample.create(schema1);
    executeCompare(schema1);
  }

  public void executeCompare(SchemaMetadata schema1) throws IOException, MolgenisException {
    try {
      // now write it out and fromReader back and compare

      List<Row> contents = ConvertSchemaToEmx2.toRowList(schema1);
      for (Row r : contents) {
        System.out.println(r);
      }

      SchemaMetadata schema2 = new SchemaMetadata("test");
      ConvertEmx2ToSchema.fromRowList(schema2, contents);

      CompareTools.assertEquals(schema1, schema2);

    } catch (MolgenisException e) {
      System.out.println(e.getMessages());
      throw e;
    }
  }
}
