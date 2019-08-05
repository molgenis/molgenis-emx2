package org.molgenis.emx2.io;

import org.junit.Test;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.MolgenisExceptionMessage;
import org.molgenis.emx2.io.legacyformat.AttributesFileReader;
import org.molgenis.emx2.io.legacyformat.AttributesFileRow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;

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
      Schema model =
          new AttributesFileReader().readModelFromCsv(getFile("attributes_typetest.csv"));

      StringWriter writer = new StringWriter();
      writeCsv(model, writer);
      System.out.println(writer);

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
