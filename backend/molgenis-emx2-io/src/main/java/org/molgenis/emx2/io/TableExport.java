package org.molgenis.emx2.io;

import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import org.molgenis.emx2.io.rowstore.TableStore;
import org.molgenis.emx2.io.rowstore.TableStoreForCsvInZipFile;
import org.molgenis.emx2.io.rowstore.TableStoreForXlsxFile;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class TableExport {
  public static void toZipFile(Path zipFile, Query query) {
    executeExport(new TableStoreForCsvInZipFile(zipFile), query);
  }

  public static void toExcelFile(Path excelFile, Query query) {
    executeExport(new TableStoreForXlsxFile(excelFile), query);
  }

  public static void toCsvFile(Path csvFile, Query query) {
    try {
      Writer writer = Files.newBufferedWriter(csvFile);
      CsvTableWriter.write(query.retrieveRows(), writer, ',');
      writer.close();
    } catch (IOException ioe) {
      throw new MolgenisException("Export failed", ioe.getMessage(), ioe);
    }
  }

  private static void executeExport(TableStore store, Query query) {
    store.writeTable(query.getSelect().getColumn(), query.retrieveRows());
  }
}
