package org.molgenis.emx2.io;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.emx2format.ConvertEmx2ToSchema;
import org.molgenis.emx2.io.stores.RowStoreForCsvFilesDirectory;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.io.stores.RowStore;
import org.molgenis.emx2.io.stores.RowStoreForCsvInZipFile;

import java.io.IOException;
import java.nio.file.Path;

public class MolgenisImport {

  private MolgenisImport() {
    // hide constructor
  }

  public static void fromDirectory(Path directory, Schema schema)
      throws MolgenisException, IOException {
    executeImport(new RowStoreForCsvFilesDirectory(directory), schema);
  }

  public static void fromZipFile(Path zipFile, Schema schema)
      throws IOException, MolgenisException {
    executeImport(new RowStoreForCsvInZipFile(zipFile), schema);
  }

  static void executeImport(RowStore store, Schema schema) throws MolgenisException {
    schema.transaction(
        db -> {
          // read metadata, if available
          if (store.contains("molgenis")) {
            SchemaMetadata emx2Schema = ConvertEmx2ToSchema.fromRowList(store.read("molgenis"));
            schema.merge(emx2Schema);
          }
          // read data
          for (String tableName : schema.getTableNames()) {
            if (store.contains(tableName))
              schema.getTable(tableName).update(store.read(tableName)); // actually upsert
          }
        });
  }
}
