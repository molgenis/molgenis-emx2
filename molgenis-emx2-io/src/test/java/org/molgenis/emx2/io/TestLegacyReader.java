package org.molgenis.emx2.io;

import org.javers.core.diff.Diff;
import org.junit.Test;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.MolgenisExceptionMessage;
import org.molgenis.emx2.examples.CompareTools;
import org.molgenis.emx2.io.legacyformat.AttributesFileReader;
import org.molgenis.emx2.io.legacyformat.AttributesFileRow;

import java.io.*;
import java.util.Collection;

import static junit.framework.TestCase.fail;
import static org.molgenis.emx2.io.MolgenisMetadataFileWriter.writeCsv;

public class TestLegacyReader {

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
      Schema schema =
          new AttributesFileReader().readModelFromCsv(getFile("attributes_typetest.csv"));

      StringWriter writer = new StringWriter();
      writeCsv(schema, writer);
      System.out.println(writer);

      // load it again
      Schema schema2 =
          new AttributesFileReader().readModelFromCsv(new StringReader(writer.toString()));

      // compare
      try {
        CompareTools.compare(schema, schema2);
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
