package org.molgenis.emx2.io;

import org.junit.Test;
import org.molgenis.emx2.examples.CompareTools;
import org.molgenis.emx2.io.legacyformat.AttributesFileReader;
import org.molgenis.emx2.io.emx2format.ConvertSchemaToEmx2;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.utils.MolgenisExceptionMessage;
import org.molgenis.emx2.io.legacyformat.AttributesFileRow;
import org.molgenis.emx2.SchemaMetadata;

import java.io.*;

import static junit.framework.TestCase.fail;

public class CanReadWriteLegacyEMXformat {

  @Test
  public void test() throws MolgenisException {
    try {
      for (AttributesFileRow row :
          new AttributesFileReader().readRowsFromCsv(getFile("attributes_typetest.csv"))) {
        System.out.println(row);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      SchemaMetadata schema =
          new AttributesFileReader().readModelFromCsv(getFile("attributes_typetest.csv"));

      StringWriter writer = new StringWriter();
      ConvertSchemaToEmx2.toCsv(schema, writer);
      System.out.println(writer);

      // fromReader it again
      SchemaMetadata schema2 =
          new AttributesFileReader().readModelFromCsv(new StringReader(writer.toString()));

      // assertEquals
      try {
        CompareTools.assertEquals(schema, schema2);
      } catch (Exception e) {
        fail(e.getMessage());
      }

    } catch (MolgenisException e) {
      for (MolgenisExceptionMessage m : e.getMessages()) {
        System.out.println(m);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private File getFile(String name) {
    String file = ClassLoader.getSystemResource(name).getFile();
    return new File(file);
  }
}
