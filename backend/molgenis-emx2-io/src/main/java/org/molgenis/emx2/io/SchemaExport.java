package org.molgenis.emx2.io;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.rowstore.TableStore;
import org.molgenis.emx2.io.rowstore.TableStoreForCsvFilesDirectory;
import org.molgenis.emx2.io.rowstore.TableStoreForCsvInZipFile;
import org.molgenis.emx2.io.rowstore.TableStoreForXlsxFile;

import java.nio.file.Path;

public class SchemaExport {

  private SchemaExport() {
    // hide constructor
  }

  public static void toDirectory(Path directory, Schema schema) {
    executeExport(new TableStoreForCsvFilesDirectory(directory), schema);
  }

  public static void toZipFile(Path zipFile, Schema schema) {
    executeExport(new TableStoreForCsvInZipFile(zipFile), schema);
  }

  public static void toExcelFile(Path excelFile, Schema schema) {
    executeExport(new TableStoreForXlsxFile(excelFile), schema);
  }

  private static void executeExport(TableStore store, Schema schema) {
    // write metadata
    store.writeTable("molgenis", Emx2.toRowList(schema.getMetadata()));
    // write data
    for (String tableName : schema.getTableNames()) {
      store.writeTable(tableName, schema.getTable(tableName).getRows());
    }
  }
}
