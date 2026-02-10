package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.io.ImportOntologiesTask.CSV_CHECKSUM_SETTING;
import static org.molgenis.emx2.io.ImportOntologiesTask.SEMANTICS_CHECKSUM_SETTING;

import org.junit.jupiter.api.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvFilesClasspath;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.Task;
import org.molgenis.emx2.tasks.TaskStatus;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestImportOntologiesTask {

  private static final String ONTOLOGY_LOCATION = "/_ontologies";
  private static final String SEMANTICS_LOCATION = ONTOLOGY_LOCATION + "/_semantics.csv";
  private static final String SCHEMA_NAME = "TestImportOntologiesTask";

  private Database db;
  private Schema schema;

  @BeforeAll
  void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    db.dropSchemaIfExists(SCHEMA_NAME);
    // create schema with PetStore profile to get ontology table definitions
    ImportProfileTask profileTask =
        new ImportProfileTask(db, SCHEMA_NAME, "test", "_profiles/PetStore.yaml", false);
    profileTask.run();
    schema = db.getSchema(SCHEMA_NAME);
  }

  @AfterAll
  void teardown() {
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  @org.junit.jupiter.api.Order(1)
  void firstImportStoresChecksums() {
    // clear any checksums from the profile import
    for (Table table : schema.getTablesSorted()) {
      if (table.getMetadata().getSetting(CSV_CHECKSUM_SETTING) != null) {
        table.getMetadata().setSetting(CSV_CHECKSUM_SETTING, null);
      }
    }
    schema.getMetadata().setSetting(SEMANTICS_CHECKSUM_SETTING, null);

    TableStoreForCsvFilesClasspath store = new TableStoreForCsvFilesClasspath(ONTOLOGY_LOCATION);
    ImportOntologiesTask task =
        new ImportOntologiesTask(schema, store, ONTOLOGY_LOCATION, SEMANTICS_LOCATION);
    task.run();

    // verify no subtasks were skipped (all checksums were cleared)
    for (Task subTask : task.getSubTasks()) {
      assertNotEquals(
          TaskStatus.SKIPPED,
          subTask.getStatus(),
          "No subtask should be skipped on first import: " + subTask.getDescription());
    }

    // verify checksum was stored on the Tag ontology table
    Table tagTable = schema.getTable("Tag");
    assertNotNull(tagTable, "PetStore schema should have a Tag ontology table");
    String tagChecksum = tagTable.getMetadata().getSetting(CSV_CHECKSUM_SETTING);
    assertNotNull(tagChecksum, "Tag table should have a checksum stored");
    assertFalse(tagChecksum.isEmpty(), "Checksum should not be empty");

    // verify semantics checksum stored at schema level
    String semanticsChecksum = schema.getMetadata().getSetting(SEMANTICS_CHECKSUM_SETTING);
    assertNotNull(semanticsChecksum, "Semantics checksum should be stored");
    assertFalse(semanticsChecksum.isEmpty());
  }

  @Test
  @org.junit.jupiter.api.Order(2)
  void secondImportSkipsUnchangedTables() {
    TableStoreForCsvFilesClasspath store = new TableStoreForCsvFilesClasspath(ONTOLOGY_LOCATION);
    ImportOntologiesTask task =
        new ImportOntologiesTask(schema, store, ONTOLOGY_LOCATION, SEMANTICS_LOCATION);
    task.run();

    // all ontology subtasks should be skipped (checksums match from previous run)
    boolean hasSubTasks = false;
    for (Task subTask : task.getSubTasks()) {
      hasSubTasks = true;
      assertEquals(
          TaskStatus.SKIPPED,
          subTask.getStatus(),
          "Subtask should be skipped: " + subTask.getDescription());
    }
    assertTrue(hasSubTasks, "Should have at least one subtask");
  }

  @Test
  void checksumIsDeterministic() {
    String checksum1 = ImportOntologiesTask.computeClasspathResourceChecksum(SEMANTICS_LOCATION);
    String checksum2 = ImportOntologiesTask.computeClasspathResourceChecksum(SEMANTICS_LOCATION);
    assertEquals(checksum1, checksum2, "Same resource should produce same checksum");
    assertEquals(64, checksum1.length(), "SHA-256 hex string should be 64 characters");
  }

  @Test
  void checksumThrowsForMissingResource() {
    assertThrows(
        MolgenisException.class,
        () -> ImportOntologiesTask.computeClasspathResourceChecksum("/nonexistent.csv"));
  }
}
