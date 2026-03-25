package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Operator.IS_NULL;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.rdf.RdfImportTask;
import org.molgenis.emx2.rdf.RdfSchemaService;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskStatus;

@Tag("slow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RdfRoundtripTest {

  private static final String SOURCE_SCHEMA = "rdfRoundtripSource";
  private static final String TARGET_SCHEMA = "rdfRoundtripTarget";

  private Database database;
  private Schema source;

  @BeforeAll
  void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(TARGET_SCHEMA);
    database.dropSchemaIfExists(SOURCE_SCHEMA);

    DataModels.Profile.DATA_CATALOGUE.getImportTask(database, SOURCE_SCHEMA, "test", true).run();
    source = database.getSchema(SOURCE_SCHEMA);
  }

  @Test
  void roundtripExportThenImport() throws Exception {
    ByteArrayOutputStream ttlOutput = new ByteArrayOutputStream();
    try (RdfSchemaService rdfService =
        new RdfSchemaService("http://localhost:8080", source, RDFFormat.TURTLE, ttlOutput)) {
      rdfService.getGenerator().generate(source);
    }
    assertTrue(ttlOutput.size() > 0, "TTL export should produce output");

    database.dropSchemaIfExists(TARGET_SCHEMA);
    DataModels.Profile.DATA_CATALOGUE.getImportTask(database, TARGET_SCHEMA, "test", false).run();
    Schema target = database.getSchema(TARGET_SCHEMA);

    // Count source resources that have an rdf:type (computed column is non-null)
    // Count exportable resources by rdf:type (computed column)
    long sourceCatalogs =
        source
            .getTable("Resources")
            .query()
            .where(f("rdf type", EQUALS, "http://www.w3.org/ns/dcat#Catalog"))
            .retrieveRows()
            .size();
    long sourceExportable =
        source
            .getTable("Resources")
            .query()
            .where(f("rdf type", IS_NULL, false))
            .retrieveRows()
            .size();
    long sourceDatasets = sourceExportable - sourceCatalogs;
    assertTrue(sourceExportable > 0, "Source should have exportable resources");

    // Import
    byte[] ttlBytes = ttlOutput.toByteArray();
    RdfImportTask importTask =
        new RdfImportTask(target, new ByteArrayInputStream(ttlBytes), ".ttl");
    importTask.run();

    assertEquals(
        TaskStatus.COMPLETED,
        importTask.getStatus(),
        "Import failed: " + importTask.getDescription());

    // Verify Resources: total count, catalog vs dataset split, and names
    long targetResources =
        target
            .getTable("Resources")
            .query()
            .where(f("rdf type", IS_NULL, false))
            .retrieveRows()
            .size();
    long targetCatalogs =
        target
            .getTable("Resources")
            .query()
            .where(f("rdf type", EQUALS, "http://www.w3.org/ns/dcat#Catalog"))
            .retrieveRows()
            .size();
    long targetDatasets = targetResources - targetCatalogs;

    assertEquals(sourceExportable, targetResources, "Resource count should match after roundtrip");
    assertEquals(sourceCatalogs, targetCatalogs, "Catalog count should match");
    assertEquals(sourceDatasets, targetDatasets, "Dataset count should match");
    assertTrue(
        target.getTable("Resources").retrieveRows().stream()
            .anyMatch(r -> r.getString("name") != null),
        "Imported resources should have names");

    // Note: Organisations/Contacts have composite pkeys (resource+id) scoped to Resources,
    // which the simple importer can't resolve yet — a known v2 improvement.
  }

  @AfterAll
  void tearDown() {
    if (database != null) {
      database.dropSchemaIfExists(TARGET_SCHEMA);
      database.dropSchemaIfExists(SOURCE_SCHEMA);
    }
  }
}
