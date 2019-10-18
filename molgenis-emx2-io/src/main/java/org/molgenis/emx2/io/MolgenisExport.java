package org.molgenis.emx2.io;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.emx2.ConvertSchemaToEmx2;
import org.molgenis.emx2.io.stores.RowStore;
import org.molgenis.emx2.io.stores.RowStoreForCsvFilesDirectory;
import org.molgenis.emx2.io.stores.RowStoreForCsvInZipFile;
import org.molgenis.emx2.io.stores.RowStoreForXlsxFile;

import java.io.IOException;
import java.nio.file.Path;

public class MolgenisExport {

  private MolgenisExport() {
    // hide constructor
  }

  public static void toDirectory(Path directory, Schema schema) throws IOException {
    executeExport(new RowStoreForCsvFilesDirectory(directory), schema);
  }

  public static void toZipFile(Path zipFile, Schema schema) throws IOException {
    executeExport(new RowStoreForCsvInZipFile(zipFile), schema);
  }

  public static void toExcelFile(Path excelFile, Schema schema) throws IOException {
    executeExport(new RowStoreForXlsxFile(excelFile), schema);
  }

  private static void executeExport(RowStore store, Schema schema) throws IOException {
    // write metadata
    store.write("molgenis", ConvertSchemaToEmx2.toRowList(schema.getMetadata()));
    // write data
    for (String tableName : schema.getTableNames()) {
      store.write(tableName, schema.getTable(tableName).retrieve());
    }
  }
}
