package org.molgenis.emx2.io;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.emx1.Emx1;
import org.molgenis.emx2.io.emx2.Emx2Reader;
import org.molgenis.emx2.io.rowstore.TableStoreForCsvFilesDirectory;
import org.molgenis.emx2.io.rowstore.TableStoreForXlsxFile;
import org.molgenis.emx2.io.rowstore.TableStore;
import org.molgenis.emx2.io.rowstore.TableStoreForCsvInZipFile;

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

    schema.transaction(
        db -> {
          // read emx1 metadata, if available (to be removed in future versions)
          // todo: only do this if it looks like metadata file OR make this parameter?
          if (store.containsTable("attributes")) {
            Emx1.uploadFromStoreToSchema(store, schema);
          } else {
            if (store.containsTable("molgenis")) {
              SchemaMetadata emx2Schema = Emx2Reader.fromRowList(store.readTable("molgenis"));
              schema.merge(emx2Schema);
            } else
              // read data
              for (String tableName : schema.getTableNames()) {
                if (store.containsTable(tableName))
                  schema.getTable(tableName).update(store.readTable(tableName)); // actually upsert
              }
          }
        });
  }
}
