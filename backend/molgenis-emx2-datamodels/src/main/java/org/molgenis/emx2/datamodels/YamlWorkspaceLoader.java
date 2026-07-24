package org.molgenis.emx2.datamodels;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.profiles.ResourceListing;
import org.molgenis.emx2.io.ImportDataTask;
import org.molgenis.emx2.io.ImportOntologiesTask;
import org.molgenis.emx2.io.emx2.Emx2Yaml;
import org.molgenis.emx2.io.emx2.Emx2YamlBundle;
import org.molgenis.emx2.io.emx2.ModelPermissions;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvFilesClasspath;
import org.molgenis.emx2.tasks.Task;

/**
 * Loads the /templates workspace of durable YAML model bundles. Each YAML file at the workspace
 * root is a bundle template, discovered by scanning (no hand registration). A template's {@code
 * data:} content loads on every create; its {@code demo:} content only when demo data is requested.
 * {@code additionalSchemas:} companions are provisioned once and reused.
 */
public class YamlWorkspaceLoader {

  private static final String WORKSPACE = "templates";
  private static final String ROOT_PATH = "/" + WORKSPACE;
  private static final String MOLGENIS_YAML = "molgenis.yaml";
  private static final String YAML_SUFFIX = ".yaml";
  private static final String LABEL_SUFFIX = " yaml";
  private static final String SLASH = "/";
  private static final String KEY_TABLES = "tables";
  private static final String KEY_IMPORTS = "imports";
  private static final String KEY_DEMO = "demo";
  private static final String KEY_ADDITIONAL_SCHEMAS = "additionalSchemas";
  private static final String KEY_BUNDLE = "bundle";
  private static final String KEY_PERMISSIONS = "permissions";
  private static final String SCHEMA_BLOCK = "Schema ";
  private static final String CREATED_SCHEMA = "Created schema ";
  private static final String EXISTS_SUFFIX = " already exists — model untouched, data ensured";
  private static final String COMMITTING = "Committing";
  private static final String STEP_DATA = "Import data";
  private static final String STEP_DEMO = "Import demo data";
  private static final String STEP_DEMO_SKIPPED = "Import demo data: skipped (not requested)";
  private static final String TABLES_TEMPLATE = "Created %d tables: %s";
  private static final String SETTINGS_TEMPLATE = "Applied %d settings: %s";
  private static final String PERMISSIONS_TEMPLATE = "Applied permissions: %s";
  private static final String LIST_SEPARATOR = ", ";

  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  public record TemplateInfo(String name, String label, boolean hasDemoData) {}

  public boolean isAvailable() {
    return getClass().getResource(ROOT_PATH) != null;
  }

  public boolean hasTemplate(String template) {
    return isAvailable() && templates().contains(template);
  }

  /**
   * Returns the named template woven into the canonical single-file wire form (imports resolved).
   * Companion schemas declared under {@code additionalSchemas:} are woven from their own multi-file
   * layout and inlined per the inline companion key-set (tables/settings/version/permissions); data
   * and demo CSVs stay out because they are not YAML.
   */
  public String toSingleFileWireForm(String template) {
    String rootContent = readClasspathFile(WORKSPACE + SLASH + template + YAML_SUFFIX);
    Map<String, Object> root = parse(rootContent);
    Emx2YamlBundle bundle = Emx2Yaml.fromBundleFiles(gatherBundleFiles(rootContent, ""));
    return Emx2Yaml.toSingleFile(bundle, weaveCompanions(root));
  }

  private Map<String, Emx2YamlBundle> weaveCompanions(Map<String, Object> root) {
    Map<String, Emx2YamlBundle> woven = new LinkedHashMap<>();
    Object additionalSchemas = root.get(KEY_ADDITIONAL_SCHEMAS);
    if (!(additionalSchemas instanceof Map<?, ?> companions)) {
      return woven;
    }
    for (Map.Entry<?, ?> companion : companions.entrySet()) {
      String name = String.valueOf(companion.getKey());
      if (companion.getValue() instanceof Map<?, ?> body && body.get(KEY_BUNDLE) != null) {
        String bundleReference = String.valueOf(body.get(KEY_BUNDLE));
        String base = parentPath(bundleReference);
        String companionRoot = readClasspathFile(WORKSPACE + SLASH + bundleReference);
        Emx2YamlBundle companionBundle =
            Emx2Yaml.fromBundleFiles(gatherBundleFiles(companionRoot, base));
        woven.put(name, withPermissions(companionBundle, permissionsOf(body)));
      }
    }
    return woven;
  }

  private static Emx2YamlBundle withPermissions(
      Emx2YamlBundle bundle, Map<String, String> permissions) {
    return new Emx2YamlBundle(
        bundle.schema(),
        bundle.formatVersion(),
        bundle.version(),
        bundle.namespaces(),
        bundle.previousNames(),
        bundle.drops(),
        permissions,
        bundle.dataFiles(),
        bundle.demoFiles());
  }

  public List<String> templates() {
    try {
      String[] entries = new ResourceListing().retrieve(ROOT_PATH);
      return Arrays.stream(entries)
          .filter(entry -> entry.endsWith(YAML_SUFFIX))
          .map(entry -> entry.substring(0, entry.length() - YAML_SUFFIX.length()))
          .sorted()
          .toList();
    } catch (Exception exception) {
      throw new MolgenisException("Failed to discover workspace templates", exception);
    }
  }

  public List<TemplateInfo> availableTemplates() {
    return templates().stream().map(this::describeTemplate).toList();
  }

  private TemplateInfo describeTemplate(String template) {
    String rootContent = readClasspathFile(WORKSPACE + SLASH + template + YAML_SUFFIX);
    boolean hasDemoData = parse(rootContent).get(KEY_DEMO) != null;
    return new TemplateInfo(template, template + LABEL_SUFFIX, hasDemoData);
  }

  public Schema create(
      Database database, String template, String schemaName, boolean includeDemoData) {
    return create(
        database,
        template,
        schemaName,
        includeDemoData,
        new Task("Load yaml template: " + template));
  }

  /**
   * Creates a schema from the named workspace template. Each companion schema and the main schema
   * is a single per-schema transaction (create-if-absent + migrate + settings + permissions + data
   * + demo, ending in one {@code Committing} step), mirroring the classic {@link
   * org.molgenis.emx2.io.ImportProfileTask} phase pattern. Companion ontology data is imported
   * through {@link ImportOntologiesTask} so unchanged CSVs are checksum-skipped; the caller's task
   * tree shows the per-schema block, the full created-table list, and the machinery's per-table row
   * counts and skipped notes.
   */
  public Schema create(
      Database database,
      String template,
      String schemaName,
      boolean includeDemoData,
      Task parentTask) {
    String rootContent = readClasspathFile(WORKSPACE + SLASH + template + YAML_SUFFIX);
    provisionCompanions(database, parse(rootContent), parentTask);

    Emx2YamlBundle bundle = Emx2Yaml.fromBundleFiles(gatherBundleFiles(rootContent, ""));
    loadMainSchema(database, schemaName, template, bundle, includeDemoData, parentTask);
    return database.getSchema(schemaName);
  }

  private void loadMainSchema(
      Database database,
      String schemaName,
      String template,
      Emx2YamlBundle bundle,
      boolean includeDemoData,
      Task parentTask) {
    Task block = parentTask.addSubTask(SCHEMA_BLOCK + schemaName).start();
    Task commit = new Task(COMMITTING);
    database.tx(
        db -> {
          Schema schema =
              getOrCreateSchema(db, schemaName, "YAML workspace template " + template, block);
          schema.migrate(bundle.schema());
          reportCreatedTables(block, schema);
          applySettings(schema, bundle.schema().getSettings(), block);
          applyPermissions(schema, bundle.permissions(), block);
          loadMainData(schema, bundle.dataFiles(), STEP_DATA, block);
          if (includeDemoData) {
            loadMainData(schema, bundle.demoFiles(), STEP_DEMO, block);
          } else {
            block.addSubTask(STEP_DEMO_SKIPPED).setSkipped();
          }
          block.addSubTask(commit);
        });
    commit.complete();
    block.complete();
  }

  private void provisionCompanions(Database database, Map<String, Object> root, Task parentTask) {
    Object additionalSchemas = root.get(KEY_ADDITIONAL_SCHEMAS);
    if (!(additionalSchemas instanceof Map<?, ?> companions)) {
      return;
    }
    for (Map.Entry<?, ?> companion : companions.entrySet()) {
      String name = String.valueOf(companion.getKey());
      if (companion.getValue() instanceof Map<?, ?> body && body.get(KEY_BUNDLE) != null) {
        provisionCompanion(
            database, name, String.valueOf(body.get(KEY_BUNDLE)), permissionsOf(body), parentTask);
      }
    }
  }

  private void provisionCompanion(
      Database database,
      String name,
      String bundleReference,
      Map<String, String> permissions,
      Task parentTask) {
    String base = parentPath(bundleReference);
    String rootContent = readClasspathFile(WORKSPACE + SLASH + bundleReference);
    Emx2YamlBundle bundle = Emx2Yaml.fromBundleFiles(gatherBundleFiles(rootContent, base));
    Task block = parentTask.addSubTask(SCHEMA_BLOCK + name).start();
    Task commit = new Task(COMMITTING);
    database.tx(
        db -> {
          Schema existing = db.getSchema(name);
          Schema companion;
          if (existing == null) {
            companion = db.createSchema(name, "Companion schema: " + name);
            block.addSubTask(CREATED_SCHEMA + name);
            companion.migrate(bundle.schema());
            reportCreatedTables(block, companion);
            applyPermissions(companion, permissions, block);
          } else {
            companion = existing;
            block.addSubTask(name + EXISTS_SUFFIX);
          }
          loadCompanionData(companion, bundle.dataFiles(), base, block);
          block.addSubTask(commit);
        });
    commit.complete();
    block.complete();
  }

  private void loadMainData(Schema schema, List<String> entries, String label, Task block) {
    for (String entry : entries) {
      TableStoreForCsvFilesClasspath store =
          new TableStoreForCsvFilesClasspath(WORKSPACE + SLASH + entry);
      ImportDataTask task = new ImportDataTask(schema, store, false);
      task.setDescription(label);
      block.addSubTask(task);
      task.run();
    }
  }

  private void loadCompanionData(Schema companion, List<String> entries, String base, Task block) {
    for (String entry : entries) {
      String directory = base.isEmpty() ? entry : base + SLASH + entry;
      String storePath = WORKSPACE + SLASH + directory;
      TableStoreForCsvFilesClasspath store = new TableStoreForCsvFilesClasspath(storePath);
      ImportOntologiesTask task =
          new ImportOntologiesTask(companion, store, SLASH + storePath, null);
      task.setDescription(STEP_DATA);
      block.addSubTask(task);
      task.run();
    }
  }

  private static void reportCreatedTables(Task block, Schema schema) {
    List<String> names = new ArrayList<>(schema.getMetadata().getTableNames());
    Collections.sort(names);
    block.addSubTask(
        String.format(TABLES_TEMPLATE, names.size(), String.join(LIST_SEPARATOR, names)));
  }

  private static void applyPermissions(Schema schema, Map<String, String> permissions, Task block) {
    if (permissions.isEmpty()) {
      return;
    }
    ModelPermissions.apply(schema, permissions);
    List<String> rendered =
        permissions.entrySet().stream()
            .map(entry -> entry.getKey() + " = " + entry.getValue())
            .toList();
    block.addSubTask(String.format(PERMISSIONS_TEMPLATE, String.join(LIST_SEPARATOR, rendered)));
  }

  private static Map<String, String> permissionsOf(Map<?, ?> companionBody) {
    if (companionBody.get(KEY_PERMISSIONS) instanceof Map<?, ?> permissions) {
      Map<String, String> result = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : permissions.entrySet()) {
        result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
      }
      return result;
    }
    return Map.of();
  }

  private static void applySettings(Schema schema, Map<String, String> settings, Task block) {
    if (settings.isEmpty()) {
      return;
    }
    for (Map.Entry<String, String> entry : settings.entrySet()) {
      schema.getMetadata().setSetting(entry.getKey(), entry.getValue());
    }
    block.addSubTask(
        String.format(
            SETTINGS_TEMPLATE, settings.size(), String.join(LIST_SEPARATOR, settings.keySet())));
  }

  private Map<String, String> gatherBundleFiles(String rootContent, String base) {
    Map<String, String> files = new LinkedHashMap<>();
    files.put(MOLGENIS_YAML, rootContent);
    Map<String, Object> root = parse(rootContent);
    gatherImports(files, root.get(KEY_IMPORTS), base);
    if (root.get(KEY_TABLES) instanceof List<?> entries) {
      for (Object entry : entries) {
        if (entry instanceof String reference) {
          gatherFileAndImports(files, reference, base);
        }
      }
    }
    return files;
  }

  private void gatherFileAndImports(Map<String, String> files, String reference, String base) {
    if (files.containsKey(reference)) {
      return;
    }
    String resourcePath = base.isEmpty() ? reference : base + SLASH + reference;
    String content = readClasspathFile(WORKSPACE + SLASH + resourcePath);
    files.put(reference, content);
    gatherImports(files, parse(content).get(KEY_IMPORTS), base);
  }

  private void gatherImports(Map<String, String> files, Object imports, String base) {
    if (imports instanceof List<?> importedFiles) {
      for (Object imported : importedFiles) {
        if (imported instanceof String importReference) {
          gatherFileAndImports(files, importReference, base);
        }
      }
    }
  }

  private static Schema getOrCreateSchema(
      Database database, String schemaName, String description, Task block) {
    Schema schema = database.getSchema(schemaName);
    if (schema == null) {
      schema = database.createSchema(schemaName, description);
      block.addSubTask(CREATED_SCHEMA + schemaName);
    } else {
      block.addSubTask(schemaName + EXISTS_SUFFIX);
    }
    return schema;
  }

  private static String parentPath(String path) {
    int lastSlash = path.lastIndexOf('/');
    return lastSlash < 0 ? "" : path.substring(0, lastSlash);
  }

  private Map<String, Object> parse(String content) {
    try {
      return yamlMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
    } catch (Exception exception) {
      throw new MolgenisException("Failed to parse workspace bundle root", exception);
    }
  }

  private static String readClasspathFile(String resourcePath) {
    try (InputStream stream =
        YamlWorkspaceLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (stream == null) {
        throw new MolgenisException("Failed to read workspace resource: " + resourcePath);
      }
      try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        StringBuilder builder = new StringBuilder();
        int character;
        while ((character = reader.read()) != -1) {
          builder.append((char) character);
        }
        return builder.toString();
      }
    } catch (MolgenisException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new MolgenisException("Failed to read workspace resource: " + resourcePath, exception);
    }
  }
}
