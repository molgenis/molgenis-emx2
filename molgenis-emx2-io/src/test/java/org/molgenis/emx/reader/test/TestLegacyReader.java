package org.molgenis.emx.reader.test;

import org.junit.Test;
import org.molgenis.DatabaseException;
import org.molgenis.Schema;
import org.molgenis.emx2.io.MolgenisReaderException;
import org.molgenis.emx2.io.MolgenisReaderMessage;
import org.molgenis.emx2.io.MolgenisWriter;
import org.molgenis.emx2.io.MolgenisWriterException;
import org.molgenis.emx2.io.legacyformat.AttributesFileReader;
import org.molgenis.emx2.io.legacyformat.AttributesFileRow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;

public class TestLegacyReader {

  @Test
  public void test() throws MolgenisWriterException {
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
      new MolgenisWriter().writeCsv(model, writer);
      System.out.println(writer);

    } catch (MolgenisReaderException e) {
      for (MolgenisReaderMessage m : e.getMessages()) {
        System.out.println(m);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (DatabaseException e) {
      e.printStackTrace();
    }
  }

  private File getFile(String name) {
    String file = ClassLoader.getSystemResource(name).getFile();
    return new File(file);
  }
}
