package org.molgenis.emx2.io;

import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Constants.ANONYMOUS;
import static org.molgenis.emx2.Privileges.AGGREGATOR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.test.ProductComponentPartsExample;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.StopWatch;

@Tag("slow")
class TestImportExportEmx2DataAndMetadata {

  static Database database;
  private Path tmp;

  @BeforeAll
  static void setupDatabase() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @BeforeEach
  void setup() throws IOException {
    tmp = Files.createTempDirectory(null);
    database.becomeAdmin();
  }

  @AfterEach
  void cleanUpTemporaryDirectory() throws IOException {
    Files.walk(tmp).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
  }

  @Test
  void shouldImportAndExportInDifferentFormats() throws IOException {
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
    Schema schema3 = database.dropCreateSchema(getClass().getSimpleName() + "2");
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
  }

  @Nested
  class ZipExportTest {

    @Test
    void whenIncludeMembersSpecified_thenIncludeMembersInCSV() {
      Schema schema = setupSchema();
      Path path = tmp.resolve("with-members.zip");
      MolgenisIO.toZipFile(path, schema, false);

      assertTrue(zipContainsMembers(path));
    }

    @Test
    void whenIncludeMembersNotSpecified_thenExcludeMembersInCSV() {
      Schema schema = setupSchema();
      database.clearActiveUser();
      Path path = tmp.resolve("without-members.zip");
      MolgenisIO.toZipFile(path, schema, false);

      assertFalse(zipContainsMembers(path));
    }

    private Schema setupSchema() {
      Schema schema = database.dropCreateSchema(getClass().getSimpleName() + "-members");
      schema.addMember("test-user", Privileges.OWNER.toString());
      return schema;
    }

    private boolean zipContainsMembers(Path path) {
      try (ZipFile zipFile = new ZipFile(path.toFile())) {
        List<String> csvFileNames =
            zipFile.stream().map(ZipEntry::getName).filter(name -> name.endsWith(".csv")).toList();

        return csvFileNames.contains("molgenis_members.csv");
      } catch (IOException e) {
        throw new AssertionError("Unable to read zip file", e);
      }
    }
  }

  @Nested
  class ExcelExportTest {

    private Schema setupSchema() {
      Schema schema = database.dropCreateSchema(getClass().getSimpleName() + "-members");
      schema.addMember("test-user", Privileges.OWNER.toString());
      return schema;
    }

    @Test
    void whenIncludeMembersNotSpecified_thenExcludeMembersInExcel() throws IOException {
      Path excelFile = tmp.resolve("test.xlsx");
      Schema schema = setupSchema();

      database.clearActiveUser();
      MolgenisIO.toExcelFile(excelFile, schema, false);
      assertFalse(excelFileContainsMembersSheet(excelFile));
    }

    @Test
    void whenIncludeMembersSpecified_thenIncludeMembersInExcel() throws IOException {
      Path excelFile = tmp.resolve("test.xlsx");
      Schema schema = setupSchema();

      MolgenisIO.toExcelFile(excelFile, schema, false);
      assertTrue(excelFileContainsMembersSheet(excelFile));
    }

    private boolean excelFileContainsMembersSheet(Path excelFile) throws IOException {
      try (Workbook workbook = new XSSFWorkbook(excelFile.toFile())) {
        return range(0, workbook.getNumberOfSheets())
            .mapToObj(workbook::getSheetName)
            .anyMatch(name -> name.contains("_members"));
      } catch (InvalidFormatException e) {
        throw new AssertionError("Unable to open excel file from provided path", e);
      }
    }
  }
}
