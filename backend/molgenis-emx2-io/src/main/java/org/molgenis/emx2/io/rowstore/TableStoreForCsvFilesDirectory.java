package org.molgenis.emx2.io.rowstore;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import org.molgenis.emx2.io.readers.RowReaderJackson;

public class TableStoreForCsvFilesDirectory implements TableStore {
  static final String CSV_EXTENSION = ".csv";
  private final Path directoryPath;
  private final Character separator;

  public TableStoreForCsvFilesDirectory(Path directoryPath, Character separator) {
    this.directoryPath = directoryPath;
    if (!directoryPath.toFile().exists())
      throw new MolgenisException("Import failed: Directory " + directoryPath + " doesn't exist");
    this.separator = separator;
  }

  public TableStoreForCsvFilesDirectory(Path directoryPath) {
    this(directoryPath, ',');
  }

  @Override
  public void writeTable(String name, Iterable<Row> rows) {
    Path relativePath = directoryPath.resolve(name + CSV_EXTENSION);
    try {
      Writer writer = Files.newBufferedWriter(relativePath);
      CsvTableWriter.write(rows, writer, separator);
      writer.close();
    } catch (IOException ioe) {
      throw new MolgenisException("Impoart failed", ioe);
    }
  }

  @Override
  public List<Row> readTable(String name) {
    Path relativePath = directoryPath.resolve(name + CSV_EXTENSION);
    try {
      Reader reader = Files.newBufferedReader(relativePath);
      return RowReaderJackson.readList(reader, separator);
    } catch (IOException ioe) {
      throw new MolgenisException(
          "Import failed: Table not found. File with name '"
              + name
              + "' doesn't exist. "
              + ioe.getMessage(),
          ioe);
    }
  }

  @Override
  public void processTable(String name, RowProcessor processor) {
    processor.process(readTable(name).iterator());
  }

  @Override
  public boolean containsTable(String name) {
    Path path = directoryPath.resolve(name + CSV_EXTENSION);
    return path.toFile().exists();
  }
}
