package org.molgenis.emx2.io;

import static org.molgenis.emx2.Privileges.VIEWER;
import static org.molgenis.emx2.io.emx2.Emx2.outputMetadata;
import static org.molgenis.emx2.io.emx2.Emx2Members.outputRoles;
import static org.molgenis.emx2.io.emx2.Emx2Settings.outputSettings;
import static org.molgenis.emx2.io.emx2.Emx2Tables.outputTable;
import static org.molgenis.emx2.io.emx2.Emx2Tables.outputTableWithSystemColumns;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.io.emx1.Emx1;
import org.molgenis.emx2.io.emx2.Emx2Yaml;
import org.molgenis.emx2.io.tablestore.*;
import org.molgenis.emx2.tasks.Task;

/** Short hands for running the tasks */
public class MolgenisIO {

  private MolgenisIO() {
    // hide constructor
  }

  private static void outputAll(TableStore store, Schema schema, boolean includeSystemColumns) {
    outputMetadata(store, schema);
    outputRoles(store, schema);
    outputSettings(store, schema);

    boolean hasViewPermission = schema.getInheritedRolesForActiveUser().contains(VIEWER.toString());
    for (String tableName : schema.getTableNames()) {
      Table table = schema.getTable(tableName);
      if (hasViewPermission || table.getMetadata().getTableType().equals(TableType.ONTOLOGIES)) {
        writeTableToStore(store, table, includeSystemColumns);
      }
    }
  }

  public static void toDirectory(Path directory, Schema schema, boolean includeSystemColumns) {
    outputAll(new TableStoreForCsvFilesDirectory(directory), schema, includeSystemColumns);
  }

  public static void toZipFile(Path zipFile, Schema schema, boolean includeSystemColumns) {
    outputAll(new TableStoreForCsvInZipFile(zipFile), schema, includeSystemColumns);
  }

  public static void toYamlZipFile(Path zipFile, Schema schema, boolean includeSystemColumns) {
    TableStoreForCsvInZipFile store = new TableStoreForCsvInZipFile(zipFile);
    outputRoles(store, schema);
    outputSettings(store, schema);
    boolean hasViewPermission = schema.getInheritedRolesForActiveUser().contains(VIEWER.toString());
    for (String tableName : schema.getTableNames()) {
      Table table = schema.getTable(tableName);
      if (hasViewPermission || table.getMetadata().getTableType().equals(TableType.ONTOLOGIES)) {
        writeTableToStore(store, table, includeSystemColumns);
      }
    }
    try {
      Path tempDir = Files.createTempDirectory("yaml_export_");
      try {
        Emx2Yaml.toYamlDirectory(schema.getMetadata(), tempDir);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, Map.of())) {
          Path tablesDir = zipfs.getPath("/tables");
          if (!Files.exists(tablesDir)) {
            Files.createDirectories(tablesDir);
          }
          Path tempTablesDir = tempDir.resolve("tables");
          if (Files.exists(tempTablesDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempTablesDir, "*.yaml")) {
              for (Path yamlFile : stream) {
                Files.copy(
                    yamlFile,
                    tablesDir.resolve(yamlFile.getFileName().toString()),
                    StandardCopyOption.REPLACE_EXISTING);
              }
            }
          }
          Files.writeString(zipfs.getPath("/molgenis.yaml"), "# MOLGENIS EMX2 YAML format\n");
        }
      } finally {
        Path tempTablesDir = tempDir.resolve("tables");
        if (Files.exists(tempTablesDir)) {
          try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempTablesDir)) {
            for (Path file : stream) {
              Files.deleteIfExists(file);
            }
          }
          Files.deleteIfExists(tempTablesDir);
        }
        Files.deleteIfExists(tempDir);
      }
    } catch (IOException e) {
      throw new MolgenisException("YAML ZIP export failed", e);
    }
  }

  public static void toExcelFile(Path excelFile, Schema schema, boolean includeSystemColumns) {
    outputAll(new TableStoreForXlsxFile(excelFile), schema, includeSystemColumns);
  }

  public static void toEmx1ExcelFile(Path excelFile, Schema schema) {
    executeEmx1Export(new TableStoreForXlsxFile(excelFile), schema);
  }

  private static void executeEmx1Export(TableStore store, Schema schema) {
    // write metadata
    store.writeTable(
        "entities", List.of("UNSUPPORTED"), Emx1.getEmx1Entities(schema.getMetadata()));
    store.writeTable(
        "attributes", List.of("UNSUPPORTED"), Emx1.getEmx1Attributes(schema.getMetadata()));
    // write data
    for (String tableName : schema.getTableNames()) {
      outputTable(store, schema.getTable(tableName));
    }
  }

  public static void toZipFile(Path zipFile, Table table, boolean includeSystemColumns) {
    writeTableToStore(new TableStoreForCsvInZipFile(zipFile), table, includeSystemColumns);
  }

  public static void toExcelFile(Path excelFile, Table table, boolean includeSystemColumns) {
    writeTableToStore(new TableStoreForXlsxFile(excelFile), table, includeSystemColumns);
  }

  public static void toCsvFile(Path csvFile, Table table, boolean includeSystemColumns) {
    writeTableToStore(new TableStoreForCsvFile(csvFile), table, includeSystemColumns);
  }

  private static void writeTableToStore(
      TableStore store, Table table, boolean includeSystemColumns) {
    if (includeSystemColumns) {
      outputTableWithSystemColumns(store, table);
    } else {
      outputTable(store, table);
    }
  }

  public static void fromDirectory(Path directory, Schema schema, boolean strict) {
    new ImportDirectoryTask(directory, schema, strict).run();
  }

  public static void fromZipFile(Path zipFile, Schema schema, boolean strict) {
    new ImportCsvZipTask(zipFile, schema, strict).run();
  }

  public static void importFromExcelFile(Path excelFile, Schema schema, boolean strict) {
    new ImportExcelTask(excelFile, schema, strict).run();
  }

  public static Task fromStore(
      TableStore store, Schema schema, boolean strict, String... includeTableNames) {
    Task task = new ImportSchemaTask(store, schema, strict, includeTableNames);
    task.run();
    return task;
  }

  public static Task fromClasspathDirectory(
      String path, Schema schema, boolean strict, String... includeTableNames) {
    return fromStore(new TableStoreForCsvFilesClasspath(path), schema, strict, includeTableNames);
  }
}
