package org.molgenis.emx2.io;

import static org.junit.Assert.assertEquals;

import java.io.File;
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
  public void importTest() {
    StopWatch.print("begin");

    loadSchema("CohortsCentral.xlsx", centralSchema);
    assertEquals(30, TestCohortCatalogueMultipleSchemas.centralSchema.getTableNames().size());

    loadSchema("CatalogueOntologies.xlsx", catalogueOntologies);
    assertEquals(13, TestCohortCatalogueMultipleSchemas.catalogueOntologies.getTableNames().size());

    loadSchema("CatalogueDescriptions.xlsx", catalogueDescriptions);
    assertEquals(
        10, TestCohortCatalogueMultipleSchemas.catalogueDescriptions.getTableNames().size());

    loadSchema("CatalogueSourceVariables.xlsx", catalogueSourceVariables);
    assertEquals(
        7, TestCohortCatalogueMultipleSchemas.catalogueSourceVariables.getTableNames().size());

    loadSchema("CatalogueHarmonizedVariables.xlsx", catalogueHarmonizedVariables);
    assertEquals(
        7, TestCohortCatalogueMultipleSchemas.catalogueHarmonizedVariables.getTableNames().size());

    loadSchema("CatalogueMappings.xlsx", catalogueMappings);
    assertEquals(2, TestCohortCatalogueMultipleSchemas.catalogueMappings.getTableNames().size());
  }

  private void loadSchema(String fileName, Schema schema) {
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
