package org.molgenis.emx2.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.emx2.Emx2Yaml;
import org.molgenis.emx2.io.emx2.bundle.AdditionalSchemaDef;
import org.molgenis.emx2.io.emx2.bundle.Bundle;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvFilesClasspath;
import org.molgenis.emx2.tasks.Task;

public class ImportBundleTask extends Task {

  private final Database database;
  private final String schemaName;
  private final String description;
  private final String bundlePath;
  private final boolean includeDemoData;

  public ImportBundleTask(
      Database database,
      String schemaName,
      String description,
      String bundlePath,
      boolean includeDemoData) {
    this.database = database;
    this.schemaName = schemaName;
    this.description = description;
    this.bundlePath = bundlePath;
    this.includeDemoData = includeDemoData;
  }

  @Override
  public void run() {
    this.start();
    try {
      runTransaction();
      this.complete();
    } catch (Exception e) {
      this.completeWithError("ImportBundleTask failed: " + e.getMessage());
    }
  }

  private void runTransaction() throws IOException {
    Task schemaTask = this.addSubTask("Create schema and metadata");
    schemaTask.start();
    Task commitTask = new Task("Committing");
    database.tx(
        db -> {
          try {
            Emx2Yaml.BundleResult result = loadBundle();
            Bundle bundle = result.getBundle();

            Schema schema = db.createSchema(schemaName, description);
            schema.migrate(result.getSchema());

            applyPermissions(schema, bundle.permissions());
            applySettings(schema, bundle, schemaTask);
            applyAdditionalSchemas(db, bundle, schemaTask);

            if (includeDemoData) {
              loadDemoData(schema, bundle, schemaTask);
            }

            schemaTask.addSubTask(commitTask);
            commitTask.complete();
          } catch (IOException e) {
            throw new MolgenisException("Failed to load bundle: " + e.getMessage(), e);
          }
        });
    schemaTask.complete();
  }

  private Emx2Yaml.BundleResult loadBundle() throws IOException {
    boolean isDirectory = bundlePath.endsWith("/");
    if (isDirectory) {
      String dirPath = bundlePath.substring(0, bundlePath.length() - 1);
      return Emx2Yaml.fromBundleClasspath(dirPath);
    } else {
      try (InputStream inputStream = getClass().getResourceAsStream("/" + bundlePath)) {
        if (inputStream == null) {
          throw new MolgenisException("Bundle not found on classpath: " + bundlePath);
        }
        return Emx2Yaml.fromBundle(inputStream);
      }
    }
  }

  private void applyPermissions(Schema schema, Map<String, String> permissions) {
    for (Map.Entry<String, String> perm : permissions.entrySet()) {
      if ("view".equals(perm.getKey())) {
        schema.addMember(perm.getValue(), Privileges.VIEWER.toString());
      } else if ("edit".equals(perm.getKey())) {
        schema.addMember(perm.getValue(), Privileges.EDITOR.toString());
      }
    }
  }

  private void applySettings(Schema schema, Bundle bundle, Task parentTask) {
    for (String settingsPath : bundle.settings()) {
      String resolved = resolveRelativePath(settingsPath);
      MolgenisIO.fromClasspathDirectory(resolved, schema, false);
      parentTask.addSubTask("Loaded settings from: " + resolved).complete();
    }
  }

  private void applyAdditionalSchemas(Database db, Bundle bundle, Task parentTask) {
    for (Map.Entry<String, AdditionalSchemaDef> entry : bundle.additionalSchemas().entrySet()) {
      String addSchemaName = entry.getKey();
      AdditionalSchemaDef def = entry.getValue();
      Schema addSchema = db.getSchema(addSchemaName);
      if (addSchema == null) {
        addSchema = db.createSchema(addSchemaName);
      }
      if (def.bundle() != null) {
        String bundleDefPath = resolveRelativePath(def.bundle());
        if (isYamlBundle(bundleDefPath)) {
          try {
            Emx2Yaml.BundleResult bundleResult = Emx2Yaml.fromBundleClasspath(bundleDefPath);
            addSchema.migrate(bundleResult.getSchema());
            parentTask.addSubTask("Imported YAML bundle into: " + addSchemaName).complete();
          } catch (IOException e) {
            throw new MolgenisException("Failed to load YAML bundle: " + bundleDefPath, e);
          }
        } else {
          Task importSchemaTask =
              new ImportSchemaTask(
                  new TableStoreForCsvFilesClasspath(bundleDefPath), addSchema, false);
          importSchemaTask.setDescription("Import CSV bundle into: " + addSchemaName);
          parentTask.addSubTask(importSchemaTask);
          importSchemaTask.run();
        }
      }
      applySettings(addSchema, def, parentTask);
      applyPermissions(addSchema, def.permissions());
      if (includeDemoData) {
        loadDataList(addSchema, def.data(), parentTask);
      }
    }
  }

  private void applySettings(Schema schema, AdditionalSchemaDef def, Task parentTask) {
    for (String settingsPath : def.settings()) {
      String resolved = resolveRelativePath(settingsPath);
      MolgenisIO.fromClasspathDirectory(resolved, schema, false);
      parentTask.addSubTask("Loaded settings from: " + resolved).complete();
    }
  }

  private void loadDemoData(Schema schema, Bundle bundle, Task parentTask) {
    loadDataList(schema, bundle.demodata(), parentTask);
  }

  private void loadDataList(Schema schema, java.util.List<String> dataPaths, Task parentTask) {
    for (String dataPath : dataPaths) {
      String resolved = resolveRelativePath(dataPath);
      Task dataTask =
          new ImportDataTask(schema, new TableStoreForCsvFilesClasspath(resolved), false)
              .setDescription("Import data from: " + resolved);
      parentTask.addSubTask(dataTask);
      dataTask.run();
    }
  }

  private boolean isYamlBundle(String modelPath) {
    if (modelPath.endsWith(".yaml") || modelPath.endsWith(".yml")) {
      return true;
    }
    String directoryYaml =
        (modelPath.endsWith("/") ? modelPath : modelPath + "/") + "molgenis.yaml";
    return getClass().getResource("/" + directoryYaml) != null;
  }

  private String resolveRelativePath(String relativePath) {
    if (relativePath.startsWith("/")) {
      return relativePath;
    }
    String normalised = relativePath.startsWith("./") ? relativePath.substring(2) : relativePath;
    String baseDir =
        bundlePath.contains("/") ? bundlePath.substring(0, bundlePath.lastIndexOf('/')) : "";
    return baseDir.isEmpty() ? normalised : baseDir + "/" + normalised;
  }
}
