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
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.emx2.Emx2Yaml;
import org.molgenis.emx2.io.emx2.Emx2YamlBundle;
import org.molgenis.emx2.io.emx2.ModelPermissions;

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

  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  public record TemplateInfo(String name, String label, boolean hasDemoData) {}

  public boolean isAvailable() {
    return getClass().getResource(ROOT_PATH) != null;
  }

  public boolean hasTemplate(String template) {
    return isAvailable() && templates().contains(template);
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
    String rootContent = readClasspathFile(WORKSPACE + SLASH + template + YAML_SUFFIX);
    provisionCompanions(database, parse(rootContent));

    Emx2YamlBundle bundle = Emx2Yaml.fromBundleFiles(gatherBundleFiles(rootContent, ""));
    Schema schema = getOrCreateSchema(database, schemaName, "YAML workspace template " + template);
    schema.migrate(bundle.schema());
    applySettings(schema, bundle.schema().getSettings());
    ModelPermissions.apply(schema, bundle.permissions());

    loadData(schema, bundle.dataFiles(), "");
    if (includeDemoData) {
      loadData(schema, bundle.demoFiles(), "");
    }
    return schema;
  }

  private void provisionCompanions(Database database, Map<String, Object> root) {
    Object additionalSchemas = root.get(KEY_ADDITIONAL_SCHEMAS);
    if (!(additionalSchemas instanceof Map<?, ?> companions)) {
      return;
    }
    for (Map.Entry<?, ?> companion : companions.entrySet()) {
      String name = String.valueOf(companion.getKey());
      if (companion.getValue() instanceof Map<?, ?> body && body.get(KEY_BUNDLE) != null) {
        provisionCompanion(
            database, name, String.valueOf(body.get(KEY_BUNDLE)), permissionsOf(body));
      }
    }
  }

  private void provisionCompanion(
      Database database, String name, String bundleReference, Map<String, String> permissions) {
    String base = parentPath(bundleReference);
    String rootContent = readClasspathFile(WORKSPACE + SLASH + bundleReference);
    Emx2YamlBundle bundle = Emx2Yaml.fromBundleFiles(gatherBundleFiles(rootContent, base));
    Schema companion = database.getSchema(name);
    if (companion == null) {
      companion = database.createSchema(name, "Companion schema: " + name);
      companion.migrate(bundle.schema());
      ModelPermissions.apply(companion, permissions);
    }
    loadData(companion, bundle.dataFiles(), base);
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

  private void loadData(Schema schema, List<String> entries, String base) {
    for (String entry : entries) {
      String directory = base.isEmpty() ? entry : base + SLASH + entry;
      MolgenisIO.fromClasspathDirectory(WORKSPACE + SLASH + directory, schema, false);
    }
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
