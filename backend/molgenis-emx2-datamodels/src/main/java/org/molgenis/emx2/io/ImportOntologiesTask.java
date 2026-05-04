package org.molgenis.emx2.io;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Task;

/**
 * Imports ontology data and semantics into a schema, skipping tables whose classpath CSV has not
 * changed since the last import. Staleness is detected by comparing SHA-256 checksums of the CSV
 * files against checksums stored in table/schema settings.
 */
public class ImportOntologiesTask extends Task {

  static final String CSV_CHECKSUM_SETTING = "mg_csv_checksum";

  private final Schema schema;
  private final TableStore store;
  private final String ontologyLocation;
  private final String semanticsLocation;

  public ImportOntologiesTask(
      Schema schema, TableStore store, String ontologyLocation, String semanticsLocation) {
    super("Import ontologies from profile");
    this.schema = schema;
    this.store = store;
    this.ontologyLocation = ontologyLocation;
    this.semanticsLocation = semanticsLocation;
  }

  @Override
  public void run() {
    this.start();
    importOntologyData();
    createOntologySchema();
    this.complete();
  }

  private void importOntologyData() {
    for (Table table : schema.getTablesSorted()) {
      String tableName = table.getName();
      if (!store.containsTable(tableName)) {
        continue;
      }
      String csvPath = ontologyLocation + "/" + tableName + ".csv";
      String newChecksum = computeClasspathResourceChecksum(csvPath);
      String storedChecksum = table.getMetadata().getSetting(CSV_CHECKSUM_SETTING);
      if (Objects.equals(newChecksum, storedChecksum)) {
        this.addSubTask("Ontology " + tableName + ": up to date, skipped").setSkipped();
        continue;
      }
      ImportTableTask importTableTask = new ImportTableTask(store, table, false);
      this.addSubTask(importTableTask);
      importTableTask.run();
      // checksum is persisted within the outer transaction in ImportProfileTask.run();
      // if that transaction rolls back, the checksum is not stored and re-import will occur on
      // retry
      table.getMetadata().setSetting(CSV_CHECKSUM_SETTING, newChecksum);
    }
  }

  // Always applied (no checksum caching) because the semantics CSV is global but filtered
  // per-schema, so a checksum on the shared ontology schema would incorrectly skip semantics
  // for ontology tables not yet seen. The migrate() here is metadata-only (no AccessExclusiveLock).
  private void createOntologySchema() {
    URL dirURL = getClass().getResource(semanticsLocation);
    if (dirURL == null) {
      throw new MolgenisException(
          "Import failed: File " + semanticsLocation + " doesn't exist in classpath");
    }
    Set<String> tablesToUpdate = new HashSet<>();
    for (TableMetadata tableMetadata : schema.getMetadata().getTables()) {
      if (tableMetadata.getTableType().equals(TableType.ONTOLOGIES)) {
        tablesToUpdate.add(tableMetadata.getTableName());
      }
    }
    InputStreamReader reader =
        new InputStreamReader(
            Objects.requireNonNull(getClass().getResourceAsStream(semanticsLocation)));
    List<Row> keepRows = new ArrayList<>();
    for (Row row : CsvTableReader.read(reader)) {
      if (tablesToUpdate.contains(row.getString("tableName"))) {
        keepRows.add(row);
      }
    }
    schema.migrate(Emx2.fromRowList(keepRows));
  }

  /** Compute SHA-256 checksum of a classpath resource, returned as hex string. */
  static String computeClasspathResourceChecksum(String resourcePath) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (InputStream is = ImportOntologiesTask.class.getResourceAsStream(resourcePath)) {
        if (is == null) {
          throw new MolgenisException(
              "Checksum failed: resource " + resourcePath + " not found on classpath");
        }
        DigestInputStream dis = new DigestInputStream(is, digest);
        byte[] buffer = new byte[8192];
        while (dis.read(buffer) != -1) {
          // reading through to compute digest
        }
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException | java.io.IOException e) {
      throw new MolgenisException("Checksum computation failed: " + e.getMessage(), e);
    }
  }
}
