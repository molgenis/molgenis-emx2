package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

/**
 * Guards ticket-21's promise that a yaml template is creatable end-to-end by the name {@code
 * /api/templates} returns, through the very seam the {@code createSchema} mutation uses. The
 * demo-data flag is threaded (false loads only {@code data:}, not {@code demo:}); the {@code
 * demo:}-when-requested direction is guarded at loader level in {@link YamlWorkspaceLoaderTest}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataModelsYamlTemplateTest {

  private static final String CATALOGUE_TEMPLATE = "catalogue";
  private static final String COLLECTIONS = "Collections";
  private static final String ORGANISATIONS = "Organisations";
  private static final String SCHEMA_NO_DEMO = "dmYamlCatalogueNoDemo";

  private static Database database;

  @BeforeAll
  void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NO_DEMO);
  }

  @AfterAll
  void cleanup() {
    database.dropSchemaIfExists(SCHEMA_NO_DEMO);
  }

  @Test
  void yamlTemplateSeamRoutesAndHonorsDemoFlag() {
    DataModels.getImportTask(database, SCHEMA_NO_DEMO, "", CATALOGUE_TEMPLATE, false).run();
    Schema schema = database.getSchema(SCHEMA_NO_DEMO);

    assertNotNull(
        schema.getTable(COLLECTIONS),
        "yaml template must be creatable via the createSchema seam by its /api/templates name");
    assertEquals(
        2,
        schema.getTable(ORGANISATIONS).retrieveRows().size(),
        "data: reference rows must load through the seam");
    assertTrue(
        schema.getTable(COLLECTIONS).retrieveRows().isEmpty(),
        "includeDemoData=false must not load demo: rows through the seam");
  }

  @Test
  void unknownTemplateNameStillThrows() {
    assertThrows(
        MolgenisException.class,
        () -> DataModels.getImportTask(database, "dmUnknownTemplate", "", "does-not-exist", false));
  }
}
