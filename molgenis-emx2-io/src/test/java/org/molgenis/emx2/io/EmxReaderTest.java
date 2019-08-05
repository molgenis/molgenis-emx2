package org.molgenis.emx2.io;

import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.Test;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.beans.SchemaBean;
import org.molgenis.emx2.io.format.MolgenisFileRow;
import org.molgenis.utils.StopWatch;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static junit.framework.TestCase.fail;
import static org.molgenis.emx2.io.MolgenisMetadataFileWriter.convertModelToMolgenisFileRows;
import static org.molgenis.emx2.io.MolgenisMetadataFileWriter.writeCsv;

public class EmxReaderTest {

  @Test
  public void test1() throws IOException, MolgenisException {
    try {
      StopWatch.start("\nmodel read from test1.txt:");
      Schema model1 = new SchemaBean("model1");
      MolgenisMetadataFileReader.load(model1, getFile("test1.txt"));
      // System.out.println(model1.toString());

      StopWatch.print("\nmodel converted back to lines:");
      for (MolgenisFileRow row : convertModelToMolgenisFileRows(model1)) {
        // System.out.println(row);
      }

      StopWatch.print("\nmodel printed back to csv");
      StringWriter writer = new StringWriter();
      writeCsv(model1, writer);
      String csv = writer.toString();
      // System.out.println(csv);

      StopWatch.print("\nroundtrip readBuffered model from this csv");
      Schema model2 = new SchemaBean("model1");
      MolgenisMetadataFileReader.load(model2, new StringReader(csv));
      // System.out.println(model1.print());

      // compare
      Javers javers = JaversBuilder.javers().build();
      Diff diff = javers.compare(model1, model2);
      if (diff.hasChanges()) {
        fail("Roundtrip test failed: changes, " + diff.toString());
      }
      StopWatch.print("comparison returned 'equal'");

    } catch (MolgenisException e) {
      e.printMessages();
      throw new RuntimeException(e);
    }
  }

  private File getFile(String name) {
    String file = ClassLoader.getSystemResource(name).getFile();
    return new File(file);
  }
}
