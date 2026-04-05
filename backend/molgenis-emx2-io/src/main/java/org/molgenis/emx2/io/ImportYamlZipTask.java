package org.molgenis.emx2.io;

import java.io.IOException;
import java.nio.file.*;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.emx2.Emx2Members;
import org.molgenis.emx2.io.emx2.Emx2Settings;
import org.molgenis.emx2.io.emx2.Emx2Yaml;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInZipFile;
import org.molgenis.emx2.tasks.Task;

public class ImportYamlZipTask extends Task {
  private final Path zipFile;
  private final Schema schema;
  private final boolean strict;

  public ImportYamlZipTask(Path zipFile, Schema schema, boolean strict) {
    super("Import YAML zip file");
    this.zipFile = zipFile;
    this.schema = schema;
    this.strict = strict;
  }

  @Override
  public void run() {
    this.start();
    Task commit = new Task("Committing");
    try {
      schema.tx(
          db -> {
            Schema transactionalSchema = db.getSchema(schema.getName());
            try {
              Path tempDir = Files.createTempDirectory("yaml_import_");
              try {
                extractYamlFiles(zipFile, tempDir);
                SchemaMetadata yamlSchema = Emx2Yaml.fromBundle(tempDir).getSchema();

                Task metadataTask = this.addSubTask("Loading YAML metadata");
                metadataTask.start();
                transactionalSchema.migrate(yamlSchema);
                metadataTask.complete();

                TableStoreForCsvInZipFile csvStore = new TableStoreForCsvInZipFile(zipFile);

                if (csvStore.containsTable("molgenis_members")) {
                  Emx2Members.inputRoles(csvStore, transactionalSchema);
                  this.addSubTask("Loaded members from 'molgenis_members'").complete();
                }
                if (csvStore.containsTable("molgenis_settings")) {
                  Emx2Settings.inputSettings(csvStore, transactionalSchema);
                  this.addSubTask("Loaded settings from 'molgenis_settings'").complete();
                }

                Task dataTask = new ImportDataTask(transactionalSchema, csvStore, strict);
                this.addSubTask(dataTask);
                dataTask.run();
              } finally {
                deleteDirectory(tempDir);
              }
            } catch (IOException e) {
              throw new MolgenisException("YAML ZIP import failed", e);
            }
            this.addSubTask(commit.start());
          });
    } catch (Exception e) {
      this.setError("Import failed: " + e.getMessage());
      throw e;
    }
    commit.complete();
    this.complete();
  }

  private static void extractYamlFiles(Path zipFile, Path targetDir) throws IOException {
    Path tablesDir = targetDir.resolve("tables");
    Files.createDirectories(tablesDir);
    try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, (ClassLoader) null)) {
      Path zipMolgenisYaml = zipfs.getPath("/molgenis.yaml");
      if (Files.exists(zipMolgenisYaml)) {
        Files.copy(zipMolgenisYaml, targetDir.resolve("molgenis.yaml"));
      }
      Path zipTablesDir = zipfs.getPath("/tables");
      if (Files.exists(zipTablesDir)) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(zipTablesDir, "*.yaml")) {
          for (Path yamlFile : stream) {
            Files.copy(yamlFile, tablesDir.resolve(yamlFile.getFileName().toString()));
          }
        }
      }
    }
  }

  private static void deleteDirectory(Path dir) {
    try {
      Path tablesDir = dir.resolve("tables");
      if (Files.exists(tablesDir)) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tablesDir)) {
          for (Path file : stream) {
            Files.deleteIfExists(file);
          }
        }
        Files.deleteIfExists(tablesDir);
      }
      Files.deleteIfExists(dir.resolve("molgenis.yaml"));
      Files.deleteIfExists(dir);
    } catch (IOException e) {
      // best effort cleanup
    }
  }
}
