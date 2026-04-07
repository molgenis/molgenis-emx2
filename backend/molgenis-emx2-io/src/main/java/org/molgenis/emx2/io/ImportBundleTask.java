package org.molgenis.emx2.io;

import java.io.IOException;
import java.io.InputStream;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.emx2.Emx2Yaml;
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

            applyPermissions(schema, bundle);
            applySettings(schema, bundle, schemaTask);

            Schema ontologySchema = resolveOntologySchema(schema, db, bundle, schemaTask);
            applyOntologySchemaPermissions(ontologySchema, bundle);

            if (includeDemoData) {
              loadDemoData(schema, bundle, schemaTask);
            }

            schemaTask.addSubTask(commitTask);
          } catch (IOException e) {
            throw new MolgenisException("Failed to load bundle: " + e.getMessage(), e);
          }
        });
    commitTask.complete();
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

  private void applyPermissions(Schema schema, Bundle bundle) {
    if (bundle.viewPermission() != null) {
      schema.addMember(bundle.viewPermission(), Privileges.VIEWER.toString());
    }
    if (bundle.editPermission() != null) {
      schema.addMember(bundle.editPermission(), Privileges.EDITOR.toString());
    }
  }

  private void applySettings(Schema schema, Bundle bundle, Task parentTask) {
    if (bundle.settings() == null) return;
    for (String settingsPath : bundle.settings()) {
      String resolved = resolveRelativePath(settingsPath);
      MolgenisIO.fromClasspathDirectory(resolved, schema, false);
      parentTask.addSubTask("Loaded settings from: " + resolved).complete();
    }
  }

  private Schema resolveOntologySchema(Schema schema, Database db, Bundle bundle, Task parentTask) {
    if (bundle.ontologiesToFixedSchema() == null) {
      return schema;
    }
    Schema ontologySchema = db.getSchema(bundle.ontologiesToFixedSchema());
    if (ontologySchema == null) {
      ontologySchema = db.createSchema(bundle.ontologiesToFixedSchema());
    }
    if (bundle.additionalFixedSchemaModel() != null) {
      String modelPath = resolveRelativePath(bundle.additionalFixedSchemaModel());
      Task importSchemaTask =
          new ImportSchemaTask(
              new TableStoreForCsvFilesClasspath(modelPath), ontologySchema, false);
      importSchemaTask.setDescription("Import additional model into ontology schema");
      parentTask.addSubTask(importSchemaTask);
      importSchemaTask.run();
    }
    return ontologySchema;
  }

  private void applyOntologySchemaPermissions(Schema ontologySchema, Bundle bundle) {
    if (bundle.fixedSchemaViewPermission() != null) {
      ontologySchema.addMember(bundle.fixedSchemaViewPermission(), Privileges.VIEWER.toString());
    }
  }

  private void loadDemoData(Schema schema, Bundle bundle, Task parentTask) {
    if (bundle.demodata() == null) return;
    for (String demodataPath : bundle.demodata()) {
      String resolved = resolveRelativePath(demodataPath);
      Task demoTask =
          new ImportDataTask(schema, new TableStoreForCsvFilesClasspath(resolved), false)
              .setDescription("Import demo data from: " + resolved);
      parentTask.addSubTask(demoTask);
      demoTask.run();
    }
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
