package org.molgenis.emx2.io;

import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.emx2.io.emx2format.ConvertSchemaToEmx2;
import org.molgenis.emx2.io.stores.RowStore;
import org.molgenis.emx2.io.stores.RowStoreForCsvFilesDirectory;
import org.molgenis.emx2.io.stores.RowStoreForCsvInZipFile;

import java.io.IOException;
import java.nio.file.Path;

public class MolgenisExport {

  private MolgenisExport() {
    // hide constructor
  }

  public static void toDirectory(Path directory, Schema schema)
      throws MolgenisException, IOException {
    executeExport(new RowStoreForCsvFilesDirectory(directory), schema);
  }

  public static void toZipFile(Path zipFile, Schema schema) throws IOException, MolgenisException {
    executeExport(new RowStoreForCsvInZipFile(zipFile), schema);
  }

  private static void executeExport(RowStore store, Schema schema)
      throws MolgenisException, IOException {
    // write metadata
    store.write("molgenis", ConvertSchemaToEmx2.toRowList(schema));
    // write data
    for (String tableName : schema.getTableNames()) {
      store.write(tableName, schema.getTable(tableName).retrieve());
    }
  }
}
