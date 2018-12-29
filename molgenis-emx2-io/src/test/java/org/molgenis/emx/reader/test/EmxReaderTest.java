package org.molgenis.emx.reader.test;

import org.junit.Test;
import org.molgenis.emx2.EmxModel;
import org.molgenis.emx2.io.MolgenisReader;
import org.molgenis.emx2.io.MolgenisWriter;
import org.molgenis.emx2.io.MolgenisWriterException;
import org.molgenis.emx2.io.format.MolgenisFileRow;

import java.io.File;
import java.io.IOException;

public class EmxReaderTest {

  @Test
  public void test1() throws IOException, MolgenisWriterException {

    for (MolgenisFileRow row : new MolgenisReader().readRowsFromCsv(getFile("test1.txt"))) {
      System.out.println(row);
    }

    EmxModel model = new MolgenisReader().readModelFromCsv(getFile("test1.txt"));
    System.out.println(model);

    System.out.println("now back to rows:");
    for (MolgenisFileRow row : new MolgenisWriter().convertModelToMolgenisFileRows(model)) {
      System.out.println(row);
    }
  }

  private File getFile(String name) {
    String file = ClassLoader.getSystemResource(name).getFile();
    return new File(file);
  }
}
