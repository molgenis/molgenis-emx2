package org.molgenis.emx2.io.stores;

import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvRowReader;
import org.molgenis.emx2.io.readers.CsvRowWriter;
import org.molgenis.emx2.utils.MolgenisException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RowStoreForCsvFilesDirectory implements RowStore {
  static final String CSV_EXTENSION = ".csv";
  private final Path directoryPath;
  private final Character separator;

  public RowStoreForCsvFilesDirectory(Path directoryPath, Character separator) {
    this.directoryPath = directoryPath;
    if (!directoryPath.toFile().exists())
      throw new MolgenisException(
          "not exist", "Not exist", "Directory " + directoryPath + " doesn't exist");
    this.separator = separator;
  }

  public RowStoreForCsvFilesDirectory(Path directoryPath) {
    this(directoryPath, ',');
  }

  @Override
  public void write(String name, List<Row> rows) {
    if (rows.isEmpty()) return;
    Path relativePath = directoryPath.resolve(name + CSV_EXTENSION);
    try {
      Writer writer = Files.newBufferedWriter(relativePath);
      CsvRowWriter.writeCsv(rows, writer, separator);
      writer.close();
    } catch (IOException ioe) {
      throw new MolgenisException("io_exception", "IO exception", ioe.getMessage(), ioe);
    }
  }

  @Override
  public List<Row> read(String name) {
    Path relativePath = directoryPath.resolve(name + CSV_EXTENSION);
    try {
      Reader reader = Files.newBufferedReader(relativePath);
      return CsvRowReader.readList(reader, separator);
    } catch (IOException ioe) {
      throw new MolgenisException("io_exception", "IO exception", ioe.getMessage(), ioe);
    }
  }

  @Override
  public boolean containsTable(String name) {
    Path path = directoryPath.resolve(name + CSV_EXTENSION);
    return path.toFile().exists();
  }
}
