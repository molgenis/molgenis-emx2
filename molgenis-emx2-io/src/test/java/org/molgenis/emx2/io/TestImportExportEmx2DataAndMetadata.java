package org.molgenis.emx2.io;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.CompareTools;
import org.molgenis.emx2.examples.ProductComponentPartsExample;
import org.molgenis.emx2.sql.DatabaseFactory;
import org.molgenis.emx2.utils.StopWatch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class TestImportExportEmx2DataAndMetadata {

  static Database database;

  @BeforeClass
  public static void setup() throws MolgenisException {
    database = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void test() throws MolgenisException, IOException {

    Path tmp = Files.createTempDirectory(null);
    try {
      StopWatch.start("export and then import again test");

      Path directory = tmp.resolve("test");
      Files.createDirectories(directory);
      Schema schema1 = database.createSchema(getClass().getSimpleName() + "1");

      StopWatch.print("schema1 created, ready to load the example data");

      ProductComponentPartsExample.create(schema1.getMetadata());
      ProductComponentPartsExample.populate(schema1);

      StopWatch.print("example schema loaded");

      MolgenisExport.toDirectory(directory, schema1);

      StopWatch.print("export complete");

      Schema schema2 = database.createSchema(getClass().getSimpleName() + "2");

      StopWatch.print("schema2 created, ready to reload the exported data");

      MolgenisImport.fromDirectory(directory, schema2);
      StopWatch.print("import complete");

      CompareTools.assertEquals(schema1.getMetadata(), schema2.getMetadata());

      StopWatch.print("schema comparison: equal");

    } finally {
      Files.walk(tmp).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
  }
}
