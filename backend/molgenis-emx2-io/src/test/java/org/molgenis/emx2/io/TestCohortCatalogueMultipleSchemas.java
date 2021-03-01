package org.molgenis.emx2.io;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.StopWatch;

/** representative import file for testing */
public class TestCohortCatalogueMultipleSchemas {

  static Database database;
  static Schema centralSchema;
  static Schema localSchema;
  static Schema catalogueOntologies;
  static Schema catalogueDescriptions;
  static Schema catalogueSourceVariables;
  static Schema catalogueHarmonizedVariables;
  static Schema catalogueMappings;
  ;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    localSchema = database.dropCreateSchema("CohortsLocal");
    centralSchema = database.dropCreateSchema("CohortsCentral");
    catalogueOntologies = database.dropCreateSchema("CatalogueOntologies");
    catalogueDescriptions = database.dropCreateSchema("CatalogueDescriptions");
    catalogueSourceVariables = database.dropCreateSchema("CatalogueSourceVariables");
    catalogueHarmonizedVariables = database.dropCreateSchema("CatalogueHarmonizedVariables");
    catalogueMappings = database.dropCreateSchema("CatalogueMappings");
  }

  @Test
  public void importTest() throws IOException {
    StopWatch.print("begin");

    // in three modules

    loadSchema("CatalogueCentralOntologies.xlsx", centralSchema);
    assertEquals(18, TestCohortCatalogueMultipleSchemas.centralSchema.getTableNames().size());

    loadSchema("CatalogueCentralCollections.xlsx", centralSchema);
    assertEquals(34, TestCohortCatalogueMultipleSchemas.centralSchema.getTableNames().size());

    loadSchema("CatalogueCentralDictionaries.xlsx", centralSchema);
    assertEquals(41, TestCohortCatalogueMultipleSchemas.centralSchema.getTableNames().size());

    // submodules for the 'data'
    loadSchema("CatalogueCentralOntologies.xlsx", catalogueOntologies);
    assertEquals(18, TestCohortCatalogueMultipleSchemas.catalogueOntologies.getTableNames().size());

    loadSchema("CatalogueLocalSourceVariables.xlsx", catalogueSourceVariables);
    assertEquals(
        7, TestCohortCatalogueMultipleSchemas.catalogueSourceVariables.getTableNames().size());

    loadSchema("CatalogueLocalHarmonizedVariables.xlsx", catalogueHarmonizedVariables);
    assertEquals(
        7, TestCohortCatalogueMultipleSchemas.catalogueHarmonizedVariables.getTableNames().size());

    loadSchema("CatalogueLocalMappings.xlsx", catalogueMappings);
    assertEquals(2, TestCohortCatalogueMultipleSchemas.catalogueMappings.getTableNames().size());
  }

  private void loadSchema(String fileName, Schema schema) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    Path file = new File(classLoader.getResource(fileName).getFile()).toPath();

    TableStoreForXlsxFile store = new TableStoreForXlsxFile(file);
    SchemaMetadata source = Emx2.fromRowList(store.readTable("molgenis"));
    source.setDatabase(schema.getDatabase()); // enable cross links to existing data
    System.out.println(source);
    StopWatch.print("schema loaded, now creating tables");

    database.tx(
        db -> {
          schema.migrate(source);
        });

    // don't put alter in same transaction as update
    database.tx(
        db -> {
          runImportProcedure(store, source, schema);
          StopWatch.print("import of data complete");
        });

    // repeat for idempotency test (should not change anything)
    database.tx(
        db -> {
          schema.migrate(source);
        });

    database.tx(
        db -> {
          runImportProcedure(store, source, schema);
          StopWatch.print("import of data complete");
        });

    // verify export doesn't throw exceptions
    Path tempDir =
        Files.createTempDirectory(TestCohortCatalogueMultipleSchemas.class.getSimpleName());
    tempDir.toFile().deleteOnExit();

    Path excelFile = tempDir.resolve("download.xlsx");
    MolgenisIO.toExcelFile(excelFile, schema);

    Path zipFile = tempDir.resolve("download.zip");
    MolgenisIO.toZipFile(zipFile, schema);
  }

  private void runImportProcedure(
      TableStoreForXlsxFile store, SchemaMetadata source, Schema target) {
    StopWatch.print("creation of tables complete, now starting import data");
    for (String tableName : target.getTableNames()) {
      if (store.containsTable(tableName))
        target.getTable(tableName).update(store.readTable(tableName)); // actually upsert
    }
  }
}
