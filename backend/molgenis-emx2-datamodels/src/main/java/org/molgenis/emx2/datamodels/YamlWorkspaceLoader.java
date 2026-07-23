package org.molgenis.emx2.datamodels;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.profiles.ResourceListing;
import org.molgenis.emx2.io.ImportSchemaTask;
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
  private static final String STEP_CREATE = "Create schema and metadata: ";
  private static final String STEP_DATA = "Import data: ";
  private static final String STEP_DEMO = "Import demo data: ";
  private static final String STEP_COMPANIONS = "Provision companion schemas";
  private static final String STEP_SETTINGS = "Apply settings and permissions: ";
  private static final String STEP_COMPANION_DATA = "Import companion data: ";

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
   * Creates a schema from the named workspace template, emitting a visible task step for every
   * stage (companion provisioning, create schema/metadata, settings/permissions, data: and demo:
   * imports) so the caller's task tree shows the same per-table row counts and skipped-sheet notes
   * the classic loaders produce.
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
    Task schemaTask = parentTask.addSubTask(STEP_CREATE + template).start();
    Schema schema = getOrCreateSchema(database, schemaName, "YAML workspace template " + template);
    schema.migrate(bundle.schema());
    schemaTask.complete();

    Task settingsTask = parentTask.addSubTask(STEP_SETTINGS + template).start();
    applySettings(schema, bundle.schema().getSettings());
    ModelPermissions.apply(schema, bundle.permissions());
    settingsTask.complete();

    loadData(schema, bundle.dataFiles(), "", parentTask, STEP_DATA + template);
    if (includeDemoData) {
      loadData(schema, bundle.demoFiles(), "", parentTask, STEP_DEMO + template);
    }
    return schema;
  }

  private void provisionCompanions(Database database, Map<String, Object> root, Task parentTask) {
    Object additionalSchemas = root.get(KEY_ADDITIONAL_SCHEMAS);
    if (!(additionalSchemas instanceof Map<?, ?> companions)) {
      return;
    }
    Task companionTask = parentTask.addSubTask(STEP_COMPANIONS).start();
    for (Map.Entry<?, ?> companion : companions.entrySet()) {
      String name = String.valueOf(companion.getKey());
      if (companion.getValue() instanceof Map<?, ?> body && body.get(KEY_BUNDLE) != null) {
        provisionCompanion(
            database,
            name,
            String.valueOf(body.get(KEY_BUNDLE)),
            permissionsOf(body),
            companionTask);
      }
    }
    companionTask.complete();
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
    Schema companion = database.getSchema(name);
    if (companion == null) {
      companion = database.createSchema(name, "Companion schema: " + name);
      companion.migrate(bundle.schema());
      ModelPermissions.apply(companion, permissions);
    }
    loadData(companion, bundle.dataFiles(), base, parentTask, STEP_COMPANION_DATA + name);
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

  private static void applySettings(Schema schema, Map<String, String> settings) {
    for (Map.Entry<String, String> entry : settings.entrySet()) {
      schema.getMetadata().setSetting(entry.getKey(), entry.getValue());
    }
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

  private void loadData(
      Schema schema, List<String> entries, String base, Task parentTask, String label) {
    if (entries.isEmpty()) {
      return;
    }
    Task dataTask = parentTask.addSubTask(label).start();
    for (String entry : entries) {
      String directory = base.isEmpty() ? entry : base + SLASH + entry;
      TableStoreForCsvFilesClasspath store =
          new TableStoreForCsvFilesClasspath(WORKSPACE + SLASH + directory);
      ImportSchemaTask importTask =
          new ImportSchemaTask(store, schema, false).setFilter(ImportSchemaTask.Filter.DATA_ONLY);
      dataTask.addSubTask(importTask);
      importTask.run();
    }
    dataTask.complete();
  }

  private static Schema getOrCreateSchema(
      Database database, String schemaName, String description) {
    Schema schema = database.getSchema(schemaName);
    if (schema == null) {
      schema = database.createSchema(schemaName, description);
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
