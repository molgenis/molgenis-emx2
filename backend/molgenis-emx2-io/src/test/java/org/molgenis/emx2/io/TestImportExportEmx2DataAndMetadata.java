package org.molgenis.emx2.io;

import static org.molgenis.emx2.Constants.ANONYMOUS;
import static org.molgenis.emx2.Privileges.AGGREGATOR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.test.ProductComponentPartsExample;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.StopWatch;

@Tag("slow")
public class TestImportExportEmx2DataAndMetadata {
  static Database database;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void test() throws IOException {

    Path tmp = Files.createTempDirectory(null);
    try {
      StopWatch.start("export and then import again test");

      Path directory = tmp.resolve("test");
      Files.createDirectories(directory);
      Schema schema1 = database.dropCreateSchema(getClass().getSimpleName() + "1");

      StopWatch.print("schema1 created, ready to load the example data");

      ProductComponentPartsExample.create(schema1.getMetadata());
      ProductComponentPartsExample.populate(schema1);

      StopWatch.print("example schema loaded");
      MolgenisIO.toDirectory(directory, schema1, false);
      StopWatch.print("export to directory complete");
      Schema schema2 = database.dropCreateSchema(getClass().getSimpleName() + "2");
      StopWatch.print("schema2 created, ready to reload the exported data");
      MolgenisIO.fromDirectory(directory, schema2, true);
      StopWatch.print("import from directory complete");
      CompareTools.assertEquals(schema1.getMetadata(), schema2.getMetadata());

      Path excelFile = tmp.resolve("test.xlsx");
      MolgenisIO.toExcelFile(excelFile, schema1, true);
      StopWatch.print("export to excel complete");
      Schema schema3 = database.dropCreateSchema(getClass().getSimpleName() + "3");
      StopWatch.print("schema3 created, ready to reload the exported data");
      MolgenisIO.importFromExcelFile(excelFile, schema3, true);
      StopWatch.print("import from excel complete");
      CompareTools.assertEquals(schema1.getMetadata(), schema3.getMetadata());

      Path zipFile = tmp.resolve("test.zip");
      MolgenisIO.toZipFile(zipFile, schema1, false);
      StopWatch.print("export to zipfile complete");
      Schema schema4 = database.dropCreateSchema(getClass().getSimpleName() + "4");
      StopWatch.print("schema4 created, ready to reload the exported data");
      MolgenisIO.fromZipFile(zipFile, schema4, true);
      StopWatch.print("import from zipfile complete");
      CompareTools.assertEquals(schema1.getMetadata(), schema4.getMetadata());

      StopWatch.print("schema comparison: all equal");

      // test if we can also download as aggregator
      excelFile = tmp.resolve("test2.xlsx");
      schema1.addMember(ANONYMOUS, AGGREGATOR.toString());
      database.setActiveUser(ANONYMOUS);
      schema1 = database.getSchema(getClass().getSimpleName() + "1");
      MolgenisIO.toExcelFile(excelFile, schema1, true);

    } finally {
      Files.walk(tmp).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
  }
}
