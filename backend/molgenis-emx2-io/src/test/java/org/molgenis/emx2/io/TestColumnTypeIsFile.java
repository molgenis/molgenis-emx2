package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvFile;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@Tag("slow")
public class TestColumnTypeIsFile {

  private static final String SCHEMA_NAME = "TestColumnTypeIsFile";
  static Database database;
  static Schema schema;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(SCHEMA_NAME);
    new PetStoreLoader().load(schema, false);

    schema
        .getTable("User")
        .insert(
            row(
                "username",
                "bob",
                "picture",
                new BinaryFileWrapper(
                    "text/plain", "test.txt", "test".getBytes(Charset.defaultCharset()))));
  }

  @Test
  public void testExcelShouldNotIncludeFileColumns() throws IOException {
    Path tmp = Files.createTempDirectory(null);
    Path excelFile = tmp.resolve("test.xlsx");
    MolgenisIO.toExcelFile(excelFile, schema, false);
    TableStore store = new TableStoreForXlsxFile(excelFile);
    assertFalse(store.readTable("User").iterator().next().getColumnNames().contains("picture"));
  }

  @Test
  public void testCsvShouldNotIncludeFileColumns() throws IOException {
    Path tmp = Files.createTempDirectory(null);
    Path csvFile = tmp.resolve("User.csv");
    MolgenisIO.toExcelFile(csvFile, schema.getTable("User"), false);
    TableStore store = new TableStoreForCsvFile(csvFile);
    assertFalse(store.readTable("User").iterator().next().getColumnNames().contains("picture"));
  }

  @Test
  public void testCsvWithSystemColumnsShouldIncludeFileColumns() throws IOException {
    Path tmp = Files.createTempDirectory(null);
    Path csvFile = tmp.resolve("User.csv");
    MolgenisIO.toExcelFile(csvFile, schema.getTable("User"), true);
    TableStore store = new TableStoreForCsvFile(csvFile);
    assertFalse(store.readTable("User").iterator().next().getColumnNames().contains("mg_draft"));
  }

  @Test
  public void testCanExportImportFilesInZip() throws IOException {
    Path tmp = Files.createTempDirectory(null);
    Path zipFile = tmp.resolve("test.zip");
    MolgenisIO.toZipFile(zipFile, schema, false);

    schema = database.dropCreateSchema(SCHEMA_NAME);
    MolgenisIO.fromZipFile(zipFile, schema, true);

    Table userTable = schema.getTable("User");
    assertEquals(
        "test",
        new String(
            userTable
                .query()
                .select(s("picture", s("contents"), s("mimetype"), s("extension")))
                .retrieveRows()
                .get(0)
                .getBinary("picture_contents")));
  }

  @Test
  public void testCanExportImportFilesInDirectory() throws IOException {
    Path tmp = Files.createTempDirectory(null);
    Path directory = tmp.resolve("test");
    Files.createDirectories(directory);

    MolgenisIO.toDirectory(directory, schema, false);

    schema = database.dropCreateSchema(SCHEMA_NAME);
    MolgenisIO.fromDirectory(directory, schema, true);

    Table userTable = schema.getTable("User");
    Row result =
        userTable
            .query()
            .select(s("picture", s("contents"), s("mimetype"), s("extension")))
            .retrieveRows()
            .get(0);
    assertEquals("test", new String(result.getBinary("picture_contents")));
    assertEquals("txt", result.getString("picture_extension"));
    assertEquals("text/plain", result.getString("picture_mimetype"));
  }
}
