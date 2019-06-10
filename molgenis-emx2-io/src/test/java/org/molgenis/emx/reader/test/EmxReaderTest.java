package org.molgenis.emx.reader.test;

import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.Test;
import org.molgenis.DatabaseException;
import org.molgenis.Schema;
import org.molgenis.emx2.io.MolgenisReader;
import org.molgenis.emx2.io.MolgenisWriter;
import org.molgenis.emx2.io.MolgenisWriterException;
import org.molgenis.emx2.io.format.MolgenisFileRow;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static junit.framework.TestCase.fail;

public class EmxReaderTest {

  @Test
  public void test1() throws IOException, MolgenisWriterException, DatabaseException {

    System.out.println("lines parsed from test1.txt:");
    for (MolgenisFileRow row : new MolgenisReader().readRowsFromCsvFile(getFile("test1.txt"))) {
      System.out.println(row);
    }

    System.out.println("\nmodel read from test1.txt:");
    Schema model1 = new MolgenisReader().readModelFromCsvFile(getFile("test1.txt"));
    // System.out.println(model1.print());

    System.out.println("\nmodel converted back to lines:");
    for (MolgenisFileRow row : new MolgenisWriter().convertModelToMolgenisFileRows(model1)) {
      System.out.println(row);
    }

    System.out.println("\nmodel printed back to csv");
    StringWriter writer = new StringWriter();
    new MolgenisWriter().writeCsv(model1, writer);
    String csv = writer.toString();
    System.out.println(csv);

    System.out.println("\nroundtrip read model from this csv");
    Schema model2 = new MolgenisReader().readModelFromCsvReader(new StringReader(csv));
    // System.out.println(model1.print());

    // compare
    Javers javers = JaversBuilder.javers().build();
    Diff diff = javers.compare(model1, model2);
    if (diff.hasChanges()) {
      fail("Roundtrip test failed: changes, " + diff.toString());
    }
  }

  private File getFile(String name) {
    String file = ClassLoader.getSystemResource(name).getFile();
    return new File(file);
  }
}
