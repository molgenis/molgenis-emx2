package org.molgenis.emx2.io;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.emx1.ConvertEmx1ToSchema;
import org.molgenis.emx2.io.emx2.ConvertEmx2ToSchema;
import org.molgenis.emx2.io.stores.RowStoreForCsvFilesDirectory;
import org.molgenis.emx2.io.stores.RowStoreForXlsxFile;
import org.molgenis.emx2.io.stores.RowStore;
import org.molgenis.emx2.io.stores.RowStoreForCsvInZipFile;

import java.nio.file.Path;

public class MolgenisImport {

  private MolgenisImport() {
    // hide constructor
  }

  public static void fromDirectory(Path directory, Schema schema) {
    executeImport(new RowStoreForCsvFilesDirectory(directory), schema);
  }

  public static void fromZipFile(Path zipFile, Schema schema) {
    executeImport(new RowStoreForCsvInZipFile(zipFile), schema);
  }

  public static void fromExcelFile(Path excelFile, Schema schema) {
    executeImport(new RowStoreForXlsxFile(excelFile), schema);
  }

  static void executeImport(RowStore store, Schema schema) {

    schema.transaction(
        db -> {
          String prefix = "";
          // read emx2 metadata, if available
          if (store.containsTable("molgenis")) {
            SchemaMetadata emx2Schema = ConvertEmx2ToSchema.fromRowList(store.read("molgenis"));
            schema.merge(emx2Schema);
          } else
          // read emx1 metadata, if available (to be removed in future versions)
          // todo: only do this if it looks like metadata file OR make this parameter?
          if (store.containsTable("attributes")) {
            // todo: prefix should come from entities or package file, we will remove root package
            prefix = "bbmri_nl_";
            SchemaMetadata emx1Schema = ConvertEmx1ToSchema.convert(store, prefix);
            schema.merge(emx1Schema);
          }
          // read data
          for (String tableName : schema.getTableNames()) {
            if (store.containsTable(prefix + tableName))
              schema.getTable(tableName).update(store.read(prefix + tableName)); // actually upsert
          }
        });
  }
}
