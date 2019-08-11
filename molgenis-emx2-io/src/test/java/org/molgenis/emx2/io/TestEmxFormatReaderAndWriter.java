package org.molgenis.emx2.io;

import org.junit.Test;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.beans.SchemaMetadata;
import org.molgenis.emx2.examples.CompareTools;
import org.molgenis.emx2.io.emx2format.ConvertEmx2ToSchema;
import org.molgenis.utils.StopWatch;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static junit.framework.TestCase.fail;
import static org.molgenis.emx2.io.emx2format.ConvertSchemaToEmx2.toCsv;

public class TestEmxFormatReaderAndWriter {

  @Test
  public void test1() throws IOException, MolgenisException {
    try {
      StopWatch.start("\nfromReader model from test1.txt:\n");

      Schema model1 = new SchemaMetadata("model1");
      ConvertEmx2ToSchema.fromCsvFile(model1, getFile("test1.txt"));
      System.out.println(model1.toString());

      StopWatch.print("\nwrite model back to csv:\n");
      StringWriter writer = new StringWriter();
      toCsv(model1, writer);
      String csv = writer.toString();
      System.out.println(csv);

      StopWatch.print("\nroundtrip fromReader model from this csv\n");
      Schema model2 = new SchemaMetadata("model1");
      ConvertEmx2ToSchema.fromReader(model2, new StringReader(csv));
      System.out.println(model1.toString());

      // assertEquals
      StopWatch.print("\nassertEquals\n");

      CompareTools.assertEquals(model1, model2);

      StopWatch.print("Roundtrip test success: comparison returned 'equal'");

    } catch (MolgenisException e) {
      System.out.println(e.getMessages());
      throw new RuntimeException(e);
    }
  }

  private File getFile(String name) {
    String file = ClassLoader.getSystemResource(name).getFile();
    return new File(file);
  }
}
