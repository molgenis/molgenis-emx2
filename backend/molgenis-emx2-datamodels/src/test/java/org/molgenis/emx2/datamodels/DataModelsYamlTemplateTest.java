package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.Task;
import org.molgenis.emx2.tasks.TaskStatus;

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
  private static final String SCHEMA_DEMO = "dmYamlCatalogueDemo";
  private static final String SCHEMA_STEPS = "dmYamlCatalogueSteps";

  private static Database database;

  @BeforeAll
  void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    dropSchemas();
  }

  @AfterAll
  void cleanup() {
    dropSchemas();
  }

  private void dropSchemas() {
    database.dropSchemaIfExists(SCHEMA_NO_DEMO);
    database.dropSchemaIfExists(SCHEMA_DEMO);
    database.dropSchemaIfExists(SCHEMA_STEPS);
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
  void yamlTemplateSeamLoadsDemoRowsWhenRequested() {
    DataModels.getImportTask(database, SCHEMA_DEMO, "", CATALOGUE_TEMPLATE, true).run();
    Schema schema = database.getSchema(SCHEMA_DEMO);

    assertEquals(
        2,
        schema.getTable(COLLECTIONS).retrieveRows().size(),
        "includeDemoData=true must load demo: rows through the createSchema seam");
  }

  @Test
  void yamlTemplateLoadTaskReportsPerSchemaHierarchyAndPerTableSteps() {
    Task task = DataModels.getImportTask(database, SCHEMA_STEPS, "", CATALOGUE_TEMPLATE, true);
    task.run();

    List<String> steps = new ArrayList<>();
    collectDescriptions(task, steps);

    assertTrue(
        steps.stream().anyMatch(step -> step.startsWith("Schema " + SCHEMA_STEPS)),
        "task must log a per-schema block for the main schema");
    assertTrue(
        steps.stream().anyMatch(step -> step.startsWith("Schema CatalogueOntologies")),
        "task must log a per-schema block for the companion, listed first");

    String tablesLine =
        steps.stream()
            .filter(
                step ->
                    step.startsWith("Created ")
                        && step.contains(" tables: ")
                        && step.contains(COLLECTIONS))
            .findFirst()
            .orElse("");
    assertTrue(
        tablesLine.contains(COLLECTIONS)
            && tablesLine.contains("Contacts")
            && tablesLine.contains(ORGANISATIONS),
        "the main schema created-tables line must list the full set of tables, uncapped: "
            + tablesLine);

    assertTrue(
        steps.stream().anyMatch(step -> step.contains("Modified") && step.contains(ORGANISATIONS)),
        "task must surface a per-table row-count line for the data: import");

    long committing = steps.stream().filter(step -> step.startsWith("Committing")).count();
    assertEquals(
        2, committing, "exactly one Committing per schema block is expected (companion + main)");

    assertTrue(
        steps.stream().noneMatch(step -> step.contains("Import from store")),
        "the bare 'Import from store' plumbing step must not surface in the log");

    List<Task> allTasks = new ArrayList<>();
    collectTasks(task, allTasks);
    assertTrue(
        allTasks.stream().noneMatch(Task::isRunning),
        "after a completed load no subtask may be left running or waiting");
    assertTrue(
        allTasks.stream()
            .allMatch(
                subTask ->
                    subTask.getStatus() == TaskStatus.COMPLETED
                        || subTask.getStatus() == TaskStatus.SKIPPED),
        "every subtask must end COMPLETED or SKIPPED after a completed load");
  }

  private static void collectTasks(Task task, List<Task> tasks) {
    tasks.add(task);
    for (Task subTask : task.getSubTasks()) {
      collectTasks(subTask, tasks);
    }
  }

  private static void collectDescriptions(Task task, List<String> descriptions) {
    descriptions.add(task.getDescription());
    for (Task subTask : task.getSubTasks()) {
      collectDescriptions(subTask, descriptions);
    }
  }

  @Test
  void unknownTemplateNameStillThrows() {
    assertThrows(
        MolgenisException.class,
        () -> DataModels.getImportTask(database, "dmUnknownTemplate", "", "does-not-exist", false));
  }
}
