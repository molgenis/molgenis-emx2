package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.tasks.Task;
import org.molgenis.emx2.tasks.TaskStatus;

class YamlWorkspaceLoaderTest extends TestLoaders {

  private static final String CATALOGUE = "catalogue";
  private static final String DIAMOND = "diamond";
  private static final String RD3 = "rd3";
  private static final String CONTACTS = "Contacts";
  private static final String COLLECTIONS = "Collections";
  private static final String ORGANISATIONS = "Organisations";
  private static final String EMAIL = "email";
  private static final String COMPANION = "CatalogueOntologies";

  private static final String CATALOGUE_NO_DEMO = "wsCatalogueNoDemo";
  private static final String CATALOGUE_DEMO = "wsCatalogueDemo";
  private static final String RD3_SCHEMA = "wsRd3";
  private static final String ROOT_IMPORTS_TEMPLATE = "rootimports/demo";
  private static final String ROOT_IMPORTS_SCHEMA = "wsRootImports";
  private static final String REUSE_TEMPLATE = "reusecompanion/demo";
  private static final String REUSE_MAIN_A = "wsReuseCompanionA";
  private static final String REUSE_MAIN_B = "wsReuseCompanionB";
  private static final String REUSE_COMPANION = "YwlReuseOntologies";
  private static final String COUNTRIES = "Countries";
  private static final String NAME = "name";
  private static final String NETHERLANDS = "Netherlands";
  private static final String SKIPPED_MARKER = "up to date, skipped";
  private static final String ATOMICITY_TEMPLATE = "atomicity/broken";
  private static final String ATOMICITY_SCHEMA = "wsAtomicityMain";
  private static final String COMMITFAIL_TEMPLATE = "atomicity/commitfail";
  private static final String COMMITFAIL_SCHEMA = "wsCommitFail";
  private static final String RELOAD_SCHEMA = "wsReloadMain";
  private static final String COMMITTING = "Committing";
  private static final String MODEL_UNTOUCHED = "model untouched";
  private static final String CREATED_TABLES = "Created ";
  private static final String UPDATED_TABLES = "Updated ";
  private static final String TABLES_INFIX = " tables: ";
  private static final String MAIN_REUSE_PHRASE = "applying template model into it";
  private static final String STAGES_SCHEMA = "wsRd3Stages";
  private static final String SETTINGS_NONE = "Settings: none declared";
  private static final String PERMISSIONS_NONE = "Permissions: none declared";
  private static final String DATA_NONE = "Import data: none declared";
  private static final String DEMO_SKIPPED = "Import demo data: skipped (not requested)";
  private static final String SCHEMA_PREFIX = "Schema ";

  private static final YamlWorkspaceLoader loader = new YamlWorkspaceLoader();

  @BeforeAll
  void provisionWorkspaces() {
    for (String schemaName : List.of(CATALOGUE_NO_DEMO, CATALOGUE_DEMO, RD3_SCHEMA)) {
      database.dropSchemaIfExists(schemaName);
    }
    loader.create(database, CATALOGUE, CATALOGUE_NO_DEMO, false);
    loader.create(database, CATALOGUE, CATALOGUE_DEMO, true);
    loader.create(database, RD3, RD3_SCHEMA, true);
  }

  @Test
  void discoveryFindsBothBundlesAndCreatesBothSchemas() {
    assertEquals(List.of(CATALOGUE, DIAMOND, RD3), loader.templates());
    assertNotNull(
        database.getSchema(CATALOGUE_NO_DEMO).getTable(COLLECTIONS),
        "catalogue schema must have Collections");
    assertNotNull(
        database.getSchema(RD3_SCHEMA).getTable("Subjects"), "rd3 schema must have Subjects");
    assertNotNull(database.getSchema(COMPANION), "companion ontology schema must be provisioned");
  }

  @Test
  void sharedAndRedefinedContactsDiverge() {
    Schema catalogueSchema = database.getSchema(CATALOGUE_NO_DEMO);
    Schema rd3Schema = database.getSchema(RD3_SCHEMA);

    assertNotNull(
        catalogueSchema.getTable(CONTACTS).getMetadata().getColumn(EMAIL),
        "catalogue Contacts (shared) must carry the email column");
    assertNull(
        rd3Schema.getTable(CONTACTS).getMetadata().getColumn(EMAIL),
        "rd3 Contacts (redefined) must not carry the shared email column");
    assertNotNull(
        rd3Schema.getTable(CONTACTS).getMetadata().getColumn("firstName"),
        "rd3 Contacts (redefined) must carry its own firstName column");
  }

  @Test
  void importedTableFileColumnsResolveInWorkspace() {
    assertNotNull(
        database
            .getSchema(CATALOGUE_NO_DEMO)
            .getTable(COLLECTIONS)
            .getMetadata()
            .getColumn("notes"),
        "Collections must carry the 'notes' column pulled in via the table file's own imports:");
  }

  @Test
  void rootLevelImportsResolveInWorkspace() {
    database.dropSchemaIfExists(ROOT_IMPORTS_SCHEMA);
    Schema schema = loader.create(database, ROOT_IMPORTS_TEMPLATE, ROOT_IMPORTS_SCHEMA, false);
    assertNotNull(
        schema.getTable("Widget").getMetadata().getColumn("reviewed"),
        "an inline table must resolve a column pulled in via the bundle's own root-level imports:");
  }

  @Test
  void availableTemplatesCarryYamlLabelAndDemoFlag() {
    List<YamlWorkspaceLoader.TemplateInfo> available = loader.availableTemplates();
    assertEquals(
        List.of(CATALOGUE, DIAMOND, RD3),
        available.stream().map(YamlWorkspaceLoader.TemplateInfo::name).toList());
    YamlWorkspaceLoader.TemplateInfo catalogue = available.get(0);
    assertEquals(
        "catalogue yaml", catalogue.label(), "discovered template label carries the yaml suffix");
    assertTrue(catalogue.hasDemoData(), "the catalogue template carries demo: data");
  }

  @Test
  void dataLoadsAlwaysDemoLoadsOnlyOnRequest() {
    Schema noDemo = database.getSchema(CATALOGUE_NO_DEMO);
    assertEquals(
        2,
        noDemo.getTable(ORGANISATIONS).retrieveRows().size(),
        "data: reference rows must load even without demo data");
    assertTrue(
        noDemo.getTable(COLLECTIONS).retrieveRows().isEmpty(),
        "demo: rows must not load when demo data is not requested");

    Schema withDemo = database.getSchema(CATALOGUE_DEMO);
    assertEquals(
        2,
        withDemo.getTable(COLLECTIONS).retrieveRows().size(),
        "demo: rows must load when demo data is requested");
  }

  @Test
  void rootPermissionsAndSettingsAndCompanionPermissionsApply() {
    Schema catalogueSchema = database.getSchema(CATALOGUE_NO_DEMO);
    assertEquals(
        "[{\"label\":\"Home\",\"href\":\"/\"}]",
        catalogueSchema.getMetadata().getSetting("menu"),
        "bundle-root settings must be written to the created schema");
    assertEquals(
        Privileges.VIEWER.toString(),
        catalogueSchema.getRoleForUser(Constants.ANONYMOUS),
        "bundle-root permissions must add the role default to the main schema");
    assertEquals(
        Privileges.VIEWER.toString(),
        database.getSchema(COMPANION).getRoleForUser(Constants.ANONYMOUS),
        "companion permissions must add the role default to the provisioned companion schema");
  }

  @Test
  void companionOntologyImportIsChecksumSkippedOnReuse() {
    database.dropSchemaIfExists(REUSE_MAIN_A);
    database.dropSchemaIfExists(REUSE_MAIN_B);
    database.dropSchemaIfExists(REUSE_COMPANION);

    Task firstRun = new Task("first reuse run");
    loader.create(database, REUSE_TEMPLATE, REUSE_MAIN_A, false, firstRun);
    assertTrue(
        collectDescriptions(firstRun).stream().noneMatch(step -> step.contains(SKIPPED_MARKER)),
        "the first companion import must actually load the ontology, not report it skipped");

    Task secondRun = new Task("second reuse run");
    loader.create(database, REUSE_TEMPLATE, REUSE_MAIN_B, false, secondRun);
    assertTrue(
        collectDescriptions(secondRun).stream()
            .anyMatch(step -> step.contains(COUNTRIES) && step.contains(SKIPPED_MARKER)),
        "the second create must checksum-skip the unchanged companion ontology");

    List<Row> terms = database.getSchema(REUSE_COMPANION).getTable(COUNTRIES).retrieveRows();
    assertTrue(
        terms.stream().anyMatch(term -> NETHERLANDS.equals(term.getString(NAME))),
        "the companion ontology data loaded on first create must remain present");
  }

  @Test
  void everyStageLineAppearsEvenWhenNothingDeclared() {
    database.dropSchemaIfExists(STAGES_SCHEMA);

    Task task = new Task("rd3 stages run");
    loader.create(database, RD3, STAGES_SCHEMA, false, task);

    List<Task> tasks = collectTasks(task);
    assertTrue(
        tasks.stream().anyMatch(subTask -> subTask.getDescription().startsWith(SCHEMA_PREFIX)),
        "a per-schema block must be emitted");
    assertStageSkipped(tasks, SETTINGS_NONE);
    assertStageSkipped(tasks, PERMISSIONS_NONE);
    assertStageSkipped(tasks, DATA_NONE);
    assertStageSkipped(tasks, DEMO_SKIPPED);
  }

  @Test
  void failedMainSchemaRollsBackAndSurfacesError() {
    database.dropSchemaIfExists(ATOMICITY_SCHEMA);

    YamlWorkspaceLoadTask task =
        new YamlWorkspaceLoadTask(database, ATOMICITY_TEMPLATE, ATOMICITY_SCHEMA, false);
    MolgenisException thrown =
        assertThrows(
            MolgenisException.class,
            task::run,
            "a main-schema migrate failure must abort the create");
    assertNotNull(
        thrown.getCause(),
        "the propagated failure must carry the original cause, not drop the chain");
    assertNull(
        database.getSchema(ATOMICITY_SCHEMA),
        "the per-schema transaction must roll back, leaving no half-created main schema");

    assertEquals(
        TaskStatus.ERROR,
        task.getStatus(),
        "the load task must surface the failure as error state");
    List<Task> tasks = collectTasks(task);
    assertTrue(
        tasks.stream()
            .anyMatch(
                subTask ->
                    subTask.getStatus() == TaskStatus.ERROR
                        && subTask.getDescription().startsWith(SCHEMA_PREFIX + ATOMICITY_SCHEMA)),
        "the failing schema block must end in error state");
    assertTrue(
        tasks.stream().noneMatch(Task::isRunning),
        "no subtask may be left running or waiting after a failed load");
  }

  @Test
  void reloadingTemplateOntoExistingMainSchemaLogsTruthfully() {
    database.dropSchemaIfExists(RELOAD_SCHEMA);
    loader.create(database, RD3, RELOAD_SCHEMA, false, new Task("first reload run"));

    Task reload = new Task("second reload run");
    loader.create(database, RD3, RELOAD_SCHEMA, false, reload);

    List<String> steps = collectDescriptions(reload);
    assertTrue(
        steps.stream().noneMatch(step -> step.contains(MODEL_UNTOUCHED)),
        "main-schema reuse migrates the model, so it must not claim the model was left untouched");
    assertTrue(
        steps.stream()
            .noneMatch(step -> step.startsWith(CREATED_TABLES) && step.contains(TABLES_INFIX)),
        "main-schema reuse must not claim tables were freshly created while it migrates");
    assertTrue(
        steps.stream().anyMatch(step -> step.contains(MAIN_REUSE_PHRASE)),
        "main-schema reuse must state the template model is applied into the existing schema");
    assertTrue(
        steps.stream()
            .anyMatch(step -> step.startsWith(UPDATED_TABLES) && step.contains(TABLES_INFIX)),
        "main-schema reuse must report its tables as updated, not created");
  }

  @Test
  void commitTimeFailureFinalizesCommittingSubtask() {
    database.dropSchemaIfExists(COMMITFAIL_SCHEMA);

    YamlWorkspaceLoadTask task =
        new YamlWorkspaceLoadTask(database, COMMITFAIL_TEMPLATE, COMMITFAIL_SCHEMA, false);
    assertThrows(
        MolgenisException.class,
        task::run,
        "a deferred foreign-key violation must fail the transaction at commit");
    assertNull(
        database.getSchema(COMMITFAIL_SCHEMA),
        "a commit-time failure must roll back, leaving no half-created schema");

    List<Task> tasks = collectTasks(task);
    assertTrue(
        tasks.stream().anyMatch(subTask -> subTask.getDescription().startsWith(COMMITTING)),
        "the Committing subtask must have been reached before the commit failed");
    assertTrue(
        tasks.stream().noneMatch(Task::isRunning),
        "the Committing subtask must be finalized on commit failure, not left waiting");
  }

  private static void assertStageSkipped(List<Task> tasks, String prefix) {
    assertTrue(
        tasks.stream()
            .anyMatch(
                subTask ->
                    subTask.getDescription().startsWith(prefix)
                        && subTask.getStatus() == TaskStatus.SKIPPED),
        "stage line must appear as a skipped step: " + prefix);
  }

  private static List<Task> collectTasks(Task task) {
    List<Task> tasks = new ArrayList<>();
    collectTasks(task, tasks);
    return tasks;
  }

  private static void collectTasks(Task task, List<Task> tasks) {
    tasks.add(task);
    for (Task subTask : task.getSubTasks()) {
      collectTasks(subTask, tasks);
    }
  }

  private static List<String> collectDescriptions(Task task) {
    List<String> descriptions = new ArrayList<>();
    collectDescriptions(task, descriptions);
    return descriptions;
  }

  private static void collectDescriptions(Task task, List<String> descriptions) {
    descriptions.add(task.getDescription());
    for (Task subTask : task.getSubTasks()) {
      collectDescriptions(subTask, descriptions);
    }
  }
}
