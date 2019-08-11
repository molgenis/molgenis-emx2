package org.molgenis.emx2.io;

import org.junit.Test;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.beans.SchemaMetadata;
import org.molgenis.emx2.examples.CompareTools;
import org.molgenis.emx2.examples.ProductComponentPartsExample;
import org.molgenis.emx2.examples.synthetic.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class TestRoundTripModels {

  @Test
  public void testArrayTypeTestExample() throws MolgenisException, IOException {
    Schema schema1 = new SchemaMetadata("test");
    ArrayTypeTestExample.createSimpleTypeTest(schema1);
    executeCompare(schema1);
  }

  @Test
  public void testCompositePrimaryKeyExample() throws MolgenisException, IOException {
    Schema schema1 = new SchemaMetadata("test");
    CompositePrimaryKeyExample.createCompositePrimaryKeyExample(schema1);
    executeCompare(schema1);
  }

  @Test
  public void testCompositeRefExample() throws MolgenisException, IOException {
    Schema schema1 = new SchemaMetadata("test");
    CompositeRefExample.createCompositeRefExample(schema1);
    executeCompare(schema1);
  }

  @Test
  public void testRefAndRefArrayExample() throws MolgenisException, IOException {
    Schema schema1 = new SchemaMetadata("test");
    RefAndRefArrayTestExample.createRefAndRefArrayTestExample(schema1);
    executeCompare(schema1);
  }

  @Test
  public void testSimpleTypeTestExample() throws MolgenisException, IOException {
    Schema schema1 = new SchemaMetadata("test");
    SimpleTypeTestExample.createSimpleTypeTest(schema1);
    executeCompare(schema1);
  }

  @Test
  public void tesProductComponentPartsExample() throws MolgenisException, IOException {
    Schema schema1 = new SchemaMetadata("test");
    ProductComponentPartsExample.create(schema1);
    executeCompare(schema1);
  }

  public void executeCompare(Schema schema1) throws IOException, MolgenisException {
    try {
      // now write it out and read back and compare
      StringWriter writer = new StringWriter();
      Emx2FileWriter.writeCsv(schema1, writer);

      System.out.println(writer.toString());

      Schema schema2 = new SchemaMetadata("test");
      Emx2FileReader.load(schema2, new StringReader(writer.toString()));

      CompareTools.assertEquals(schema1, schema2);
    } catch (MolgenisException e) {
      System.out.println(e.getMessages());
      throw e;
    }
  }
}
