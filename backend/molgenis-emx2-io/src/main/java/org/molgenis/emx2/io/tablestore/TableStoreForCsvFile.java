package org.molgenis.emx2.io.tablestore;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvTableWriter;

/** can only handle one table */
public class TableStoreForCsvFile implements TableStore {
  private Path csvFile;

  public TableStoreForCsvFile(Path csvFile) {
    this.csvFile = csvFile;
  }

  @Override
  public void writeTable(String name, Iterable<Row> rows) {
    try {
      Writer writer = Files.newBufferedWriter(csvFile);
      CsvTableWriter.write(rows, writer, ',');
      writer.close();
    } catch (IOException ioe) {
      throw new MolgenisException("Export failed", ioe);
    }
  }

  @Override
  public Iterable<Row> readTable(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void processTable(String name, RowProcessor processor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsTable(String name) {
    throw new UnsupportedOperationException();
  }
}
