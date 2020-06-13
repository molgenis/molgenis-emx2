package org.molgenis.emx2.io;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.emx1.Emx1Import;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.rowstore.TableStore;
import org.molgenis.emx2.io.rowstore.TableStoreForCsvFilesDirectory;
import org.molgenis.emx2.io.rowstore.TableStoreForCsvInZipFile;
import org.molgenis.emx2.io.rowstore.TableStoreForXlsxFile;

import java.nio.file.Path;

public class SchemaImport {

  private SchemaImport() {
    // hide constructor
  }

  public static void fromDirectory(Path directory, Schema schema) {
    executeImport(new TableStoreForCsvFilesDirectory(directory), schema);
  }

  public static void fromZipFile(Path zipFile, Schema schema) {
    executeImport(new TableStoreForCsvInZipFile(zipFile), schema);
  }

  public static void fromExcelFile(Path excelFile, Schema schema) {
    executeImport(new TableStoreForXlsxFile(excelFile), schema);
  }

  static void executeImport(TableStore store, Schema schema) {

    schema.tx(
        db -> {
          // read emx1 metadata, if available (to be removed in future versions)
          if (store.containsTable("attributes")) {
            Emx1Import.uploadFromStoreToSchema(store, schema);
          } else {
            if (store.containsTable("molgenis")) {
              SchemaMetadata emx2Schema = Emx2.fromRowList(store.readTable("molgenis"));
              schema.merge(emx2Schema);
            } else {
              // read data
              for (String tableName : schema.getTableNames()) {
                if (store.containsTable(tableName))
                  schema.getTable(tableName).update(store.readTable(tableName)); // actually upsert
              }
            }
          }
        });
  }
}
